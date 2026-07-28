plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
}

description = "Runnable Objs assembly (REST + core persistence)."

dependencyManagement {
    imports {
        mavenBom(libs.boot.dependencies.get().toString())
    }
}

dependencies {
    implementation(project(":objs-service"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    runtimeOnly(libs.h2.database)
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
