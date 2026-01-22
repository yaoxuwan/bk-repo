package com.tencent.bkrepo.fs.server.request.v2.user

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull

class UserChangeAttributeRequest(request: ServerRequest) : UserNodeRequest(request) {
    val uid: String? = request.queryParamOrNull("uid")
    // 组id
    val gid: String? = request.queryParamOrNull("gid")
    // 文件权限，八进制
    val mode: Int? = request.queryParamOrNull("mode")?.toIntOrNull()
    // windows文件flag，十六进制
    val flags: Int? = request.queryParamOrNull("flags")?.toIntOrNull()
    // 设备文件设备号
    val rdev: Int? = request.queryParamOrNull("rdev")?.toIntOrNull()
    // 文件类型
    val type: Int? = request.queryParamOrNull("type")?.toIntOrNull()
}