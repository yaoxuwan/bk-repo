package com.tencent.bkrepo.fs.server.request.v2.user

import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull

class UserSetLengthRequest(request: ServerRequest) : UserNodeRequest(request) {
    val length: Long = request.queryParamOrNull("length")?.toLong()
        ?: throw ParameterInvalidException("required length parameter.")
}
