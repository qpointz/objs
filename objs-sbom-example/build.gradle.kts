import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.Exec
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
}

description = "Concrete Software BOM example app on the objs graph foundation."

dependencyManagement {
    imports {
        mavenBom(libs.boot.dependencies.get().toString())
    }
}

dependencies {
    api(project(":objs-core"))
    api(project(":objs-service"))
    api(libs.kotlin.stdlib)
    api(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
}

kotlin {
    jvmToolchain(24)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
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
                    implementation(libs.assertj.core)
                    implementation(libs.mockito.core)
                    implementation(libs.mockito.junit.jupiter)
                    runtimeOnly(libs.h2.database)
                }
            }
        }
    }
}

// --- Graph explorer SPA (objs-sbom-example/ui) ---
// Exec/Sync UI tasks declare incompatibility; still avoid capturing Project script objects.
val uiDir = layout.projectDirectory.dir("ui")
val uiPackageJson = uiDir.file("package.json")
val uiLockFile = uiDir.file("package-lock.json")
val uiDistDir = uiDir.dir("dist")
val npmCmd =
    providers.systemProperty("os.name").map { os ->
        if (os.lowercase().contains("windows")) "npm.cmd" else "npm"
    }.orElse("npm")
val skipUi = providers.gradleProperty("skipUi").map { it.toBoolean() }.orElse(false)

val npmInstallUi by tasks.registering(Exec::class) {
    group = "ui"
    description = "npm install for graph explorer SPA"
    notCompatibleWithConfigurationCache("npm UI install uses local Node toolchain")
    workingDir = uiDir.asFile
    inputs.file(uiPackageJson)
    if (uiLockFile.asFile.exists()) {
        inputs.file(uiLockFile)
        commandLine(npmCmd.get(), "ci")
    } else {
        commandLine(npmCmd.get(), "install")
    }
    outputs.dir(uiDir.dir("node_modules"))
    onlyIf {
        !skipUi.get() && uiPackageJson.asFile.exists()
    }
}

val npmBuildUi by tasks.registering(Exec::class) {
    group = "ui"
    description = "vite build graph explorer SPA"
    notCompatibleWithConfigurationCache("npm UI build uses local Node toolchain")
    dependsOn(npmInstallUi)
    workingDir = uiDir.asFile
    inputs.dir(uiDir.dir("src"))
    inputs.file(uiPackageJson)
    inputs.file(uiDir.file("vite.config.ts"))
    inputs.file(uiDir.file("index.html"))
    inputs.file(uiDir.file("tsconfig.json"))
    outputs.dir(uiDistDir)
    commandLine(npmCmd.get(), "run", "build")
    onlyIf {
        !skipUi.get() && uiPackageJson.asFile.exists()
    }
}

val syncUiStatic by tasks.registering(Sync::class) {
    group = "ui"
    description = "Copy SPA dist into classpath static/ui"
    notCompatibleWithConfigurationCache("SPA static sync tied to npm UI build")
    dependsOn(npmBuildUi)
    from(uiDistDir)
    into(layout.buildDirectory.dir("resources/main/static/ui"))
    onlyIf {
        !skipUi.get() && uiPackageJson.asFile.exists()
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(syncUiStatic)
}

tasks.named("jar") {
    dependsOn(syncUiStatic)
}
