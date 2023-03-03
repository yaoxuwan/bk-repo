/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.repository.service.packages.impl.center

import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.cluster.ClusterProperties
import com.tencent.bkrepo.common.service.cluster.StarCenterCondition
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.pojo.packages.request.PackagePopulateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.search.packages.PackageSearchInterpreter
import com.tencent.bkrepo.repository.service.packages.impl.PackageServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

/**
 * Star组网方式的Center节点Package管理服务
 */
@Service
@Conditional(StarCenterCondition::class)
class StarCenterPackageServiceImpl(
    packageDao: PackageDao,
    packageVersionDao: PackageVersionDao,
    packageSearchInterpreter: PackageSearchInterpreter,
    clusterProperties: ClusterProperties
) : PackageServiceImpl(
    packageDao,
    packageVersionDao,
    packageSearchInterpreter,
    clusterProperties
) {
    override fun buildPackage(request: PackageVersionCreateRequest): TPackage {
        return super.buildPackage(request).also { addSrcRegionToPackage(it) }
    }

    override fun buildPackage(request: PackagePopulateRequest): TPackage {
        return super.buildPackage(request).also { addSrcRegionToPackage(it) }
    }

    /**
     * 获取已存在的Package或创建Package，会将当前的操作来源region添加到package region中
     */
    override fun findOrCreatePackage(tPackage: TPackage): TPackage {
        with(tPackage) {
            val savedPackage = packageDao.findByKey(projectId, repoName, key)
            val srcRegion = srcRegion()

            if (savedPackage != null && srcRegion.isNotEmpty() && savedPackage.region?.contains(srcRegion) == false) {
                val result = packageDao.addRegionByKey(projectId, repoName, key, srcRegion)
                addSrcRegionToPackage(savedPackage, srcRegion)
                logger.info("Update package[$tPackage] region[$srcRegion] result[${result?.modifiedCount}]")
            }

            return savedPackage ?: createPackage(tPackage)
        }
    }

    private fun createPackage(tPackage: TPackage): TPackage {
        val srcRegion = srcRegion()
        with(tPackage) {
            try {
                val savedPackage = packageDao.save(tPackage)
                logger.info("Create package[$tPackage] success")
                return savedPackage
            } catch (exception: DuplicateKeyException) {
                logger.warn("Create package[$tPackage] error: [${exception.message}]")
                val result = packageDao.addRegionByKey(projectId, repoName, key, srcRegion)
                logger.info("Update package[$tPackage] region[$srcRegion] result[${result?.modifiedCount}]")
            }
            return packageDao.findByKey(projectId, repoName, key)!!
        }
    }

    private fun addSrcRegionToPackage(tPackage: TPackage, srcRegion: String = srcRegion()) {
        val oldRegion = tPackage.region ?: mutableSetOf()
        tPackage.region = oldRegion + srcRegion
    }

    private fun srcRegion() = SecurityUtils.getRegion() ?: clusterProperties.region!!

    companion object {
        private val logger = LoggerFactory.getLogger(StarCenterPackageServiceImpl::class.java)
    }
}
