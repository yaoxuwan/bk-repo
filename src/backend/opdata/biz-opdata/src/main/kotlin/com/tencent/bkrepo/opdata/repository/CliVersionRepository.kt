package com.tencent.bkrepo.opdata.repository

import com.tencent.bkrepo.opdata.model.TCliVersion
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CliVersionRepository: MongoRepository<TCliVersion, String> {
    fun findByVersion(version: String): TCliVersion?
    fun findByLatest(latest: Boolean): TCliVersion?
    fun deleteByVersion(version: String)
}
