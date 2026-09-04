import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    id("org.jsonschema2pojo") version "1.3.3"
}

description = "SBOM applications inventory — domain + Boot-launchable example under examples/sbom."

/**
 * Foundation workbench/REST may sit on the same JVM as a **demo sidecar**
 * (`runtimeOnly` only). Inventory Kotlin must not compile against those modules.
 */
val forbiddenFoundationCompileProjects =
    setOf(":objs-service", ":objs-service-ui", ":objs-gremlin-service", ":objs-service-app")

fun Configuration.forbidsFoundationCompile(): Boolean {
    val n = name
    return n == "api" || n == "compileOnly" || n == "implementation" || n.endsWith("Implementation")
}

val codegenTool by configurations.creating

dependencies {
    implementation(platform(libs.boot.dependencies))
    implementation(project(":objs-api"))
    implementation(project(":objs-autoconfigure"))
    implementation(project(":objs-gremlin-core"))
    implementation(libs.boot.starter.webmvc)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.kotlin.reflect)
    runtimeOnly(project(":sbom-service-ui"))
    runtimeOnly(project(":objs-service"))
    runtimeOnly(project(":objs-service-ui"))
    runtimeOnly(project(":objs-gremlin-service"))
    runtimeOnly(project(":objs-policy-service"))
    runtimeOnly(libs.h2.database)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)

    codegenTool(project(":objs-codegen-java"))
}

val sbomCodegenSchema =
    layout.projectDirectory.file("src/jsonschema/sbom-catalog.codegen.schema.json")
val sbomGeneratedPojoDir =
    layout.buildDirectory.dir("generated/sources/jsonschema2pojo")
val sbomGeneratedBindingsDir =
    layout.buildDirectory.dir("generated/sources/typed-bindings")

jsonSchema2Pojo {
    setSource(files(sbomCodegenSchema))
    targetDirectory = sbomGeneratedPojoDir.get().asFile
    targetPackage = "org.poc.objs.sbom.codegen.generated"
    setSourceType("jsonschema")
    setAnnotationStyle("jackson2")
    setUseTitleAsClassname(true)
    setIncludeAdditionalProperties(true)
    setGenerateBuilders(true)
    setIncludeConstructors(true)
    setRemoveOldOutput(true)
    setTargetVersion("21")
}

sourceSets {
    named("main") {
        java.srcDir(sbomGeneratedBindingsDir)
    }
}

tasks.register<JavaExec>("generateSbomObjsJava") {
    group = "codegen"
    description = "Generate SBOM typed graph bindings from the checked-in codegen schema"
    classpath = codegenTool
    mainClass.set("org.poc.objs.codegen.java.JavaCodegenMain")
    args(
        sbomCodegenSchema.asFile.absolutePath,
        sbomGeneratedBindingsDir.get().asFile.absolutePath,
        "org.poc.objs.sbom.codegen.generated",
    )
    dependsOn("generateJsonSchema2Pojo")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateJsonSchema2Pojo", "generateSbomObjsJava")
}

tasks.named("compileKotlin") {
    dependsOn("generateJsonSchema2Pojo", "generateSbomObjsJava")
}

configurations.configureEach {
    if (!forbidsFoundationCompile()) {
        return@configureEach
    }
    withDependencies {
        for (dep in this) {
            if (dep is ProjectDependency && dep.path in forbiddenFoundationCompileProjects) {
                throw GradleException(
                    ":sbom-service must not compile against ${dep.path} " +
                        "(use runtimeOnly for demo workbench sidecar)",
                )
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

application {
    mainClass.set("org.poc.objs.sbom.SbomApplicationKt")
}

tasks.named<JavaExec>("run") {
    group = "application"
    description = "Runs SBOM inventory app locally (H2 + demo seeds)."
    args("--spring.profiles.active=demo")
}

/**
 * Refresh the SBOM ontology seed from SbomRegistry.
 * `./gradlew :sbom-service:exportSbomOntology`
 */
tasks.register<JavaExec>("exportSbomOntology") {
    group = "codegen"
    description = "Regenerate src/main/resources/seeds/sbom-ontology.yaml from SbomRegistry"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.poc.objs.sbom.codegen.ExportSbomOntology")
    args(layout.projectDirectory.file("src/main/resources/seeds/sbom-ontology.yaml").asFile.absolutePath)
    dependsOn("compileKotlin", "processResources")
}

testing {
    suites {
        configureEach {
            if (this is JvmTestSuite) {
                useJUnitJupiter()
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
