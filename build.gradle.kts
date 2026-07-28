import java.nio.file.Files

plugins {
    base
}

val projectVersion: String =
    (findProperty("projectVersion") as String?)
        ?: layout.projectDirectory.file("VERSION").asFile
            .takeIf { it.exists() }
            ?.let { Files.readAllLines(it.toPath()).firstOrNull()?.trim() }
        ?: "0.1.0"

allprojects {
    group = "io.qpointz.poc.objs"
    version = projectVersion
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }

        tasks.withType<Test>().configureEach {
            workingDir = project.projectDir
            useJUnitPlatform()
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
        ":core:objs-core:test",
        ":services:objs-service:test"
    )
}

tasks.register("testIT") {
    description = "Runs all testIT tasks across leaf modules"
    group = "verification"
    dependsOn(
        ":services:objs-service:testIT"
    )
}
