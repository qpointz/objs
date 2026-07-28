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
// Node/npm Exec + Copy are marked incompatible with configuration cache.
val uiDirFile = file("ui")
val uiPackageJsonFile = file("ui/package.json")
val uiLockFile = file("ui/package-lock.json")
val uiDistFile = file("ui/dist")
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val npmCmd = if (isWindows) "npm.cmd" else "npm"
fun skipUiBuild(): Boolean = findProperty("skipUi")?.toString()?.toBoolean() == true

val npmInstallUi by tasks.registering(Exec::class) {
    group = "ui"
    description = "npm install for graph explorer SPA"
    notCompatibleWithConfigurationCache("npm UI install uses local Node toolchain")
    workingDir = uiDirFile
    inputs.file(uiPackageJsonFile)
    if (uiLockFile.exists()) {
        inputs.file(uiLockFile)
        commandLine(npmCmd, "ci")
    } else {
        commandLine(npmCmd, "install")
    }
    outputs.dir(file("ui/node_modules"))
    onlyIf { !skipUiBuild() && uiPackageJsonFile.exists() }
}

val npmBuildUi by tasks.registering(Exec::class) {
    group = "ui"
    description = "vite build graph explorer SPA"
    notCompatibleWithConfigurationCache("npm UI build uses local Node toolchain")
    dependsOn(npmInstallUi)
    workingDir = uiDirFile
    inputs.dir(file("ui/src"))
    inputs.file(uiPackageJsonFile)
    inputs.file(file("ui/vite.config.ts"))
    inputs.file(file("ui/index.html"))
    inputs.file(file("ui/tsconfig.json"))
    outputs.dir(uiDistFile)
    commandLine(npmCmd, "run", "build")
    onlyIf { !skipUiBuild() && uiPackageJsonFile.exists() }
}

val syncUiStatic by tasks.registering(Sync::class) {
    group = "ui"
    description = "Copy SPA dist into classpath static/ui"
    notCompatibleWithConfigurationCache("SPA static sync tied to npm UI build")
    dependsOn(npmBuildUi)
    from(uiDistFile)
    into(layout.buildDirectory.dir("resources/main/static/ui"))
    onlyIf { !skipUiBuild() && uiPackageJsonFile.exists() }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(syncUiStatic)
}

tasks.named("jar") {
    dependsOn(syncUiStatic)
}
