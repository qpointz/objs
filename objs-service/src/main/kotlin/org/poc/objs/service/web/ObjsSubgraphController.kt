package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.BoMResolvedSubgraph
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.domain.BoMSubgraphException
import org.poc.objs.core.domain.BoMSubgraphListItem
import org.poc.objs.core.domain.BoMSubgraphSpec
import org.poc.objs.core.persistence.BoMSubgraphStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Soft-link subgraph REST under `/api/v1/objs/graph/subgraphs`.
 */
@RestController
@RequestMapping("/api/v1/objs/graph/subgraphs")
@Tag(name = "subgraphs")
class ObjsSubgraphController(
    private val subgraphStore: BoMSubgraphStore,
) {
    data class SubgraphWriteBody(
        val id: UUID? = null,
        val annotations: Map<String, String> = emptyMap(),
        val entityIds: Set<UUID> = emptySet(),
        val edgeIds: Set<UUID> = emptySet(),
    )

    data class SubgraphResponse(
        val id: UUID,
        val annotations: Map<String, String>,
        val subgraph: BoMSubgraph,
    )

    @GetMapping
    @Operation(summary = "List soft-link subgraphs")
    fun list(): List<BoMSubgraphListItem> = subgraphStore.list()

    @PostMapping
    @Operation(summary = "Create a soft-link subgraph")
    fun create(@RequestBody body: SubgraphWriteBody): ResponseEntity<SubgraphResponse> {
        val created = subgraphStore.create(body.toSpec())
        return ResponseEntity.status(HttpStatus.CREATED).body(created.toResponse())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subgraph by id (header + resolved members)")
    fun get(@PathVariable id: UUID): ResponseEntity<SubgraphResponse> {
        val resolved = subgraphStore.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(resolved.toResponse())
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace subgraph annotations and membership")
    fun replace(
        @PathVariable id: UUID,
        @RequestBody body: SubgraphWriteBody,
    ): SubgraphResponse = subgraphStore.replace(id, body.toSpec()).toResponse()

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subgraph header and membership (graph objects remain)")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        subgraphStore.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/snapshot")
    @Operation(summary = "Hard snapshot: clone members and create a new evidence-pack subgraph")
    fun snapshot(
        @PathVariable id: UUID,
        @RequestBody body: SnapshotBody?,
    ): ResponseEntity<Any> {
        if (body?.annotations == null) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "'annotations' is required", "code" to "SUBGRAPH_SNAPSHOT_ANNOTATIONS"),
            )
        }
        val created = subgraphStore.snapshot(id, body.annotations)
        return ResponseEntity.status(HttpStatus.CREATED).body(created.toResponse())
    }

    data class SnapshotBody(
        val annotations: Map<String, String>? = null,
    )

    @ExceptionHandler(BoMSubgraphException::class)
    fun handleSubgraphException(ex: BoMSubgraphException): ResponseEntity<Map<String, String>> {
        val status = when (ex.code) {
            "SUBGRAPH_NOT_FOUND" -> HttpStatus.NOT_FOUND
            else -> HttpStatus.BAD_REQUEST
        }
        return ResponseEntity.status(status).body(
            mapOf("error" to (ex.message ?: ex.code), "code" to ex.code),
        )
    }

    private fun SubgraphWriteBody.toSpec() = BoMSubgraphSpec(
        id = id,
        annotations = annotations,
        entityIds = entityIds,
        edgeIds = edgeIds,
    )

    private fun BoMResolvedSubgraph.toResponse() = SubgraphResponse(
        id = id,
        annotations = annotations,
        subgraph = subgraph,
    )
}
