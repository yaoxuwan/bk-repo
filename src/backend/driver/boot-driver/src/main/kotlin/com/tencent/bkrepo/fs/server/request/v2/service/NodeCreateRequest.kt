package com.tencent.bkrepo.fs.server.request.v2.service

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 节点创建请求（服务层）
 */
@Schema(title = "节点创建请求")
data class NodeCreateRequest(
    @get:Schema(title = "节点ID", required = false)
    val id: String? = null,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "仓库名称", required = true)
    val repoName: String,
    @get:Schema(title = "父节点ID", required = true)
    val parentId: String,
    @get:Schema(title = "节点名称", required = true)
    val name: String,
    @get:Schema(title = "是否为文件夹", required = true)
    val folder: Boolean,
    @get:Schema(title = "文件大小（字节）", required = true)
    val size: Long,
    @get:Schema(title = "文件权限（八进制）", required = false)
    val mode: Int?,
    @get:Schema(title = "Windows文件标志（十六进制）", required = false)
    val flags: Int?,
    @get:Schema(title = "设备ID", required = false)
    val rdev: Int?,
    @get:Schema(title = "文件类型", required = false)
    val type: Int?,
)
