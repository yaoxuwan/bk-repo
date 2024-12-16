/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.fs.server.service

import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.metadata.constant.FAKE_MD5
import com.tencent.bkrepo.common.metadata.constant.FAKE_SHA256
import com.tencent.bkrepo.common.metadata.model.TBlockNode
import com.tencent.bkrepo.common.metadata.service.fs.FsService
import com.tencent.bkrepo.common.metadata.service.metadata.RMetadataService
import com.tencent.bkrepo.fs.server.config.properties.StreamProperties
import com.tencent.bkrepo.fs.server.constant.FS_ATTR_KEY
import com.tencent.bkrepo.fs.server.context.ReactiveArtifactContextHolder
import com.tencent.bkrepo.fs.server.model.NodeAttribute
import com.tencent.bkrepo.fs.server.request.BlockRequest
import com.tencent.bkrepo.fs.server.request.FlushRequest
import com.tencent.bkrepo.fs.server.request.StreamRequest
import com.tencent.bkrepo.fs.server.service.node.RNodeService
import com.tencent.bkrepo.fs.server.storage.CoArtifactFile
import com.tencent.bkrepo.fs.server.storage.CoArtifactFileFactory
import com.tencent.bkrepo.fs.server.storage.CoStorageManager
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeSetLengthRequest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.web.reactive.function.server.bodyToFlow
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class FileOperationService(
    private val storageManager: CoStorageManager,
    private val fileNodeService: FileNodeService,
    private val streamProperties: StreamProperties,
    private val metadataService: RMetadataService,
    private val fsService: FsService,
    private val nodeService: RNodeService,
) {

    suspend fun read(nodeDetail: NodeDetail, range: Range): ArtifactInputStream? {
        val repo = ReactiveArtifactContextHolder.getRepoDetail()
        return fileNodeService.read(
            nodeDetail = nodeDetail,
            storageCredentials = repo.storageCredentials,
            range = range
        )
    }

    suspend fun write(artifactFile: CoArtifactFile, request: BlockRequest, user: String): TBlockNode {
        with(request) {
            val blockNode = TBlockNode(
                createdBy = user,
                createdDate = LocalDateTime.now(),
                nodeFullPath = fullPath,
                startPos = offset,
                sha256 = artifactFile.getFileSha256(),
                projectId = projectId,
                repoName = repoName,
                size = artifactFile.getSize()
            )
            storageManager.storeBlock(artifactFile, blockNode).awaitSingle()
            return blockNode
        }
    }

    suspend fun stream(streamRequest: StreamRequest, user: String): NodeDetail {
        val nodeDetail = nodeService.createNode(buildNodeCreateRequest(streamRequest, user))
        val artifactFiles = mutableListOf(CoArtifactFileFactory.buildArtifactFile())
        val blockNodeMonos = mutableListOf<Mono<TBlockNode>>()
        val index = AtomicInteger(0)
        val offset = AtomicLong(0)
        streamRequest.request.bodyToFlow<DataBuffer>().onCompletion {
            val artifactFile = artifactFiles[index.get()]
            if (artifactFile.getSize() > 0) {
                storeBlockNode(artifactFiles, index, offset, user, streamRequest, true)?.let {
                    blockNodeMonos.add(it)
                }
            }
        }.collect {
            try {
                val artifactFile = artifactFiles[index.get()]
                artifactFile.write(it)
                storeBlockNode(artifactFiles, index, offset, user, streamRequest)?.let { m -> blockNodeMonos.add(m) }
            } finally {
                DataBufferUtils.release(it)
            }
        }
        blockNodeMonos.forEach { it.awaitSingle() }
        artifactFiles.forEach { it.close() }
        return nodeDetail
    }

    private suspend fun storeBlockNode(
        artifactFiles: MutableList<CoArtifactFile>,
        index: AtomicInteger,
        offset: AtomicLong,
        user: String,
        streamRequest: StreamRequest,
        lastBlock: Boolean = false
    ): Mono<TBlockNode>? {
        val artifactFile = artifactFiles[index.get()]
        val blockSize = artifactFiles[index.get()].getSize()
        return if (blockSize >= streamProperties.blockSize.toBytes() || lastBlock) {
            artifactFiles.add(CoArtifactFileFactory.buildArtifactFile())
            index.incrementAndGet()

            artifactFile.finish()
            val blockNode = TBlockNode(
                createdBy = user,
                createdDate = LocalDateTime.now(),
                nodeFullPath = streamRequest.fullPath,
                startPos = offset.toLong(),
                sha256 = artifactFile.getFileSha256(),
                projectId = streamRequest.projectId,
                repoName = streamRequest.repoName,
                size = artifactFile.getSize()
            )
            val tBlockNodeMono = storageManager.storeBlock(artifactFile, blockNode)
            offset.addAndGet(blockSize)
            tBlockNodeMono
        } else {
            null
        }
    }

    suspend fun flush(request: FlushRequest, user: String) {
        with(request) {
            metadataService.listMetadata(projectId, repoName, fullPath)[FS_ATTR_KEY] ?: let {
                val attributes = NodeAttribute(
                    uid = NodeAttribute.NOBODY,
                    gid = NodeAttribute.NOBODY,
                    mode = NodeAttribute.DEFAULT_MODE
                )
                val fsAttr = MetadataModel(
                    key = FS_ATTR_KEY,
                    value = attributes
                )
                val saveMetaDataRequest = MetadataSaveRequest(
                    projectId = projectId,
                    repoName = repoName,
                    fullPath = fullPath,
                    nodeMetadata = listOf(fsAttr),
                    operator = user
                )
                metadataService.saveMetadata(saveMetaDataRequest)
            }

            val nodeSetLengthRequest = NodeSetLengthRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath,
                newLength = length,
                operator = user
            )
            fsService.setLength(nodeSetLengthRequest)
        }
    }

    private fun buildNodeCreateRequest(
        request: StreamRequest,
        user: String
    ): NodeCreateRequest {
        val attributes = NodeAttribute(
            uid = NodeAttribute.NOBODY,
            gid = NodeAttribute.NOBODY,
            mode = NodeAttribute.DEFAULT_MODE
        )
        val fsAttr = MetadataModel(
            key = FS_ATTR_KEY,
            value = attributes
        )
        return NodeCreateRequest(
            projectId = request.projectId,
            repoName = request.repoName,
            fullPath = request.fullPath,
            folder = false,
            overwrite = request.overwrite,
            expires = request.expires,
            size = request.size,
            sha256 = FAKE_SHA256,
            md5 = FAKE_MD5,
            operator = user,
            createdBy = user,
            createdDate = LocalDateTime.now(),
            lastModifiedBy = user,
            lastModifiedDate = LocalDateTime.now(),
            nodeMetadata = listOf(fsAttr)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileOperationService::class.java)
    }
}
