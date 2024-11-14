package com.tencent.bkrepo.archive.repository

import com.tencent.bkrepo.archive.ArchiveStatus
import com.tencent.bkrepo.archive.model.TArchiveFile
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ArchiveFileRepository : MongoRepository<TArchiveFile, String> {
    fun findBySha256AndStorageCredentialsKey(sha256: String, key: String?): TArchiveFile?
    fun countByLastModifiedDateAfterAndStatus(date: LocalDateTime, status: ArchiveStatus): Int
    fun countByStatus(status: ArchiveStatus): Int
}
