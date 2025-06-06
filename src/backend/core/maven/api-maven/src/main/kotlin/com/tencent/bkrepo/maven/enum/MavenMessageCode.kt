package com.tencent.bkrepo.maven.enum

import com.tencent.bkrepo.common.api.message.MessageCode

enum class MavenMessageCode(private val key: String) : MessageCode {
    MAVEN_CHECKSUM_CONFLICT("maven.checksum.conflict"),
    MAVEN_ARTIFACT_FORMAT_ERROR("maven.artifact.format.error"),
    MAVEN_ARTIFACT_NOT_FOUND("maven.artifact.not.found"),
    MAVEN_METADATA_CHECKSUM_EXCEPTION("maven.metadata.checksum.exception"),
    MAVEN_PATH_PARSER_ERROR("maven.path.parser.error"),
    MAVEN_REQUEST_FORBIDDEN("maven.request.forbidden"),
    MAVEN_PROPERTIES_MISSING("maven.properties.missing"),
    MAVEN_ARTIFACT_DELETE("maven.artifact.delete"),
    MAVEN_ARTIFACT_UPLOAD("maven.artifact.upload"),
    FILE_RESOLVE_FAILED("file.resolve.failed"),
    PARAMETER_CONTAINS_INVALID("parameter.contains.invalid"),
    PARAMETER_EXPECT("parameter.expect"),
    MAVEN_ARTIFACT_COVER_FORBIDDEN("maven.artifact.cover.forbidden"),
    MAVEN_ARTIFACT_DELETE_FORBIDDEN("maven.artifact.delete.forbidden"),
    MAVEN_VERSION_NOT_FOUND("maven.version.not.found"),
    MAVEN_VERSION_RESOLVE_FAILED("maven.version.resolve.failed")
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 21
}
