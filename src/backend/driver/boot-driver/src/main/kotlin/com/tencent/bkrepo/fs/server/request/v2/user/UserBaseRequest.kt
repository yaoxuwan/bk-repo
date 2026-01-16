package com.tencent.bkrepo.fs.server.request.v2.user

import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.repository.constant.REPO_NAME
import org.springframework.web.reactive.function.server.ServerRequest

open class UserBaseRequest(
    open val projectId: String,
    open val repoName: String,
) {
    constructor(request: ServerRequest) : this(
        projectId = request.pathVariable(PROJECT_ID),
        repoName = request.pathVariable(REPO_NAME),
    )
}