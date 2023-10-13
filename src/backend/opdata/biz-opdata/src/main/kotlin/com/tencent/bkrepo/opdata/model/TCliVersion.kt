package com.tencent.bkrepo.opdata.model

import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("cli_version")
data class TCliVersion(
    val version: String,
    val buildTime: LocalDateTime,
    val gitCommit: String,
    var latest: Boolean
)
