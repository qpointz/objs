import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
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

dependencies {
    implementation(platform(libs.boot.dependencies))
    implementation(project(":objs-core"))
    implementation(project(":objs-gremlin-core"))
    implementation(libs.boot.starter.webmvc)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.kotlin.reflect)
    runtimeOnly(project(":sbom-service-ui"))
    runtimeOnly(project(":objs-service"))
    runtimeOnly(project(":objs-service-ui"))
    runtimeOnly(project(":objs-gremlin-service"))
    runtimeOnly(libs.h2.database)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)
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
 * Refresh `src/jsonschema/sbom-catalog-linked.schema.json` from SbomRegistry.
 * `./gradlew :sbom-service:exportSbomJsonSchema`
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
