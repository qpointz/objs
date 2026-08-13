import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description = "Objs Spring REST service and Boot autoconfiguration (Kotlin)."

dependencies {
    api(platform(libs.boot.dependencies))
    api(project(":objs-core"))
    api(libs.boot.starter.webmvc)
    api(libs.kotlin.reflect)
    // api: Swagger/OpenAPI annotations are used by this module and downstream controllers
    api(libs.springdoc.openapi.starter.webmvc.ui)
    // Workbench SPA on classpath at static/ui/ (served at /workbench/)
    runtimeOnly(project(":objs-service-ui"))
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
                    implementation(libs.boot.starter.webmvc.test)
                }
            }
        }
    }
}
