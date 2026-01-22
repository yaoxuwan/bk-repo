package com.tencent.bkrepo.fs.server.request.v2.user

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull

class UserMoveRequest(request: ServerRequest) : UserNodeRequest(request) {
    val dstParentId: String = request.queryParamOrNull("dstParentId")
        ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "dstParentId")
    val dstName: String = request.queryParamOrNull("dstName")
        ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "dstName")
    val overwrite: Boolean = request.queryParamOrNull("overwrite")?.toBoolean() ?: false
}