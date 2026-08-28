import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    java
    application
    id("org.jsonschema2pojo") version "1.3.3"
}

description = "Asset repository example — Java 21 Boot app (objs as object store)."

val codegenTool = configurations.create("codegenTool")

dependencies {
    implementation(platform(libs.boot.dependencies))
    implementation(project(":objs-api"))
    implementation(project(":objs-service"))
    implementation(project(":objs-gremlin-service"))
    implementation(libs.boot.starter.webmvc)
    implementation(libs.boot.starter.data.jpa)
    implementation(libs.boot.starter.flyway)
    implementation(libs.commons.jexl)
    runtimeOnly(project(":asset-repository-service-ui"))
    runtimeOnly(project(":objs-service-ui"))
    runtimeOnly(libs.h2.database)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)

    add("codegenTool", project(":objs-codegen-java"))
}

val assetRepositoryCodegenSchema =
    layout.projectDirectory.file("src/jsonschema/asset-repository-catalog.codegen.schema.json")
val assetRepositoryGeneratedPojoDir =
    layout.buildDirectory.dir("generated/sources/jsonschema2pojo")
val assetRepositoryGeneratedBindingsDir =
    layout.buildDirectory.dir("generated/sources/typed-bindings")

jsonSchema2Pojo {
    setSource(files(assetRepositoryCodegenSchema))
    targetDirectory = assetRepositoryGeneratedPojoDir.get().asFile
    targetPackage = "org.poc.objs.assetrepository.codegen.generated"
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
        java.srcDir(assetRepositoryGeneratedBindingsDir)
    }
}

tasks.register<JavaExec>("generateAssetRepositoryObjsJava") {
    group = "codegen"
    description = "Generate asset repository typed graph bindings from the checked-in codegen schema"
    classpath = codegenTool
    mainClass.set("org.poc.objs.codegen.java.JavaCodegenMain")
    args(
        assetRepositoryCodegenSchema.asFile.absolutePath,
        assetRepositoryGeneratedBindingsDir.get().asFile.absolutePath,
        "org.poc.objs.assetrepository.codegen.generated",
    )
    dependsOn("generateJsonSchema2Pojo")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateJsonSchema2Pojo", "generateAssetRepositoryObjsJava")
}

application {
    mainClass.set("org.poc.objs.assetrepository.AssetRepositoryApplication")
}

tasks.named<JavaExec>("run") {
    group = "application"
    description = "Runs AssetRepositoryApplication locally (H2, demo profile)."
    systemProperty("spring.profiles.active", "demo")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(libs.boot.starter.test)
                implementation(libs.boot.starter.webmvc.test)
                runtimeOnly(libs.h2.database)
            }
        }
    }
}
