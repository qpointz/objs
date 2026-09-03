rootProject.name = "objs"

include(":objs-api")
include(":objs-codegen-java")
include(":objs-persistence")
include(":objs-autoconfigure")
include(":objs-service")
include(":objs-service-ui")
include(":objs-gremlin-core")
include(":objs-gremlin-service")
include(":objs-jgrapht-core")
include(":objs-jgrapht-service")
include(":objs-service-app")

include(":sbom-service")
include(":sbom-service-ui")
project(":sbom-service").projectDir = file("examples/sbom/sbom-service")
project(":sbom-service-ui").projectDir = file("examples/sbom/sbom-service-ui")

include(":asset-repository-service")
include(":asset-repository-service-ui")
project(":asset-repository-service").projectDir = file("examples/asset-repository/asset-repository-service")
project(":asset-repository-service-ui").projectDir = file("examples/asset-repository/asset-repository-service-ui")

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
