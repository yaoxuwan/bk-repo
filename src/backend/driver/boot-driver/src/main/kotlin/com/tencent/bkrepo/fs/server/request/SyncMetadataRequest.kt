package com.tencent.bkrepo.fs.server.request

import com.tencent.bkrepo.fs.server.pojo.DriveNode

/**
 * 元数据同步请求
 * 支持批量同步多个节点的多种操作
 */
data class SyncMetadataRequest(
    /**
     * 同步操作列表
     * 一个请求可包含多个节点的多种操作
     */
    val operations: List<SyncOperation>,

    /**
     * 客户端标识（用于去重和日志追踪）
     */
    val clientId: String? = null,

    /**
     * 批次ID（可选，用于幂等）
     */
    val batchId: String? = null
)

/**
 * 单个同步操作
 * 使用完整 DriveNode 结构携带操作数据
 */
data class SyncOperation(
    /**
     * 操作类型
     * - create: 创建节点
     * - update: 更新节点
     * - delete: 删除节点
     * - move: 移动/重命名节点
     */
    val op: String,

    /**
     * 节点数据
     * - create: 包含完整的新建节点信息
     * - update: 包含需要更新的节点信息
     * - delete: 包含 id、deleted=true、deletedAt
     * - move: 包含 id、parentId(新父节点)、name(新名称)
     */
    val node: DriveNode
) {
    companion object {
        const val OP_CREATE = "create"
        const val OP_UPDATE = "update"
        const val OP_DELETE = "delete"
        const val OP_MOVE = "move"
    }
}
