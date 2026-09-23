package org.poc.objs.sbom

import org.poc.objs.policy.service.ObjsPolicyServiceAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Launchable SBOM inventory application (`examples/sbom/sbom-service`).
 * Compiles against objs + policy service (assessment seeds/handlers). Foundation workbench/REST
 * may still sit on the runtime classpath as a demo sidecar (`runtimeOnly`).
 */
@SpringBootApplication(scanBasePackages = ["org.poc.objs"])
@ImportAutoConfiguration(ObjsPolicyServiceAutoConfiguration::class)
class SbomApplication

fun main(args: Array<String>) {
    runApplication<SbomApplication>(*args)
}
