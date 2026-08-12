import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.jsonschema2pojo)
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

val catalogSchema = layout.projectDirectory.file("src/jsonschema/sbom-catalog-linked.schema.json")
val generatedPojoDir = layout.buildDirectory.dir("generated/sources/jsonschema2pojo")

/**
 * Refresh `src/jsonschema/sbom-catalog-linked.schema.json` from SbomRegistry.
 * Run after ontology / exporter changes:
 * `./gradlew :objs-sbom-example:exportSbomJsonSchema`
 *
 * Reads committed schema for POJO gen; this task only regenerates the schema file
 * (not on the generateJsonSchema2Pojo → compile critical path as an input producer).
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

/*
 * Java only (jsonschema2pojo does not emit Kotlin). Linked catalog includes relation props
 * (e.g. Database.containsDataset + Dataset.containsFromDatabase) intended to replace
 * hand-written WaveATypes / WaveBCDTypes once TypedEntity migration lands.
 */
jsonSchema2Pojo {
    setSource(files(catalogSchema))
    targetDirectory = generatedPojoDir.get().asFile
    targetPackage = "org.poc.objs.sbom.generated"
    setSourceType("jsonschema")
    setAnnotationStyle("jackson2")
    setUseTitleAsClassname(true)
    setIncludeAdditionalProperties(true)
    setGenerateBuilders(true)
    setIncludeConstructors(true)
    setRemoveOldOutput(true)
    setTargetVersion("21")
}

tasks.named("compileKotlin") {
    dependsOn("generateJsonSchema2Pojo")
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
