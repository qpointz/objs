plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description =
    "Workbench-only runnable: objs-service + objs-service-ui + gremlin REST. " +
        "Must not depend on examples or other concrete product modules."

dependencies {
    implementation(platform(libs.boot.dependencies))
    implementation(project(":objs-service"))
    implementation(project(":objs-gremlin-service"))
    implementation(libs.kotlin.reflect)
    runtimeOnly(project(":objs-service-ui"))
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
    description = "Runs the workbench (foundation REST + SPA, H2, port 8081)."
}
