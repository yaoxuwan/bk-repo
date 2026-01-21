package com.tencent.bkrepo.fs.server.request

interface BlockWriteRequest {
    val projectId: String
    val repoName: String
    val fullPath: String
    val offset: Long
}
