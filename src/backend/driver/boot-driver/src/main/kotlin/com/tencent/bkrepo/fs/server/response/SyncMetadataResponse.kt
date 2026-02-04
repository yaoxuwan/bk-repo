package com.tencent.bkrepo.fs.server.response

/**
 * 元数据同步响应
 */
data class SyncMetadataResponse(
    /**
     * 每个操作的结果
     */
    val results: List<SyncOperationResult>,

    /**
     * 服务端当前版本号
     */
    val serverVersion: Long? = null
)

/**
 * 单个操作的结果
 */
data class SyncOperationResult(
    /**
     * 节点ID
     */
    val nodeId: String,

    /**
     * 是否成功
     */
    val success: Boolean,

    /**
     * 错误信息（失败时）
     */
    val error: String? = null,

    /**
     * 错误码（失败时）
     */
    val errorCode: String? = null
) {
    companion object {
        /**
         * 创建成功结果
         */
        fun success(nodeId: String): SyncOperationResult {
            return SyncOperationResult(
                nodeId = nodeId,
                success = true,
            )
        }

        /**
         * 创建失败结果
         */
        fun failure(nodeId: String, errorCode: String, error: String): SyncOperationResult {
            return SyncOperationResult(
                nodeId = nodeId,
                success = false,
                errorCode = errorCode,
                error = error
            )
        }
    }
}

/**
 * 同步操作错误码
 */
object SyncErrorCode {
    const val NODE_NOT_FOUND = "NODE_NOT_FOUND"
    const val PARENT_NOT_FOUND = "PARENT_NOT_FOUND"
    const val TARGET_EXISTS = "TARGET_EXISTS"
    const val INVALID_OPERATION = "INVALID_OPERATION"
    const val PERMISSION_DENIED = "PERMISSION_DENIED"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
    const val INVALID_PATH = "INVALID_PATH"
}
