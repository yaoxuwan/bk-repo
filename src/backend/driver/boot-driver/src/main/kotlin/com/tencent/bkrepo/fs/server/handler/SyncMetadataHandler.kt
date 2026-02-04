package com.tencent.bkrepo.fs.server.handler

import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.fs.server.request.SyncMetadataRequest
import com.tencent.bkrepo.fs.server.service.SyncMetadataService
import com.tencent.bkrepo.fs.server.utils.ReactiveResponseBuilder
import com.tencent.bkrepo.fs.server.utils.ReactiveSecurityUtils
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody

/**
 * 元数据同步处理器
 * 处理客户端批量同步元数据的请求
 */
@Component
class SyncMetadataHandler(
    private val syncMetadataService: SyncMetadataService
) {
    /**
     * 同步元数据
     * POST /v2/node/sync/{projectId}/{repoName}
     */
    suspend fun syncMetadata(request: ServerRequest): ServerResponse {
        val projectId = request.pathVariable(PROJECT_ID)
        val repoName = request.pathVariable(REPO_NAME)
        val operator = ReactiveSecurityUtils.getUser()

        val syncRequest = request.awaitBody<SyncMetadataRequest>()

        val response = syncMetadataService.syncMetadata(
            projectId = projectId,
            repoName = repoName,
            request = syncRequest,
            operator = operator
        )

        return ReactiveResponseBuilder.success(response)
    }
}
