package com.tencent.bkrepo.fs.server.handler

import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.fs.server.bodyToArtifactFile
import com.tencent.bkrepo.fs.server.request.v2.user.UserNodeRequest
import com.tencent.bkrepo.fs.server.service.FileOperationService
import com.tencent.bkrepo.fs.server.utils.ReactiveResponseBuilder
import com.tencent.bkrepo.fs.server.utils.ReactiveSecurityUtils
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.queryParamOrNull

@Component
class V2BlockOperationsHandler(
    private val fileOperationService: FileOperationService
) {

    suspend fun write(request: ServerRequest): ServerResponse {
        val user = ReactiveSecurityUtils.getUser()
        val artifactFile = request.bodyToArtifactFile()
        val result = with(UserNodeRequest(request)) {
            val offset = request.pathVariable("offset").toLong()
            fileOperationService.writeByNodeId(artifactFile, projectId, repoName, id, offset, user)
        }
        setArtifactInfo(request, result.fullPath)
        return ReactiveResponseBuilder.success(result.blockNode)
    }

    suspend fun writeAndFlush(request: ServerRequest): ServerResponse {
        val user = ReactiveSecurityUtils.getUser()
        val artifactFile = request.bodyToArtifactFile()
        val (blockNode, fullPath) = with(UserNodeRequest(request)) {
            val offset = request.pathVariable("offset").toLong()
            fileOperationService.writeByNodeId(artifactFile, projectId, repoName, id, offset, user)
        }
        with(UserNodeRequest(request)) {
            val length = resolveLength(request)
            fileOperationService.flushByNodeId(projectId, repoName, id, length, user, fullPath)
        }
        setArtifactInfo(request, fullPath)
        return ReactiveResponseBuilder.success(blockNode)
    }

    suspend fun flush(request: ServerRequest): ServerResponse {
        val user = ReactiveSecurityUtils.getUser()
        val fullPath = with(UserNodeRequest(request)) {
            val length = resolveLength(request)
            fileOperationService.flushByNodeId(projectId, repoName, id, length, user)
        }
        setArtifactInfo(request, fullPath)
        return ReactiveResponseBuilder.success()
    }

    private fun resolveLength(request: ServerRequest): Long {
        return request.queryParamOrNull("length")?.toLong()
            ?: throw ParameterInvalidException("required length parameter.")
    }

    private fun setArtifactInfo(request: ServerRequest, fullPath: String) {
        with(UserNodeRequest(request)) {
            request.exchange().attributes[ARTIFACT_INFO_KEY] = ArtifactInfo(projectId, repoName, fullPath)
        }
    }
}
