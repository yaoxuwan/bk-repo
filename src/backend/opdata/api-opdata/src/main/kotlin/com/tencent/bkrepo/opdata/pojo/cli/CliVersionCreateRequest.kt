package com.tencent.bkrepo.opdata.pojo.cli

import java.time.LocalDateTime

data class CliVersionCreateRequest(
    val version: String,
    val buildTime: LocalDateTime,
    val gitCommit: String
)
