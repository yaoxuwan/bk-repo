package com.tencent.bkrepo.fs.server.request.v2.user

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.fs.server.request.v2.service.NodeCreateRequest
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.awaitBodyOrNull

/**
 * 用户节点创建请求处理类
 * 用于从 ServerRequest 中提取路径参数和请求体
 */
class UserNodeCreateRequest(private val request: ServerRequest) : UserBaseRequest(request) {

    private suspend fun getBody(): UserNodeCreateRequestBody {
        return request.awaitBodyOrNull() ?: throw ErrorCodeException(CommonMessageCode.REQUEST_CONTENT_INVALID)
    }

    suspend fun toNodeCreateRequest(): NodeCreateRequest {
        val body = getBody()
        return NodeCreateRequest(
            projectId = projectId,
            repoName = repoName,
            parentId = body.parentId,
            name = body.name,
            folder = body.folder,
            size = body.size
        )
    }
}