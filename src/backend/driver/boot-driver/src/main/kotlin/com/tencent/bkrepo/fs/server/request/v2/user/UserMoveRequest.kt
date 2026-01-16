package com.tencent.bkrepo.fs.server.request.v2.user

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull

class UserMoveRequest(request: ServerRequest) : UserNodeRequest(request) {
    val dstParentId: String = request.queryParam("dstParentId").toString()
    val dstName: String = request.queryParam("dstName").toString()
    val overwrite: Boolean = request.queryParamOrNull("overwrite")?.toBoolean() ?: false
}