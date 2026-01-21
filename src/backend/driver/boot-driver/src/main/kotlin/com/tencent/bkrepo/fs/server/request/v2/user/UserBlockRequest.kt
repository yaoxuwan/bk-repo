package com.tencent.bkrepo.fs.server.request.v2.user

import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.fs.server.request.BlockWriteRequest
import com.tencent.bkrepo.fs.server.request.NodeRequest
import org.springframework.web.reactive.function.server.ServerRequest

class UserBlockRequest(
    projectId: String,
    repoName: String,
    fullPath: String,
    override val offset: Long
) : NodeRequest(projectId, repoName, fullPath), BlockWriteRequest {
    constructor(request: ServerRequest, fullPath: String) : this(
        projectId = request.pathVariable(PROJECT_ID),
        repoName = request.pathVariable(REPO_NAME),
        fullPath = fullPath,
        offset = request.pathVariable("offset").toLong()
    )
}
