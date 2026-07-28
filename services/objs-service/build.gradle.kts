import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.spring.dependency.management)
}

description = "Objs Spring REST service and autoconfiguration."

dependencyManagement {
    imports {
        mavenBom(libs.boot.dependencies.get().toString())
    }
}

dependencies {
    api(project(":core:objs-core"))
    api(libs.boot.starter.webmvc)
    implementation(libs.boot.starter)
    implementation(libs.bundles.logging)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.boot.configuration.processor)
}

testing {
    suites {
        register<JvmTestSuite>("testIT") {
        }

        configureEach {
            if (this is JvmTestSuite) {
                useJUnitJupiter(libs.versions.junit.get())
                dependencies {
                    implementation(project())
                    implementation(libs.boot.starter.test)
                    implementation(libs.boot.starter.webmvc.test)
                    implementation(libs.assertj.core)
                    implementation(libs.mockito.core)
                    implementation(libs.mockito.junit.jupiter)
                    compileOnly(libs.lombok)
                    annotationProcessor(libs.lombok)
                }
            }
        }
    }
}
