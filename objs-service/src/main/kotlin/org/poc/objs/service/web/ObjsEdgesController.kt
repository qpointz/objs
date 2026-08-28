package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.GraphException
import org.poc.objs.core.persistence.GraphStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Edge history under `/api/v1/objs/edges` (G-UX-over). */
@RestController
@RequestMapping("/api/v1/objs/edges")
@Tag(name = "edges")
class ObjsEdgesController(
    private val store: GraphStore,
) {
    @GetMapping("/{id}/versions/stats")
    @Operation(summary = "Edge version stats: total count + newest recent N")
    fun versionStats(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "5") recent: Int,
    ) = store.edgeVersionStats(id, recent)

    @GetMapping("/{id}/versions")
    @Operation(summary = "List edge deep-capture versions, newest first")
    fun listVersions(@PathVariable id: UUID) = store.listEdgeVersions(id)

    @GetMapping("/{id}/versions/{version}")
    @Operation(summary = "Fetch one edge deep-capture version")
    fun getVersion(
        @PathVariable id: UUID,
        @PathVariable version: Long,
    ): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(store.getEdgeVersion(id, version))
        } catch (ex: GraphException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to (ex.message ?: ex.code), "code" to ex.code),
            )
        }
}
