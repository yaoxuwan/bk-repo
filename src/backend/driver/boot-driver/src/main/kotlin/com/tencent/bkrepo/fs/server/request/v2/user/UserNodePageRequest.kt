package com.tencent.bkrepo.fs.server.request.v2.user

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull

class UserNodePageRequest(request: ServerRequest) : UserNodeRequest(request) {
    val offset: Long = request.queryParamOrNull("offset")?.toLong() ?: 0L
}