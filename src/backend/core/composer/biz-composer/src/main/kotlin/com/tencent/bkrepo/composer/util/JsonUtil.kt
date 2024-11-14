/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.composer.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode

object JsonUtil {

    val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private const val PACKAGES = "packages"
    private const val DIST = "dist"
    private const val URL = "url"
    private const val DOWNLOAD_REDIRECT_URL = "providers-lazy-url"
    private const val SEARCH = "search"

    init {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    /**
     * get value with json-param, if string is json-format
     * @param param json 属性
     */
    infix fun String.jsonValue(param: String): String {
        val jsonObject = JsonParser.parseString(this).asJsonObject
        try {
            return jsonObject.get(param).asString
        } catch (_: IllegalStateException) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "composer.json `$param`")
        }
    }

    /**
     * 在composer包的json加入到%package%.json时添加"dist"属性
     * "dist"属性包含文件压缩格式，download地址
     * @param host 服务器地址
     * @param packageName 包名
     */
    @Throws(Exception::class)
    fun String.wrapperJson(host: String, packageName: String): String {
        val jsonObject = JsonParser.parseString(this).asJsonObject
        val versions = jsonObject.get(PACKAGES).asJsonObject.get(packageName).asJsonObject
        for (it in versions.entrySet()) {
            val uri = it.value.asJsonObject.get(DIST).asJsonObject.get(URL).asString
            val downloadUrl = "$host/$uri"
            it.value.asJsonObject.get(DIST).asJsonObject.addProperty(URL, downloadUrl)
        }
        return GsonBuilder().create().toJson(jsonObject)
    }

    /**
     * 包装packages.json
     * @param host 服务器地址
     */
    @Throws(Exception::class)
    fun String.wrapperPackageJson(host: String): String {
        val jsonObject = JsonParser.parseString(this).asJsonObject
        jsonObject.get(SEARCH).asString?.let {
            jsonObject.addProperty(SEARCH, "$host$it")
        }
        jsonObject.get(DOWNLOAD_REDIRECT_URL).asString?.let {
            jsonObject.addProperty(DOWNLOAD_REDIRECT_URL, "$host$it")
        }
        return GsonBuilder().create().toJson(jsonObject)
    }

    /**
     *  add new version to %package%.json
     * @param versionJson exists %package%.json
     * @param uploadFileJson new version json content
     * @param name
     * @param version
     */
    @Throws(Exception::class)
    fun addComposerVersion(versionJson: String, uploadFileJson: String, name: String, version: String): String {
        val jsonObject = JsonParser.parseString(versionJson).asJsonObject
        val nameParam = jsonObject.getAsJsonObject(PACKAGES).getAsJsonObject(name)
        // 覆盖重复版本信息
        nameParam.add(version, JsonParser.parseString(uploadFileJson))
        return GsonBuilder().create().toJson(jsonObject)
    }

    fun deleteComposerVersion(versionJson: String, name: String, version: String): String {
        val jsonObject = JsonParser.parseString(versionJson).asJsonObject
        val nameParam = jsonObject.getAsJsonObject(PACKAGES).getAsJsonObject(name)
        nameParam.remove(version)
        return GsonBuilder().create().toJson(jsonObject)
    }
}
