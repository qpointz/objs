rootProject.name = "objs"

include(":objs-core")
include(":objs-service")
include(":objs-sbom-example")
include(":objs-gremlin-core")
include(":objs-gremlin-service")
include(":objs-app")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.spring.io/milestone")
    }
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
