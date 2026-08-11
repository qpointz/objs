import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.dependency.management)
}

description = "Objs Gremlin core: BoM subgraph materialization and gremlin-lang evaluation."

dependencyManagement {
    imports {
        mavenBom(libs.boot.dependencies.get().toString())
    }
}

dependencies {
    api(project(":objs-core"))
    api(libs.gremlin.core)
    api(libs.tinkergraph.gremlin)
    api(libs.gremlin.language)
    // TinkerGQL: match("MATCH …") declarative patterns — https://tinkerpop.apache.org/docs/4.0.0-beta.3/reference/#tinkergql
    api(libs.gql.gremlin)
    api(libs.kotlin.stdlib)
    api(libs.kotlin.reflect)
    implementation(libs.bundles.logging)
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
                    implementation(libs.assertj.core)
                    implementation(libs.junit.jupiter.api)
                    runtimeOnly(libs.junit.jupiter.engine)
                }
            }
        }
    }
}
