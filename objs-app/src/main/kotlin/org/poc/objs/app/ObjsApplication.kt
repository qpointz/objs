package org.poc.objs.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Foundation assembly: objs-service REST + workbench + gremlin traverse.
 * SBOM inventory lives under examples/sbom (:sbom-service).
 */
@SpringBootApplication(scanBasePackages = ["org.poc.objs"])
class ObjsApplication

fun main(args: Array<String>) {
    runApplication<ObjsApplication>(*args)
}
