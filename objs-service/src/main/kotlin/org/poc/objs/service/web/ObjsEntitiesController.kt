package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.match.BoMMatcherDsl
import org.poc.objs.core.match.BoMMatcherFormat
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.poc.objs.core.domain.BoMPageRequest
import java.nio.charset.StandardCharsets
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
    private val matcherDsl: BoMMatcherDsl = BoMMatcherDsl.create(),
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

    @PostMapping(
        "/query",
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            "application/yaml",
            "text/yaml",
            "application/x-yaml",
        ],
    )
    @Operation(
        summary = "Matcher DSL (obj-expr) over the entity pool",
        description = "Includes orphans (no graph membership). Accepts bare obj-expr or a chain of " +
            "obj-expr only. Equality/`&&` pushdown uses SQL (`type = ?`, …). Edges are not returned " +
            "(graph-local). Use /graphs/{id}/query or /graphs/query for graph-scoped selection.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Matching pool entities (edges empty)",
            content = [Content(schema = Schema(implementation = BoMGraphContents::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher DSL or non-obj-expr stage",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
    )
    fun query(
        request: HttpServletRequest,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): ResponseEntity<Any> {
        val matcher = matcherDsl.decode(readBody(request), resolveFormat(request))
        if (page == null && size == null) {
            return ResponseEntity.ok(store.selectFromPool(matcher))
        }
        val paged = store.selectFromPool(matcher, BoMPageRequest.of(page, size))
        return ResponseEntity.ok(
            mapOf(
                "entities" to paged.items,
                "edges" to emptyList<Any>(),
                "total" to paged.total,
                "page" to paged.page,
                "size" to paged.size,
            ),
        )
    }

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

    @GetMapping("/{id}/versions/stats")
    @Operation(summary = "Entity version stats: total count + newest recent N")
    fun versionStats(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "5") recent: Int,
    ) = store.entityVersionStats(id, recent)

    @GetMapping("/{id}/versions")
    @Operation(summary = "List entity deep-capture versions, newest first")
    fun listVersions(@PathVariable id: UUID) = store.listEntityVersions(id)

    @GetMapping("/{id}/versions/{version}")
    @Operation(summary = "Fetch one entity deep-capture version")
    fun getVersion(
        @PathVariable id: UUID,
        @PathVariable version: Long,
    ): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(store.getEntityVersion(id, version))
        } catch (ex: org.poc.objs.core.domain.BoMGraphException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to (ex.message ?: ex.code), "code" to ex.code),
            )
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

    @ExceptionHandler(BoMValidationException::class)
    fun handleValidation(ex: BoMValidationException): ResponseEntity<BoMValidationResult> =
        ResponseEntity.badRequest().body(ex.result)

    private fun EntityWriteBody.toEntity() = BoMEntity(
        id = id,
        type = type,
        schemaVersion = schemaVersion,
        payload = payload,
        annotations = annotations,
    )

    private fun readBody(request: HttpServletRequest): String =
        request.inputStream.readBytes().toString(StandardCharsets.UTF_8)

    private fun resolveFormat(request: HttpServletRequest): BoMMatcherFormat {
        val subtype = request.contentType?.let { MediaType.parseMediaType(it) }?.subtype?.lowercase().orEmpty()
        return if (subtype.contains("yaml") || subtype.contains("yml")) {
            BoMMatcherFormat.YAML
        } else {
            BoMMatcherFormat.JSON
        }
    }
}
