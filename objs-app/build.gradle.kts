plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description =
    "Foundation side service: objs-service + objs-service-ui (+ gremlin REST). " +
        "Must not be depended on by :sbom-service."

dependencies {
    implementation(platform(libs.boot.dependencies))
    // Side service libraries (workbench SPA via objs-service → objs-service-ui runtimeOnly)
    implementation(project(":objs-service"))
    implementation(project(":objs-gremlin-service"))
    implementation(libs.kotlin.reflect)
    runtimeOnly(libs.h2.database)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

application {
    mainClass.set("org.poc.objs.app.ObjsApplicationKt")
}

tasks.named<JavaExec>("run") {
    group = "application"
    description = "Runs foundation side service locally (H2, port 8081)."
}
