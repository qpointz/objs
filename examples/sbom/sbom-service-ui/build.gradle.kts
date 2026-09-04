import com.github.gradle.node.npm.task.NpmTask
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    // `java` (not `java-library`) so root jacoco is not applied — node-gradle
    // injects project repos that would otherwise shadow settings and break Jacoco resolution.
    java
    alias(libs.plugins.node.gradle)
}

description = "SBOM inventory SPA (Vite/React) packaged as classpath static/sbom."

val viteOutDir = layout.buildDirectory.dir("generated/vite")

// GitLab CI sets CI=true / GITLAB_CI=true → npm ci (clean, lockfile-strict).
// Local: npm install (incremental). Force CI path with -PnpmInstallCi=true.
val npmInstallCmd =
    when {
        findProperty("npmInstallCi")?.toString()?.toBoolean() == true -> "ci"
        System.getenv("GITLAB_CI") == "true" || System.getenv("CI") == "true" -> "ci"
        else -> "install"
    }

node {
    download.set(true)
    version.set("22.14.0")
    npmInstallCommand.set(npmInstallCmd)
}

fun Project.uiSkipped(): Boolean =
    findProperty("skipUi")?.toString()?.toBoolean() == true

tasks.npmInstall {
    group = "ui"
    description = "npm $npmInstallCmd for SBOM inventory SPA"
    notCompatibleWithConfigurationCache("node-gradle npmInstall")
    onlyIf("skipUi is not true") { !uiSkipped() }
}

val npmBuildSbomUi by tasks.registering(NpmTask::class) {
    group = "ui"
    description = "vite build SBOM SPA into build/generated/vite"
    notCompatibleWithConfigurationCache("node-gradle npmBuildSbomUi")
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "build"))
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("package.json"))
    inputs.file(layout.projectDirectory.file("package-lock.json"))
    inputs.file(layout.projectDirectory.file("vite.config.ts"))
    inputs.file(layout.projectDirectory.file("index.html"))
    inputs.file(layout.projectDirectory.file("tsconfig.json"))
    outputs.dir(viteOutDir)
    onlyIf("skipUi is not true") { !uiSkipped() }
}

// Project dependencies consume resources/ + classes/ dirs, not the jar artifact.
// Stub: src/main/resources/static/sbom/index.html (used when -PskipUi=true).
tasks.named<ProcessResources>("processResources") {
    dependsOn(npmBuildSbomUi)
    if (!uiSkipped()) {
        exclude("static/sbom/**")
        from(viteOutDir) {
            into("static/sbom")
        }
    }
}

tasks.named<Test>("test") {
    enabled = false
}
