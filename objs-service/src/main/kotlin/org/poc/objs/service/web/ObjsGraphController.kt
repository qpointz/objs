package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Graph HTTP API under `/api/v1/objs/graph` (no load-all; filtered GET only).
 */
@RestController
@RequestMapping("/api/v1/objs")
@Tag(name = "graph")
class ObjsGraphController(
    private val store: BoMGraphStore,
) {
    @PutMapping("/graph")
    @Operation(summary = "Upsert a graph batch (entities + edges)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Persisted graph with ids assigned"),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
    )
    fun putGraph(@RequestBody graph: BoMGraph): ResponseEntity<Any> {
        val result = store.write(graph)
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.ok(graph)
    }

    @PostMapping("/graph/validate")
    @Operation(summary = "Dry-run validate a graph batch (no persist)")
    @ApiResponse(responseCode = "200", description = "Validation result (may be invalid)")
    fun validateGraph(@RequestBody graph: BoMGraph): BoMValidationResult = store.validate(graph)

    @GetMapping("/graph")
    @Operation(summary = "Select induced subgraph by annotation filter (match-all)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Subgraph"),
        ApiResponse(
            responseCode = "400",
            description = "Empty filter rejected",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
    )
    fun getGraph(@RequestParam params: Map<String, String>): ResponseEntity<Any> {
        if (params.isEmpty()) {
            return ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "FILTER_EMPTY",
                        message = "Annotation filter required; refusing to load entire graph",
                    ),
                ),
            )
        }
        val subgraph: BoMSubgraph = store.selectSubgraphMatchAll(params)
        return ResponseEntity.ok(subgraph)
    }

    @DeleteMapping("/graph")
    @Operation(summary = "Batch delete entities and/or edges")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Empty delete or other validation error"),
        ApiResponse(responseCode = "404", description = "Unknown entity or edge id"),
    )
    fun deleteGraph(@RequestBody body: GraphDeleteRequest): ResponseEntity<Any> {
        val result = store.delete(
            entityIds = body.entityIds.orEmpty(),
            edgeIds = body.edgeIds.orEmpty(),
        )
        if (!result.isValid) {
            val notFound = result.issues.any {
                it.code == "ENTITY_NOT_FOUND" || it.code == "EDGE_NOT_FOUND"
            }
            val status = if (notFound) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            return ResponseEntity.status(status).body(result)
        }
        return ResponseEntity.noContent().build()
    }

    @Schema(description = "Batch delete request")
    data class GraphDeleteRequest(
        val entityIds: List<UUID>? = null,
        val edgeIds: List<UUID>? = null,
    )
}
