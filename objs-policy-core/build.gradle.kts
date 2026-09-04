import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

description = "Objs policy core: in-memory repository and flat evaluation orchestrator."

dependencies {
    api(project(":objs-policy-api"))
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
                    implementation(libs.assertj.core)
                    implementation(libs.junit.jupiter.api)
                    runtimeOnly(libs.junit.jupiter.engine)
                }
            }
        }
    }
}
