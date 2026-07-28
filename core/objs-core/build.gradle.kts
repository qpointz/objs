import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.spring.dependency.management)
}

description = "Objs core: Spring primitives, domain types, and JPA persistence."

dependencyManagement {
    imports {
        mavenBom(libs.boot.dependencies.get().toString())
    }
}

dependencies {
    api(libs.boot.starter)
    api(libs.boot.starter.data.jpa)
    api(libs.bundles.jackson)
    implementation(libs.bundles.logging)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
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
                    compileOnly(libs.lombok)
                    annotationProcessor(libs.lombok)
                }
            }
        }
    }
}
