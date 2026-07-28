package org.poc.objs.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Assembly entry point: objs-service REST + objs-core persistence +
 * objs-sbom-example (`/api/v1/example/sbom`, registry + optional demo seed).
 */
@SpringBootApplication(scanBasePackages = ["org.poc.objs"])
class ObjsApplication

fun main(args: Array<String>) {
    runApplication<ObjsApplication>(*args)
}
