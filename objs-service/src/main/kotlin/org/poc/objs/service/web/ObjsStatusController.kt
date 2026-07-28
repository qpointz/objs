package org.poc.objs.service.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Scaffold health/status endpoint for the objs service.
 */
@RestController
@RequestMapping("/api/v1/objs")
class ObjsStatusController {

    @GetMapping("/status")
    fun status(): ObjsStatus = ObjsStatus(state = "ok", module = "objs-service")

    data class ObjsStatus(val state: String, val module: String)
}
