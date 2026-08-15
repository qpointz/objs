package org.poc.objs.sbom

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Launchable SBOM inventory application (`examples/sbom/sbom-service`).
 * Depends on objs-core (+ gremlin-core). Foundation workbench/REST may be on the
 * runtime classpath as a demo sidecar (`runtimeOnly`); inventory does not compile against them.
 */
@SpringBootApplication(scanBasePackages = ["org.poc.objs"])
class SbomApplication

fun main(args: Array<String>) {
    runApplication<SbomApplication>(*args)
}
