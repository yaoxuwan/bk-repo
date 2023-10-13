package com.tencent.bkrepo.opdata.pojo.cli

import java.time.LocalDateTime

data class CliVersion(
    val version: String,
    val buildTime: LocalDateTime,
    val gitCommit: String,
    var latest: Boolean
)
