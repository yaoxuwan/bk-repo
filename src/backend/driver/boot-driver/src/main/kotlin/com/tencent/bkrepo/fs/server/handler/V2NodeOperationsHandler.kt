package com.tencent.bkrepo.fs.server.handler

import com.tencent.bkrepo.common.metadata.model.NodeAttribute
import com.tencent.bkrepo.common.metadata.model.NodeAttribute.Companion.NOBODY
import com.tencent.bkrepo.common.metadata.service.fs.FsService
import com.tencent.bkrepo.common.metadata.service.metadata.RMetadataService
import com.tencent.bkrepo.common.metadata.service.repo.RRepositoryService
import com.tencent.bkrepo.fs.server.constant.FS_ATTR_KEY
import com.tencent.bkrepo.fs.server.context.ReactiveArtifactContextHolder
import com.tencent.bkrepo.fs.server.request.v2.user.UserBaseRequest
import com.tencent.bkrepo.fs.server.request.v2.user.UserChangeAttributeRequest
import com.tencent.bkrepo.fs.server.request.v2.user.UserMoveRequest
import com.tencent.bkrepo.fs.server.request.v2.user.UserNodeAttrRequest
import com.tencent.bkrepo.fs.server.request.v2.user.UserNodeCreateRequest
import com.tencent.bkrepo.fs.server.request.v2.user.UserNodeDeleteRequest
import com.tencent.bkrepo.fs.server.request.v2.user.UserNodePageRequest
import com.tencent.bkrepo.fs.server.request.v2.user.UserSetLengthRequest
import com.tencent.bkrepo.fs.server.response.StatResponse
import com.tencent.bkrepo.fs.server.service.FileNodeService
import com.tencent.bkrepo.fs.server.service.NodeService
import com.tencent.bkrepo.fs.server.utils.ReactiveResponseBuilder
import com.tencent.bkrepo.fs.server.utils.ReactiveSecurityUtils
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactivefeign.client.ReadTimeoutException

/**
 * 节点操作相关的处理器
 *
 * 处理节点操作的请求
 * */
@Component
class V2NodeOperationsHandler(
    private val fileNodeService: FileNodeService,
    private val fsService: FsService,
    private val nodeService: NodeService,
    private val repositoryService: RRepositoryService,
    private val metadataService: RMetadataService
) {

    suspend fun getNode(request: ServerRequest): ServerResponse {
        with(UserNodeAttrRequest(request)) {
            val node = nodeService.getNode(projectId, repoName, id)
            return ReactiveResponseBuilder.success(node)
        }
    }

    suspend fun listNodes(request: ServerRequest): ServerResponse {
        val pageRequest = UserNodePageRequest(request)
        with(pageRequest) {
            val nodes = if (name.isNullOrBlank()) {
                nodeService.listNode(projectId, repoName, id, offset)
            } else {
                nodeService.getNode(projectId, repoName, id, name)?.let { listOf(it) } ?: emptyList()
            }
            return ReactiveResponseBuilder.success(nodes)
        }
    }

    /**
     * 删除节点
     * */
    suspend fun deleteNode(request: ServerRequest): ServerResponse {
        with(UserNodeDeleteRequest(request)) {
            nodeService.deleteNode(projectId, repoName, id)
//            fileNodeService.deleteNodeBlocks(projectId, repoName, fullPath)
            return ReactiveResponseBuilder.success()
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun changeAttribute(request: ServerRequest): ServerResponse {
        with(UserChangeAttributeRequest(request)) {
            val node = nodeService.getNode(projectId, repoName, id)!!

            val attributes = NodeAttribute(
                uid = uid ?: node.uid ?: NOBODY,
                gid = gid ?: node.gid ?: NOBODY,
                mode = mode ?: node.mode,
                flags = flags ?: node.flags,
                rdev = rdev ?: node.rdev,
                type = type ?: node.type
            )
            val fsAttr = MetadataModel(
                key = FS_ATTR_KEY,
                value = attributes
            )
            // TODO: 根据id保存metadata
//            val saveMetaDataRequest = MetadataSaveRequest(
//                projectId = projectId,
//                repoName = repoName,
//                fullPath = node.fullPath,
//                nodeMetadata = listOf(fsAttr),
//                operator = ReactiveSecurityUtils.getUser()
//            )
//            metadataService.saveMetadata(saveMetaDataRequest)
            return ReactiveResponseBuilder.success(attributes)
        }
    }

    suspend fun getStat(request: ServerRequest): ServerResponse {
        with(UserBaseRequest(request)) {
            val cap = ReactiveArtifactContextHolder.getRepoDetail().quota
            val nodeStat = try {
                repositoryService.statRepo(projectId, repoName)
            } catch (_: ReadTimeoutException) {
                logger.warn("get repo[$projectId/$repoName] stat timeout")
                NodeSizeInfo(0, 0, UNKNOWN)
            }

            val res = StatResponse(
                subNodeCount = nodeStat.subNodeCount,
                size = nodeStat.size,
                capacity = cap ?: UNKNOWN
            )

            return ReactiveResponseBuilder.success(res)
        }
    }

    /**
     * 移动节点
     * */
    suspend fun move(request: ServerRequest): ServerResponse {
        with(UserMoveRequest(request)) {
            val src = nodeService.getNode(projectId, repoName, id)!!
            val dstParent = nodeService.getNode(projectId, repoName, dstParentId)!!
            val dst = nodeService.getNode(projectId, repoName, dstParentId, dstName)
            if (overwrite) {
                dst?.run { nodeService.deleteNode(projectId, repoName, dst.id) }
            }
            nodeService.renameNode(projectId, repoName, src, dstParent, dstName)
//            fileNodeService.renameNodeBlocks(projectId, repoName, fullPath, dst)
            return ReactiveResponseBuilder.success()
        }
    }

//    suspend fun info(request: ServerRequest): ServerResponse {
//        with(V2NodeRequest(request)) {
//            val nodeDetail = nodeService.getNode(projectId, repoName, id)
//            val range = try {
//                request.resolveRange(nodeDetail.size)
//            } catch (e: IllegalArgumentException) {
//                logger.info("read file[$projectId/$repoName$id] failed: ${e.message}")
//                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, HttpHeaders.RANGE)
//            }
//            val blocks = fileNodeService.info(nodeDetail, range)
//            val newBlocks = OverlayRangeUtils.build(blocks, range)
//            return ReactiveResponseBuilder.success(newBlocks)
//        }
//    }

    suspend fun createNode(request: ServerRequest): ServerResponse {
        val createRequest = UserNodeCreateRequest(request).toNodeCreateRequest()
        val node = nodeService.createNode(createRequest)
        return ReactiveResponseBuilder.success(node)
    }

//    suspend fun symlink(request: ServerRequest): ServerResponse {
//        val nodeLinkRequest = with(LinkRequest(request)) {
//            NodeLinkRequest(
//                projectId = projectId,
//                repoName = repoName,
//                fullPath = fullPath,
//                targetProjectId = projectId,
//                targetRepoName = repoName,
//                targetFullPath = targetFullPath,
//                checkTargetExist = false,
//                operator = ReactiveSecurityUtils.getUser()
//            )
//        }
//        val node = nodeService.link(nodeLinkRequest)
//        return ReactiveResponseBuilder.success(node.nodeInfo.toNode())
//    }

//    private suspend fun createNode(request: ServerRequest, folder: Boolean): NodeDetail {
//        with(NodeRequest(request)) {
//            val user = ReactiveSecurityUtils.getUser()
//            // 创建节点
//            val attributes = NodeAttribute(
//                uid = NOBODY,
//                gid = NOBODY,
//                mode = mode ?: DEFAULT_MODE,
//                flags = flags,
//                rdev = rdev,
//                type = type
//            )
//            val fsAttr = MetadataModel(
//                key = FS_ATTR_KEY,
//                value = attributes
//            )
//
//            val nodeCreateRequest = NodeCreateRequest(
//                projectId = projectId,
//                repoName = repoName,
//                folder = folder,
//                fullPath = fullPath,
//                sha256 = FAKE_SHA256,
//                md5 = FAKE_MD5,
//                crc64ecma = FAKE_CRC64_ECMA,
//                nodeMetadata = listOf(fsAttr),
//                operator = user
//            )
//            return fsService.createNode(nodeCreateRequest)
//        }
//    }

    suspend fun setLength(request: ServerRequest): ServerResponse {
        with(UserSetLengthRequest(request)) {
            nodeService.updateSize(projectId, repoName, id, length)
            return ReactiveResponseBuilder.success()
        }
    }

    companion object {
        private const val UNKNOWN = -1L
        private val logger = LoggerFactory.getLogger(V2NodeOperationsHandler::class.java)
    }
}
