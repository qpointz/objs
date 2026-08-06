import java.nio.file.Files

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
}

val projectVersion: String =
    (findProperty("projectVersion") as String?)
        ?: layout.projectDirectory.file("VERSION").asFile
            .takeIf { it.exists() }
            ?.let { Files.readAllLines(it.toPath()).firstOrNull()?.trim() }
        ?: "0.1.0"

allprojects {
    group = "org.poc.objs"
    version = projectVersion
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<Test>().configureEach {
            workingDir = project.projectDir
            useJUnitPlatform()
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<Test>().configureEach {
            workingDir = project.projectDir
            useJUnitPlatform()
        }
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
    }

    plugins.withId("java-library") {
        pluginManager.apply("jacoco")
        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.required.set(true)
            }
        }
    }
}

tasks.register("test") {
    description = "Runs all test tasks across leaf modules"
    group = "verification"
    dependsOn(
        ":objs-core:test",
        ":objs-service:test",
        ":objs-sbom-example:test",
        ":objs-gremlin-core:test",
        ":objs-gremlin-service:test",
    )
}

tasks.register("testIT") {
    description = "Runs all testIT tasks across leaf modules"
    group = "verification"
    dependsOn(
        ":objs-core:testIT",
        ":objs-service:testIT",
    )
}
