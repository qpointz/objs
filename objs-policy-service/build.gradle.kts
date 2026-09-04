import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description = "Objs policy REST service and Boot autoconfiguration (workbench playground)."

dependencies {
    api(project(":objs-policy-core"))
    api(project(":objs-policy-drools"))
    api(project(":objs-service"))
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
                    implementation(libs.boot.starter.webmvc.test)
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isEnabled = false
    }
}
