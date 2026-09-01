import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

description = "Objs Gremlin core: BoM subgraph materialization and gremlin-lang evaluation."

dependencies {
    api(project(":objs-api"))
    api(project(":objs-core"))
    api(libs.gremlin.core)
    api(libs.tinkergraph.gremlin)
    api(libs.gremlin.language)
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
                    implementation(libs.assertj.core)
                    implementation(libs.junit.jupiter.api)
                    runtimeOnly(libs.junit.jupiter.engine)
                }
            }
        }
    }
}
