import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    java
    application
}

description = "Asset repository example — Java 21 Boot app (objs as object store)."

dependencies {
    implementation(platform(libs.boot.dependencies))
    implementation(project(":objs-service"))
    implementation(libs.boot.starter.webmvc)
    implementation(libs.boot.starter.data.jpa)
    implementation(libs.boot.starter.flyway)
    implementation(libs.commons.jexl)
    runtimeOnly(project(":asset-repository-service-ui"))
    runtimeOnly(libs.h2.database)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

application {
    mainClass.set("org.poc.objs.assetrepository.AssetRepositoryApplication")
}

tasks.named<JavaExec>("run") {
    group = "application"
    description = "Runs AssetRepositoryApplication locally (H2, demo profile)."
    systemProperty("spring.profiles.active", "demo")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.get())
            dependencies {
                implementation(project())
                implementation(libs.boot.starter.test)
                implementation(libs.boot.starter.webmvc.test)
                runtimeOnly(libs.h2.database)
            }
        }
    }
}
