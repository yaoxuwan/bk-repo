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

package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.cluster.ClusterProperties
import com.tencent.bkrepo.common.service.util.SpringContextUtils

/**
 * 集群操作工具类
 */
object ClusterUtils {

    /**
     * 检查请求来源region是否是资源的唯一拥有者
     */
    fun checkIsSrcRegion(regions: Set<String>?) {
        if (!isUniqueSrcRegion(regions)) {
            throw ErrorCodeException(CommonMessageCode.OPERATION_CROSS_REGION_NOT_ALLOWED)
        }
    }

    /**
     * 判断请求来源region是否拥有资源
     */
    fun containsSrcRegion(regions: Set<String>?): Boolean {
        val clusterProperties = SpringContextUtils.getBean<ClusterProperties>()
        var srcRegion = SecurityUtils.getRegion()

        if (regions == null && srcRegion.isNullOrBlank()) {
            // 兼容旧逻辑
            return true
        } else if (regions == null) {
            // edge plus操作center节点
            return false
        } else if (srcRegion.isNullOrBlank()) {
            // center操作节点
            srcRegion = clusterProperties.region
        }

        return regions.contains(srcRegion)
    }

    /**
     * 判断请求来源region是否是资源的唯一拥有者
     */
    fun isUniqueSrcRegion(regions: Set<String>?): Boolean {
        return containsSrcRegion(regions) && (regions == null || regions.size <= 1)
    }
}