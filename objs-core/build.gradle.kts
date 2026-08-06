import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.dependency.management)
}

description = "Objs core: entity SDK, validation, and JPA persistence (Kotlin)."

dependencyManagement {
    imports {
        mavenBom(libs.boot.dependencies.get().toString())
    }
}

dependencies {
    api(libs.boot.starter)
    api(libs.boot.starter.data.jpa)
    api(libs.boot.starter.flyway)
    api(libs.bundles.jackson)
    api(libs.kotlin.stdlib)
    api(libs.kotlin.reflect)
    implementation(libs.bundles.logging)
    implementation(libs.json.schema.validator)
    implementation(libs.commons.jexl)
    implementation(libs.flyway.core)
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
                useJUnitJupiter(libs.versions.junit.get())
                dependencies {
                    implementation(project())
                    implementation(libs.boot.starter.test)
                    implementation(libs.boot.starter.data.jpa.test)
                    implementation(libs.assertj.core)
                    implementation(libs.mockito.core)
                    implementation(libs.mockito.junit.jupiter)
                    runtimeOnly(libs.h2.database)
                }
            }
        }

        register<JvmTestSuite>("testIT") {
            dependencies {
                implementation(project())
                implementation(libs.boot.starter.test)
                implementation(libs.boot.starter.data.jpa.test)
                implementation(libs.assertj.core)
                runtimeOnly(libs.postgresql)
                runtimeOnly(libs.flyway.postgresql)
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.testcontainers.postgresql)
            }
        }
    }
}
