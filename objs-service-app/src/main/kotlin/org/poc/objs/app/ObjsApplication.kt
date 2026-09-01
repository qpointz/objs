package org.poc.objs.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Workbench-only runnable: objs-service REST + workbench SPA + gremlin traverse + graph algorithms.
 * Concrete products live under `examples/` and must not be on this classpath.
 */
@SpringBootApplication(scanBasePackages = ["org.poc.objs"])
class ObjsApplication

fun main(args: Array<String>) {
    runApplication<ObjsApplication>(*args)
}
