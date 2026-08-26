import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
}

description = "Objs core: entity SDK, validation, and JPA persistence (Kotlin)."

dependencies {
    api(platform(libs.boot.dependencies))
    api(libs.boot.starter)
    api(libs.boot.starter.data.jpa)
    api(libs.boot.starter.flyway)
    api(libs.bundles.jackson)
    api(libs.kotlin.reflect)
    implementation(libs.json.schema.validator)
    implementation(libs.commons.jexl)
    implementation(libs.caffeine)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

testing {
    suites {
        configureEach {
            if (this is JvmTestSuite) {
                useJUnitJupiter()
                dependencies {
                    implementation(project())
                    implementation(libs.boot.starter.test)
                    implementation(libs.boot.starter.data.jpa.test)
                    runtimeOnly(libs.h2.database)
                }
            }
        }

        register<JvmTestSuite>("testIT") {
            dependencies {
                implementation(project())
                implementation(libs.boot.starter.test)
                implementation(libs.boot.starter.data.jpa.test)
                runtimeOnly(libs.postgresql)
                runtimeOnly(libs.flyway.postgresql)
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.testcontainers.postgresql)
            }
        }
    }
}
