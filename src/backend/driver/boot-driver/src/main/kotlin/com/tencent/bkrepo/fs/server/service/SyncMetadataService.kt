package com.tencent.bkrepo.fs.server.service

import com.tencent.bkrepo.common.metadata.dao.node.NodeDao
import com.tencent.bkrepo.common.metadata.dao.node.RNodeDao
import com.tencent.bkrepo.common.metadata.model.NodeAttribute
import com.tencent.bkrepo.common.metadata.service.metadata.RMetadataService
import com.tencent.bkrepo.fs.server.constant.FS_ATTR_KEY
import com.tencent.bkrepo.fs.server.pojo.DriveNode
import com.tencent.bkrepo.fs.server.request.SyncMetadataRequest
import com.tencent.bkrepo.fs.server.request.SyncOperation
import com.tencent.bkrepo.fs.server.request.v2.service.NodeCreateRequest
import com.tencent.bkrepo.fs.server.response.SyncErrorCode
import com.tencent.bkrepo.fs.server.response.SyncMetadataResponse
import com.tencent.bkrepo.fs.server.response.SyncOperationResult
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
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
        // 构建文件属性
        val attributes = NodeAttribute(
            uid = node.uid,
            gid = node.gid,
            mode = node.mode,
            flags = node.flags,
            rdev = node.rdev,
            type = node.type
        )
        val fsAttr = MetadataModel(
            key = FS_ATTR_KEY,
            value = attributes
        )

        val createRequest = NodeCreateRequest(
            id = node.id,
            projectId = projectId,
            repoName = repoName,
            parentId = node.parentId,
            name = node.name,
            folder = node.folder,
            size = node.size,
            mode = node.mode,
            flags = node.flags,
            rdev = node.rdev,
            type = node.type
        )

        val createdNode = nodeService.createNode(createRequest)
        logger.info("Created node: $projectId/$repoName/${createdNode.id}")

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
        // 检查节点是否存在
        val existingNode = nodeService.getNode(projectId, repoName, node.id)
            ?: return SyncOperationResult.failure(
                nodeId = node.id,
                errorCode = SyncErrorCode.NODE_NOT_FOUND,
                error = "Node not found: ${node.id}"
            )

        // 更新节点大小（如果有变化）
        if (node.size != existingNode.size) {
            nodeService.updateSize(projectId, repoName, node.id, node.size)
        }

        // 更新元数据
        val attributes = NodeAttribute(
            uid = node.uid,
            gid = node.gid,
            mode = node.mode,
            flags = node.flags,
            rdev = node.rdev,
            type = node.type
        )
        val fsAttr = MetadataModel(
            key = FS_ATTR_KEY,
            value = attributes
        )

        val saveRequest = MetadataSaveRequest(
            projectId = projectId,
            repoName = repoName,
            fullPath = nodeDao.findNodeById(projectId, repoName, node.id)!!.fullPath,
            nodeMetadata = listOf(fsAttr),
            operator = operator
        )
        metadataService.saveMetadata(saveRequest)

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
        // 检查节点是否存在
        val existingNode = nodeService.getNode(projectId, repoName, node.id)
        if (existingNode == null) {
            // 节点不存在，可能已经被删除，返回成功
            logger.warn("Node already deleted or not found: $projectId/$repoName/${node.id}")
            return SyncOperationResult.success(nodeId = node.id)
        }

        // 删除节点
        val deleted = nodeService.deleteNode(projectId, repoName, node.id)
        if (!deleted) {
            return SyncOperationResult.failure(
                nodeId = node.id,
                errorCode = SyncErrorCode.INTERNAL_ERROR,
                error = "Failed to delete node: ${node.id}"
            )
        }

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
        // 检查源节点是否存在
        val srcNode = nodeService.getNode(projectId, repoName, node.id)
            ?: return SyncOperationResult.failure(
                nodeId = node.id,
                errorCode = SyncErrorCode.NODE_NOT_FOUND,
                error = "Source node not found: ${node.id}"
            )

        // 检查目标父节点是否存在
        val dstParentNode = nodeService.getNode(projectId, repoName, node.parentId)
            ?: return SyncOperationResult.failure(
                nodeId = node.id,
                errorCode = SyncErrorCode.PARENT_NOT_FOUND,
                error = "Target parent node not found: ${node.parentId}"
            )

        // 检查目标位置是否已存在同名节点
        val existingTarget = nodeService.getNode(projectId, repoName, node.parentId, node.name)
        if (existingTarget != null && existingTarget.id != node.id) {
            return SyncOperationResult.failure(
                nodeId = node.id,
                errorCode = SyncErrorCode.TARGET_EXISTS,
                error = "Target already exists: ${node.parentId}/${node.name}"
            )
        }

        // 执行重命名/移动操作
        nodeService.renameNode(projectId, repoName, srcNode, dstParentNode, node.name)

        logger.info("Moved node: $projectId/$repoName/${node.id} to ${node.parentId}/${node.name}")

        return SyncOperationResult.success(
            nodeId = node.id,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SyncMetadataService::class.java)
    }
}
