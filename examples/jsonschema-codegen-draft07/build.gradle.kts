import java.net.URI

plugins {
    `java-library`
    id("org.jsonschema2pojo") version "1.3.3"
}

description =
    "Standalone example: objs json-schema-codegen (dialect=draft-07) → Java POJOs via jsonschema2pojo."

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.19.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val schemaUrl: String =
    (findProperty("objs.schemaUrl") as String?)
        ?: "http://localhost:8080/api/v1/objs/registry/export?format=json-schema-codegen&dialect=draft-07&includeEdgePropertySchemas=true"

val catalogSchema =
    layout.projectDirectory.file("src/jsonschema/registry-catalog.codegen.draft07.schema.json")
val generatedPojoDir = layout.buildDirectory.dir("generated/sources/jsonschema2pojo")

/**
 * Refresh the committed draft-07 codegen schema from a running objs instance.
 *
 *   ./gradlew -p examples/jsonschema-codegen-draft07 fetchRegistrySchema
 */
tasks.register("fetchRegistrySchema") {
    group = "codegen"
    description = "Download json-schema-codegen (draft-07) from a running objs registry"
    outputs.file(catalogSchema)
    doLast {
        val url = URI(schemaUrl).toURL()
        logger.lifecycle("Fetching schema from $url")
        val bytes = url.openStream().use { it.readBytes() }
        catalogSchema.asFile.parentFile.mkdirs()
        catalogSchema.asFile.writeBytes(bytes)
        logger.lifecycle("Wrote ${bytes.size} bytes → ${catalogSchema.asFile}")
    }
}

/*
 * Drop workbench Export → JSON Schema (codegen) with dialect=draft-07 here, or fetchRegistrySchema.
 * Uses definitions / #/definitions/… (no wrap).
 */
jsonSchema2Pojo {
    setSource(files(catalogSchema))
    targetDirectory = generatedPojoDir.get().asFile
    targetPackage = "org.poc.objs.codegen.draft07.generated"
    setSourceType("jsonschema")
    setAnnotationStyle("jackson2")
    setUseTitleAsClassname(true)
    setIncludeAdditionalProperties(true)
    setGenerateBuilders(true)
    setIncludeConstructors(true)
    setRemoveOldOutput(true)
    setTargetVersion("21")
}

tasks.named("compileJava") {
    dependsOn("generateJsonSchema2Pojo")
}
