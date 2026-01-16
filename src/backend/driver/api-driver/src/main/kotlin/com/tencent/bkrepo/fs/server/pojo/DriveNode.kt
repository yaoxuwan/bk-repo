package com.tencent.bkrepo.fs.server.pojo

import java.time.LocalDateTime

data class DriveNode(
    val id: String,
    val parentId: String,
    val name: String,
    val folder: Boolean,
    val size: Long,
    val path: String,
    val fullPath: String,
    val createdBy: String,
    val createdDate: LocalDateTime,
    val lastModifiedBy: String,
    val lastModifiedDate: LocalDateTime,
    val lastAccessDate: LocalDateTime? = null,
)