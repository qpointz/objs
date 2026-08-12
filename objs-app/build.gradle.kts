plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description = "Runnable Objs assembly (REST + core + SBOM example)."

dependencies {
    implementation(platform(libs.boot.dependencies))
    implementation(project(":objs-service"))
    implementation(project(":objs-sbom-example"))
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
    description = "Runs ObjsApplication locally (H2)."
}
