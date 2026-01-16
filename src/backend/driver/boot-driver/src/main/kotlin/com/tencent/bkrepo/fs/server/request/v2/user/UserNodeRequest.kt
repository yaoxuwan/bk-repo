package com.tencent.bkrepo.fs.server.request.v2.user

import org.springframework.web.reactive.function.server.ServerRequest

open class UserNodeRequest(request: ServerRequest) : UserBaseRequest(request) {
    open val id: String = request.pathVariable("id")
}