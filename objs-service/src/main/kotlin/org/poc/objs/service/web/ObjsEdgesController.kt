package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.api.domain.GraphException
import org.poc.objs.core.persistence.GraphStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Edge history under `/api/v1/objs/edges` (G-UX-over). */
@RestController
@RequestMapping("/api/v1/objs/edges")
@Tag(
    name = "edges",
    description = "Pool edge history and graph-scoped edge CRUD",
)
class ObjsEdgesController(
    private val store: GraphStore,
) {
    @GetMapping("/{id}/versions/stats")
    @Operation(
        summary = "Edge version stats: total count + newest recent N",
        description = "Cheap history probe for UIs: full version count plus the newest [recent] summaries.",
    )
    @ApiResponse(responseCode = "200", description = "Version count and newest summaries")
    fun versionStats(
        @Parameter(description = "Edge id") @PathVariable id: UUID,
        @Parameter(description = "How many of the newest version summaries to include")
        @RequestParam(defaultValue = "5") recent: Int,
    ) = store.edgeVersionStats(id, recent)

    @GetMapping("/{id}/versions")
    @Operation(
        summary = "List edge deep-capture versions, newest first",
        description = "Edges are graph-local; versions come from the deep freezes of the owning graph.",
    )
    @ApiResponse(responseCode = "200", description = "Edge version summaries")
    fun listVersions(
        @Parameter(description = "Edge id") @PathVariable id: UUID,
    ) = store.listEdgeVersions(id)

    @PostMapping("/{id}/compact")
    @Operation(
        summary = "Compact orphan edge version rows",
        description = "Deletes edge version rows not pinned by any graph freeze and not equal to live head_version.",
    )
    @ApiResponse(responseCode = "200", description = "`removed`: number of version rows deleted")
    fun compact(
        @Parameter(description = "Edge id") @PathVariable id: UUID,
    ): Map<String, Int> =
        mapOf("removed" to store.compactEdge(id))

    @GetMapping("/{id}/versions/{version}")
    @Operation(summary = "Fetch one edge deep-capture version")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Frozen edge at that version"),
        ApiResponse(responseCode = "404", description = "Edge or version not found (`error` + `code`)"),
    )
    fun getVersion(
        @Parameter(description = "Edge id") @PathVariable id: UUID,
        @Parameter(description = "Edge version number") @PathVariable version: Long,
    ): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(store.getEdgeVersion(id, version))
        } catch (ex: GraphException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to (ex.message ?: ex.code), "code" to ex.code),
            )
        }
}
