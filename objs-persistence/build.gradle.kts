import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
}

description = "Objs persistence: Spring-free JPA persistence, catalogs, seed apply, validation (Kotlin)."

dependencies {
    api(project(":objs-api"))
    api(libs.bundles.jackson)
    api(libs.kotlin.reflect)
    api(libs.jakarta.persistence.api)
    implementation(libs.hibernate.core)
    implementation(libs.flyway.core)
    implementation(libs.json.schema.validator)
    implementation(libs.caffeine)
    implementation(libs.slf4j.api)
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
                    implementation(libs.junit.jupiter.api)
                    implementation(libs.assertj.core)
                    runtimeOnly(libs.junit.jupiter.engine)
                    runtimeOnly(libs.h2.database)
                    implementation("org.mockito:mockito-core:5.20.0")
                }
            }
        }

        register<JvmTestSuite>("testIT") {
            dependencies {
                implementation(project())
                implementation(libs.junit.jupiter.api)
                implementation(libs.assertj.core)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.postgresql)
                runtimeOnly(libs.flyway.postgresql)
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.testcontainers.postgresql)
            }
        }
    }
}

sourceSets.named("testIT") {
    val test = sourceSets.getByName("test")
    compileClasspath += test.output
    runtimeClasspath += test.output + test.runtimeClasspath
}

tasks.named("compileTestITKotlin") {
    dependsOn("compileTestKotlin")
}
