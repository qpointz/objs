plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
}

description = "Runnable Objs assembly (REST + core + SBOM example)."

dependencyManagement {
    imports {
        mavenBom(libs.boot.dependencies.get().toString())
    }
}

dependencies {
    implementation(project(":objs-service"))
    implementation(project(":objs-sbom-example"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    runtimeOnly(libs.h2.database)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)
}

kotlin {
    jvmToolchain(24)
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
