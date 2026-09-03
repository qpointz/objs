import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
}

description = "Objs Boot autoconfiguration: wires DataSource/EMF into Spring-free objs-core."

dependencies {
    api(project(":objs-core"))
    api(platform(libs.boot.dependencies))
    api(libs.boot.starter)
    api(libs.boot.starter.data.jpa)
    api(libs.boot.starter.flyway)
    api(libs.kotlin.reflect)
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
    }
}
