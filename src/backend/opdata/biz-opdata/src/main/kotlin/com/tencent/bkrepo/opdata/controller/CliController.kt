package com.tencent.bkrepo.opdata.controller

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.opdata.pojo.cli.CliVersion
import com.tencent.bkrepo.opdata.pojo.cli.CliVersionCreateRequest
import com.tencent.bkrepo.opdata.service.CliVersionService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cli")
@Principal(PrincipalType.ADMIN)
class CliController(
    private val cliVersionService: CliVersionService
) {

    @PostMapping("/version/create")

    fun createVersion(@RequestBody request: CliVersionCreateRequest): Response<Void> {
        cliVersionService.createVersion(request)
        return ResponseBuilder.success()
    }

    @DeleteMapping("/version/{version}")
    fun deleteVersion(@PathVariable version: String): Response<Void> {
        cliVersionService.deleteVersion(version)
        return ResponseBuilder.success()
    }

    @GetMapping("/version/page")
    fun getVersions(
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Response<Page<CliVersion>> {
        return ResponseBuilder.success(cliVersionService.getVersionList(page, size))
    }

    @Principal(PrincipalType.GENERAL)
    @GetMapping("/version/latest")
    fun getLatestVersion(): Response<CliVersion> {
        return ResponseBuilder.success(cliVersionService.getLatestVersion())
    }

    @PostMapping("/version/latest/{version}")
    fun setLatestVersion(@PathVariable version: String): Response<Void> {
        cliVersionService.setLatestVersion(version)
        return ResponseBuilder.success()
    }
}
