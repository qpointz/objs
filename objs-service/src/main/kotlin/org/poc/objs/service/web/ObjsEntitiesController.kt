package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Entity pool HTTP API under `/api/v1/objs/entities` (WI-004): plain CRUD, no graph scope.
 * Graph membership is managed via [ObjsGraphsController] (`/graphs/{id}/members/{entityId}`).
 */
@RestController
@RequestMapping("/api/v1/objs/entities")
@Tag(name = "entities")
class ObjsEntitiesController(
    private val store: BoMGraphStore,
) {
    @Schema(description = "Pool entity write body")
    data class EntityWriteBody(
        val id: UUID? = null,
        val type: String,
        val schemaVersion: String,
        val payload: MutableMap<String, Any?> = mutableMapOf(),
        val annotations: MutableMap<String, String> = mutableMapOf(),
    )

    @GetMapping
    @Operation(summary = "List pool entities")
    fun list(): List<BoMEntity> = store.listEntities()

    @PostMapping
    @Operation(summary = "Create an entity in the pool only (no graph membership)")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Created pool entity (id assigned)",
            content = [Content(schema = Schema(implementation = BoMEntity::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
    )
    fun create(@RequestBody body: EntityWriteBody): ResponseEntity<Any> {
        val entity = body.toEntity()
        val result = store.write(BoMGraph(entities = mutableListOf(entity)))
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(entity)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a pool entity by id")
    fun get(@PathVariable id: UUID): ResponseEntity<BoMEntity> {
        val entity = store.getEntity(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(entity)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payload/annotations/type/schemaVersion of an existing pool entity")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated pool entity"),
        ApiResponse(responseCode = "404", description = "Entity not found"),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
    )
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: EntityWriteBody,
    ): ResponseEntity<Any> {
        if (store.getEntity(id) == null) {
            return ResponseEntity.notFound().build()
        }
        val entity = body.toEntity().also { it.id = id }
        val result = store.write(BoMGraph(entities = mutableListOf(entity)))
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.ok(entity)
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Remove an entity from the pool",
        description = "Cascades: this entity's memberships and incident edges are removed too.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "404", description = "Entity not found"),
    )
    fun delete(@PathVariable id: UUID): ResponseEntity<Any> {
        val result = store.deleteEntity(id)
        if (!result.isValid) {
            val notFound = result.issues.any { it.code == "ENTITY_NOT_FOUND" }
            val status = if (notFound) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            return ResponseEntity.status(status).body(result)
        }
        return ResponseEntity.noContent().build()
    }

    private fun EntityWriteBody.toEntity() = BoMEntity(
        id = id,
        type = type,
        schemaVersion = schemaVersion,
        payload = payload,
        annotations = annotations,
    )
}
