package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Scaffold health/status endpoint for the objs service.
 */
@RestController
@RequestMapping("/api/v1/objs")
@Tag(name = "status")
class ObjsStatusController {

    @GetMapping("/status")
    @Operation(summary = "Service status")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "OK",
            content = [Content(schema = Schema(implementation = ObjsStatus::class))],
        ),
    )
    fun status(): ObjsStatus = ObjsStatus(state = "ok", module = "objs-service")

    @Schema(description = "Smoke status payload")
    data class ObjsStatus(val state: String, val module: String)
}
