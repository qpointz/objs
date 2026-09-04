import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

description = "Objs policy Drools: PolicyEngine adapter (fixture DRL; opt-in classpath)."

dependencies {
    api(project(":objs-policy-api"))
    implementation(platform(libs.drools.bom))
    implementation(libs.drools.engine)
    // Required for KieModuleModel / writeKModuleXML (programmatic DRL compile).
    implementation(libs.drools.xml.support)
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
                    implementation(project(":objs-policy-core"))
                    implementation(libs.assertj.core)
                    implementation(libs.junit.jupiter.api)
                    runtimeOnly(libs.junit.jupiter.engine)
                }
            }
        }
    }
}

// Drools DRL lexer methods exceed JaCoCo's instrumenter limit (MethodTooLargeException).
tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isEnabled = false
    }
}
