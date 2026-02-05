package com.tencent.bkrepo.fs.server.service

import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.metadata.constant.FAKE_CRC64_ECMA
import com.tencent.bkrepo.common.metadata.constant.FAKE_MD5
import com.tencent.bkrepo.common.metadata.constant.FAKE_SHA256
import com.tencent.bkrepo.common.metadata.dao.node.RNodeDao
import com.tencent.bkrepo.common.metadata.model.NodeAttribute
import com.tencent.bkrepo.common.metadata.model.TMetadata
import com.tencent.bkrepo.common.metadata.model.TNode
import com.tencent.bkrepo.common.metadata.service.metadata.RMetadataService
import com.tencent.bkrepo.fs.server.constant.FS_ATTR_KEY
import com.tencent.bkrepo.fs.server.pojo.DriveNode
import com.tencent.bkrepo.fs.server.request.SyncMetadataRequest
import com.tencent.bkrepo.fs.server.request.SyncOperation
import com.tencent.bkrepo.fs.server.response.SyncErrorCode
import com.tencent.bkrepo.fs.server.response.SyncMetadataResponse
import com.tencent.bkrepo.fs.server.response.SyncOperationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 元数据同步服务
 * 处理客户端批量同步元数据的请求
 */
@Service
class SyncMetadataService(
    private val nodeService: NodeService,
    private val metadataService: RMetadataService,
    private val nodeDao: RNodeDao,
) {
    /**
     * 同步元数据
     * 批量处理创建、更新、删除、移动操作
     */
    suspend fun syncMetadata(
        projectId: String,
        repoName: String,
        request: SyncMetadataRequest,
        operator: String
    ): SyncMetadataResponse {
        logger.info(
            "Sync metadata for $projectId/$repoName, " +
                "clientId=${request.clientId}, batchId=${request.batchId}, " +
                "operations=${request.operations.size}"
        )

        val results = request.operations.map { operation ->
            try {
                processOperation(projectId, repoName, operation, operator)
            } catch (e: Exception) {
                logger.error(
                    "Failed to process operation ${operation.op} for node ${operation.node.id}: ${e.message}",
                    e
                )
                SyncOperationResult.failure(
                    nodeId = operation.node.id,
                    errorCode = SyncErrorCode.INTERNAL_ERROR,
                    error = e.message ?: "Internal error"
                )
            }
        }

        return SyncMetadataResponse(
            results = results,
            serverVersion = System.currentTimeMillis()
        )
    }

    /**
     * 处理单个同步操作
     */
    private suspend fun processOperation(
        projectId: String,
        repoName: String,
        operation: SyncOperation,
        operator: String
    ): SyncOperationResult {
        return when (operation.op) {
            SyncOperation.OP_CREATE -> handleCreate(projectId, repoName, operation.node, operator)
            SyncOperation.OP_UPDATE -> handleUpdate(projectId, repoName, operation.node, operator)
            SyncOperation.OP_DELETE -> handleDelete(projectId, repoName, operation.node, operator)
            SyncOperation.OP_MOVE -> handleMove(projectId, repoName, operation.node, operator)
            else -> SyncOperationResult.failure(
                nodeId = operation.node.id,
                errorCode = SyncErrorCode.INVALID_OPERATION,
                error = "Unknown operation type: ${operation.op}"
            )
        }
    }

    /**
     * 处理创建操作
     */
    private suspend fun handleCreate(
        projectId: String,
        repoName: String,
        node: DriveNode,
        operator: String
    ): SyncOperationResult {
        nodeDao.save(node.convert(projectId, repoName))
        logger.info("Created node: $projectId/$repoName/${node.id}")

        return SyncOperationResult.success(
            nodeId = node.id,
        )
    }

    /**
     * 处理更新操作
     */
    private suspend fun handleUpdate(
        projectId: String,
        repoName: String,
        node: DriveNode,
        operator: String
    ): SyncOperationResult {
        nodeDao.save(node.convert(projectId, repoName))
        logger.info("Updated node: $projectId/$repoName/${node.id}")

        return SyncOperationResult.success(
            nodeId = node.id,
        )
    }

    /**
     * 处理删除操作
     */
    private suspend fun handleDelete(
        projectId: String,
        repoName: String,
        node: DriveNode,
        operator: String
    ): SyncOperationResult {
        nodeDao.save(node.convert(projectId, repoName))
        logger.info("Deleted node: $projectId/$repoName/${node.id}")
        return SyncOperationResult.success(nodeId = node.id)
    }

    /**
     * 处理移动/重命名操作
     */
    private suspend fun handleMove(
        projectId: String,
        repoName: String,
        node: DriveNode,
        operator: String
    ): SyncOperationResult {
        // 检查目标位置是否已存在同名节点
        val existingTarget = nodeService.getNode(projectId, repoName, node.parentId, node.name)
        if (existingTarget != null && existingTarget.id != node.id) {
            return SyncOperationResult.failure(
                nodeId = node.id,
                errorCode = SyncErrorCode.TARGET_EXISTS,
                error = "Target already exists: ${node.parentId}/${node.name}"
            )
        }

        nodeDao.save(node.convert(projectId, repoName))

        logger.info("Moved node: $projectId/$repoName/${node.id} to ${node.parentId}/${node.name}")

        return SyncOperationResult.success(
            nodeId = node.id,
        )
    }

    suspend fun DriveNode.convert(projectId: String, repoName: String): TNode {
        val parentNode = nodeDao.findNodeById(projectId, repoName, parentId)
            ?: throw NodeNotFoundException(parentId)
        val attributes = NodeAttribute(
            uid = uid,
            gid = gid,
            mode = mode,
            flags = flags,
            rdev = rdev,
            type = type
        )
        val fsAttr = TMetadata(
            key = FS_ATTR_KEY,
            value = attributes
        )
        return TNode(
            id = id,
            createdBy = createdBy,
            createdDate = createdDate,
            lastModifiedBy = lastModifiedBy,
            lastModifiedDate = lastModifiedDate,
            lastAccessDate = lastAccessDate,
            folder = folder,
            path = PathUtils.normalizePath(parentNode.fullPath),
            name = name,
            fullPath = PathUtils.combineFullPath(parentNode.fullPath, name),
            size = size,
            sha256 = FAKE_SHA256,
            md5 = FAKE_MD5,
            crc64ecma = FAKE_CRC64_ECMA,
            deleted = deletedAt,
            metadata = mutableListOf(fsAttr),
            parentId = parentId,
            projectId = projectId,
            repoName = repoName
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SyncMetadataService::class.java)
    }
}
