package org.poc.objs.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Assembly entry point: boots objs-service REST + objs-core persistence.
 */
@SpringBootApplication(scanBasePackages = ["org.poc.objs"])
class ObjsApplication

fun main(args: Array<String>) {
    runApplication<ObjsApplication>(*args)
}
