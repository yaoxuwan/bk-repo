/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.metadata.dao.node

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.metadata.condition.ReactiveCondition
import com.tencent.bkrepo.common.metadata.model.TMetadata
import com.tencent.bkrepo.common.metadata.model.TNode
import com.tencent.bkrepo.common.metadata.util.NodeQueryHelper
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.mongo.reactive.dao.HashShardingMongoReactiveDao
import com.tencent.bkrepo.fs.server.pojo.DriveNode
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import org.springframework.context.annotation.Conditional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import java.time.LocalDateTime


/**
 * 节点 Dao
 */
@Component
@Conditional(ReactiveCondition::class)
class RNodeDao: HashShardingMongoReactiveDao<TNode>() {
    /**
     * 查询节点
     */
    suspend fun findNode(projectId: String, repoName: String, fullPath: String): TNode? {
        // 系统设计上不保存根目录节点到数据库，但是有用户会手动创建根目录节点
        return this.findOne(NodeQueryHelper.nodeQuery(projectId, repoName, fullPath))
            ?: if (PathUtils.isRoot(fullPath)) buildRootNode(projectId, repoName) else null
    }

    /**
     * 查询节点是否存在
     */
    suspend fun exists(projectId: String, repoName: String, fullPath: String): Boolean {
        if (PathUtils.isRoot(fullPath)) {
            return true
        }
        return this.exists(NodeQueryHelper.nodeQuery(projectId, repoName, fullPath))
    }

    /**
     * 更新目录下变更的文件数量以及涉及的文件大小
     */
    suspend fun incSizeAndNodeNumOfFolder(
        projectId: String,
        repoName: String,
        fullPath: String,
        size: Long,
        nodeNum: Long
    ) {
        val query = NodeQueryHelper.nodeFolderQuery(projectId, repoName, fullPath)
        val update = Update()
            .inc(TNode::size.name, size)
            .inc(TNode::nodeNum.name, nodeNum)
            .set(TNode::lastModifiedDate.name, LocalDateTime.now())
        val options = FindAndModifyOptions()
        options.returnNew(true)
        val tNode = this.findAndModify(query, update, options, TNode::class.java)
        if (tNode != null && (tNode.nodeNum!! < 0L || tNode.size < 0L )) {
            // 如果数据为负数，将其设置为 0
            val updateMax = Update()
                .max(TNode::size.name, 0L)
                .max(TNode::nodeNum.name, 0L)
            this.updateFirst(query, updateMax)
        }
    }

    /**
     * 设置目录下的文件数量以及文件大小
     */
    suspend fun setSizeAndNodeNumOfFolder(
        projectId: String,
        repoName: String,
        fullPath: String,
        size: Long,
        nodeNum: Long
    ) {
        val query = NodeQueryHelper.nodeFolderQuery(projectId, repoName, fullPath)
        val update = Update().set(TNode::size.name, size)
            .set(TNode::nodeNum.name, nodeNum)
        this.updateFirst(query, update)
    }


    /**
     * 根据[sha256]分页查询节点，需要遍历所有分表
     *
     * @param includeDeleted 是否包含被删除的节点
     */
    suspend fun pageBySha256(
        sha256: String,
        option: NodeListOption,
        includeDeleted: Boolean = false
    ): Page<TNode> {
        val pageRequest = Pages.ofRequest(option.pageNumber, option.pageSize)

        // 构造查询条件
        val criteria = where(TNode::sha256).isEqualTo(sha256).and(TNode::folder).isEqualTo(false)
        if (!includeDeleted) {
            criteria.and(TNode::deleted).isEqualTo(null)
        }
        val query = Query(criteria)
        if (!option.includeMetadata) {
            query.fields().exclude(TNode::metadata.name)
        }

        return pageWithoutShardingKey(pageRequest, query)
    }

    suspend fun findNodeById(projectId: String, repoName: String, id: String): TNode? {
        if (id == "1") {
            return buildRootNode(projectId, repoName)
        }
        val query = Query(Criteria.where(ID).isEqualTo(id)
            .and(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::deleted).isEqualTo(null)
        )
        return this.findOne(query)
    }

    suspend fun listNodesById(projectId: String, repoName: String, id: String, offset: Long): List<TNode> {
        val query = Query(where(TNode::parentId).isEqualTo(id)
            .and(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::deleted).isEqualTo(null)
        ).with(Sort.by(Sort.Direction.ASC, ID)).skip(offset)
        return this.find(query)
    }

    suspend fun createNode(
        userId: String,
        projectId: String,
        repoName: String,
        name: String,
        folder: Boolean,
        size: Long,
        parent: TNode,
        id: String? = null,
        nodeMetadata: List<MetadataModel> = emptyList()
    ): TNode {
        val node = TNode(
            id = id,
            projectId = projectId,
            repoName = repoName,
            parentId = parent.id!!,
            name = name,
            folder = folder,
            size = size,
            createdBy = userId,
            createdDate = LocalDateTime.now(),
            lastModifiedBy = userId,
            lastModifiedDate = LocalDateTime.now(),
            lastAccessDate = LocalDateTime.now(),
            path = parent.fullPath,
            fullPath = PathUtils.combineFullPath(parent.fullPath, name),
            metadata = nodeMetadata.map { TMetadata(it.key, it.value) }.toMutableList()
        )
        return this.insert(node)
    }

    suspend fun deleteNode(userId: String, projectId: String, repoName: String, id: String): Boolean {
        val query = Query(Criteria.where(ID).isEqualTo(id)
            .and(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
        )
        val update = Update().set(TNode::deleted.name, LocalDateTime.now())
            .set(TNode::lastModifiedBy.name, userId)
            .set(TNode::lastModifiedDate.name, LocalDateTime.now())
        return this.updateFirst(query, update).modifiedCount == 1L
    }

    suspend fun findNodeByParentIdAndName(projectId: String, repoName: String, parentId: String, name: String): TNode? {
        val query = Query(where(TNode::parentId).isEqualTo(parentId)
            .and(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::name).isEqualTo(name)
            .and(TNode::deleted).isEqualTo(null)
        )
        return this.findOne(query)
    }

    suspend fun renameNode(userId: String, projectId: String, repoName: String, srcId: String, dstNode: DriveNode, dstName: String): Boolean {
        val query = Query(Criteria.where(ID).isEqualTo(srcId)
            .and(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::deleted).isEqualTo(null)
        )
        val update = Update().set(TNode::name.name, dstName)
            .set(TNode::parentId.name, dstNode.id)
            .set(TNode::path.name, dstNode.fullPath)
            .set(TNode::fullPath.name, PathUtils.combineFullPath(dstNode.fullPath, dstName))
        return this.updateFirst(query, update).modifiedCount == 1L
    }

    suspend fun updateSize(projectId: String, repoName: String, id: String, size: Long): Boolean {
        val query = Query(Criteria.where(ID).isEqualTo(id)
            .and(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
        )
        val update = Update().set(TNode::size.name, size)
        return this.updateFirst(query, update).modifiedCount == 1L
    }

    companion object {
        fun buildRootNode(projectId: String, repoName: String): TNode {
            return TNode(
                id = "1",
                createdBy = StringPool.EMPTY,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = StringPool.EMPTY,
                lastModifiedDate = LocalDateTime.now(),
                projectId = projectId,
                repoName = repoName,
                folder = true,
                path = PathUtils.ROOT,
                name = StringPool.EMPTY,
                fullPath = PathUtils.ROOT,
                size = 0
            )
        }
    }
}
