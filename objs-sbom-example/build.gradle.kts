import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description = "Concrete Software BOM example app on the objs graph foundation."

dependencies {
    api(platform(libs.boot.dependencies))
    api(project(":objs-core"))
    api(project(":objs-service"))
    api(libs.kotlin.reflect)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

/**
 * Refresh `src/jsonschema/sbom-catalog-linked.schema.json` from SbomRegistry.
 * Run after ontology / exporter changes:
 * `./gradlew :objs-sbom-example:exportSbomJsonSchema`
 */
tasks.register<JavaExec>("exportSbomJsonSchema") {
    group = "codegen"
    description =
        "Regenerate src/jsonschema/sbom-catalog-linked.schema.json (and types/) from SbomRegistry"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.poc.objs.sbom.codegen.ExportSbomJsonSchema")
    args(layout.projectDirectory.dir("src/jsonschema").asFile.absolutePath)
    dependsOn("compileKotlin", "processResources")
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
                    implementation(libs.boot.starter.webmvc.test)
                    runtimeOnly(libs.h2.database)
                }
            }
        }
    }
}
