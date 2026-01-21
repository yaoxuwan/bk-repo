package com.tencent.bkrepo.fs.server.request.v2.user

import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.fs.server.request.NodeRequest
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull

class UserFlushRequest(
    projectId: String,
    repoName: String,
    fullPath: String,
    val length: Long
) : NodeRequest(projectId, repoName, fullPath) {
    constructor(request: ServerRequest, fullPath: String) : this(
        projectId = request.pathVariable(PROJECT_ID),
        repoName = request.pathVariable(REPO_NAME),
        fullPath = fullPath,
        length = request.queryParamOrNull("length")?.toLong()
            ?: throw ParameterInvalidException("required length parameter.")
    )
}
