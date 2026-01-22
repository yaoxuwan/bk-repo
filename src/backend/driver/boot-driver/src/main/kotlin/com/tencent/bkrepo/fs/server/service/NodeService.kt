package com.tencent.bkrepo.fs.server.service

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.metadata.dao.node.RNodeDao
import com.tencent.bkrepo.common.metadata.model.NodeAttribute
import com.tencent.bkrepo.common.metadata.model.NodeAttribute.Companion.NOBODY
import com.tencent.bkrepo.common.metadata.model.TNode
import com.tencent.bkrepo.fs.server.constant.FS_ATTR_KEY
import com.tencent.bkrepo.fs.server.pojo.DriveNode
import com.tencent.bkrepo.fs.server.request.v2.service.NodeCreateRequest
import com.tencent.bkrepo.fs.server.utils.ReactiveSecurityUtils
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class NodeService(
    private val nodeDao: RNodeDao
) {

    suspend fun getNode(projectId: String, repoName: String, id: String): DriveNode {
        return nodeDao.findNodeById(projectId, repoName, id)?.convert() ?: throw NodeNotFoundException(id)
    }

    suspend fun getNode(projectId: String, repoName: String, parentId: String, name: String): DriveNode? {
        return nodeDao.findNodeByParentIdAndName(projectId, repoName, parentId, name)?.convert()
    }

    suspend fun listNode(projectId: String, repoName: String, id: String, offset: Long): List<DriveNode> {
        return nodeDao.listNodesById(projectId, repoName, id, offset).map { it.convert() }
    }

    suspend fun createNode(request: NodeCreateRequest): DriveNode {
        with(request) {
            val userId = ReactiveSecurityUtils.getUser()
            val parentNode = if (parentId == "1") {
                RNodeDao.buildRootNode(projectId, repoName)
            } else {
                nodeDao.findNodeById(projectId, repoName, parentId) ?: throw NodeNotFoundException(parentId)
            }
            if (!parentNode.folder) {
                throw ErrorCodeException(
                    ArtifactMessageCode.NODE_CONFLICT, parentNode.fullPath
                )
            }
            val attributes = NodeAttribute(
                uid = NOBODY,
                gid = NOBODY,
                mode = mode ,
                flags = flags,
                rdev = rdev,
                type = type
            )
            val fsAttr = MetadataModel(
                key = FS_ATTR_KEY,
                value = attributes
            )
            return try {
                nodeDao.createNode(
                    userId = userId,
                    projectId = projectId,
                    repoName = repoName,
                    name = name,
                    folder = folder,
                    size = size,
                    parent = parentNode,
                    id = id,
                    nodeMetadata = listOf(fsAttr)
                ).convert()
            } catch (_: DuplicateKeyException) {
                throw ErrorCodeException(
                    ArtifactMessageCode.NODE_EXISTED, PathUtils.combineFullPath(parentNode.fullPath, name)
                )
            }
        }
    }

    suspend fun deleteNode(projectId: String, repoName: String, id: String): Boolean {
        val userId = ReactiveSecurityUtils.getUser()
        return nodeDao.deleteNode(userId, projectId, repoName, id)
    }

    suspend fun renameNode(projectId:String, repoName: String, srcNode: DriveNode, dstParentNode: DriveNode, destName: String) {
        val userId = ReactiveSecurityUtils.getUser()
        nodeDao.renameNode(userId,  projectId, repoName, srcNode.id, dstParentNode, destName)
    }


    @Suppress("UNCHECKED_CAST")
    private fun TNode.convert(): DriveNode {
        // 从 metadata 中提取文件系统属性
        val nodeAttribute = this.metadata
            ?.find { it.key == FS_ATTR_KEY }
            ?.value as? NodeAttribute

        return DriveNode(
            id = this.id!!,
            fullPath = this.fullPath,
            size = this.size,
            parentId = this.parentId ?: StringPool.EMPTY,
            name = this.name,
            folder = this.folder,
            path = this.path,
            createdBy = this.createdBy,
            createdDate = this.createdDate,
            lastModifiedBy = this.lastModifiedBy,
            lastModifiedDate = this.lastModifiedDate,
            lastAccessDate = this.lastAccessDate,
            uid = nodeAttribute?.uid,
            gid = nodeAttribute?.gid,
            mode = nodeAttribute?.mode,
            flags = nodeAttribute?.flags,
            rdev = nodeAttribute?.rdev,
            type = nodeAttribute?.type,
        )
    }
}