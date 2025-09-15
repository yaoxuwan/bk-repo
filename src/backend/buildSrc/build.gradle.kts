import java.net.URI

plugins {
    `kotlin-dsl`
}

repositories {
    maven {
        name = "Central Portal Snapshots"
        url = URI("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
    if (System.getenv("GITHUB_WORKFLOW") == null) { // 普通环境
        maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public")
        maven(url = "https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
    } else { // GitHub Action 环境
        mavenCentral()
        gradlePluginPortal()
    }
    mavenLocal()
}
