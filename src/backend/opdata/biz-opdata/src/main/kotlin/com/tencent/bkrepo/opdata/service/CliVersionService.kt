package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.util.version.SemVersion
import com.tencent.bkrepo.common.artifact.util.version.SemVersionParser
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.opdata.model.TCliVersion
import com.tencent.bkrepo.opdata.pojo.cli.CliVersion
import com.tencent.bkrepo.opdata.pojo.cli.CliVersionCreateRequest
import com.tencent.bkrepo.opdata.repository.CliVersionRepository
import org.springframework.stereotype.Service

@Service
class CliVersionService(
    private val cliVersionRepository: CliVersionRepository
) {

    fun createVersion(request: CliVersionCreateRequest) {
        with(request) {
            Preconditions.checkArgument(SemVersionParser.parse(version) != SemVersion(0, 0, 0), version)
            val version = TCliVersion(version, buildTime, gitCommit, false)
            cliVersionRepository.save(version)
        }
    }

    fun deleteVersion(version: String) {
        cliVersionRepository.deleteByVersion(version)
    }

    fun getVersionList(pageNumber: Int, pageSize: Int): Page<CliVersion> {
        val pageable = Pages.ofRequest(pageNumber, pageSize)
        val cliVersion = cliVersionRepository.findAll(pageable)
        return Pages.ofResponse(pageable, cliVersion.totalElements, cliVersion.content.map { convert(it) })
    }

    fun getLatestVersion(): CliVersion {
        val cliVersion = cliVersionRepository.findByLatest(true)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, "latest cli version")
        return convert(cliVersion)
    }

    fun setLatestVersion(version: String) {
        val cliVersion = cliVersionRepository.findByVersion(version)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, version)
        cliVersionRepository.findByLatest(true)?.run {
            this.latest = false
            cliVersionRepository.save(this)
        }
        cliVersion.latest = true
        cliVersionRepository.save(cliVersion)
    }

    private fun convert(tCliVersion: TCliVersion): CliVersion {
        return CliVersion(
            tCliVersion.version,
            tCliVersion.buildTime,
            tCliVersion.gitCommit,
            tCliVersion.latest
        )
    }
}
