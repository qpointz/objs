package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.match.MatcherDsl
import org.poc.objs.api.match.MatcherFormat
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.api.validation.ValidationException
import org.poc.objs.api.validation.ValidationResult
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
import org.poc.objs.api.domain.PageRequest
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Entity pool HTTP API under `/api/v1/objs/entities` (WI-004): plain CRUD, no graph scope.
 * Graph entity attachment is managed via [ObjsGraphsController] (`/graphs/{id}/entities/{entityId}`).
 */
@RestController
@RequestMapping("/api/v1/objs/entities")
@Tag(
    name = "entities",
    description = "Shared pool entity CRUD and history, plus graph membership attach/detach",
)
class ObjsEntitiesController(
    private val store: GraphStore,
    private val matcherDsl: MatcherDsl = MatcherDsl.create(),
) {
    @Schema(description = "Pool entity write body")
    data class EntityWriteBody(
        @field:Schema(description = "Caller-chosen entity id; omit on create to let the store assign one")
        val id: UUID? = null,
        @field:Schema(description = "Registry schema type name, e.g. `Component`")
        val type: String,
        @field:Schema(description = "Registry schema version for [type], e.g. `1`")
        val schemaVersion: String,
        @field:Schema(description = "Entity payload; validated against the registry schema for type@schemaVersion")
        val payload: MutableMap<String, Any?> = mutableMapOf(),
        @field:Schema(description = "Free-form string annotations (not schema-validated)")
        val annotations: MutableMap<String, String> = mutableMapOf(),
    )

    @GetMapping
    @Operation(
        summary = "List pool entities",
        description = "Whole pool including orphans; prefer POST /entities/query with paging for large stores.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "All pool entities",
            content = [Content(array = ArraySchema(schema = Schema(implementation = Entity::class)))],
        ),
    )
    fun list(): List<Entity> = store.listEntities()

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
        description = "Body is a matcher document in JSON or YAML (pick via Content-Type). Includes " +
            "orphans (no graph membership). Accepts bare obj-expr or a chain of obj-expr only. " +
            "Equality/`&&` pushdown uses SQL (`type = ?`, …). Edges are not returned (graph-local). " +
            "Use /graphs/{id}/query or /graphs/query for graph-scoped selection. When `page` or `size` " +
            "is set, the response is a paged envelope (`entities`, `edges`, `total`, `page`, `size`) " +
            "instead of GraphContents.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Matching pool entities (edges empty); paged envelope when page/size is set",
            content = [Content(schema = Schema(implementation = GraphContents::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher DSL or non-obj-expr stage",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun query(
        request: HttpServletRequest,
        @Parameter(description = "Zero-based page index; set page or size to switch to the paged envelope")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "Page size; set page or size to switch to the paged envelope")
        @RequestParam(required = false) size: Int?,
    ): ResponseEntity<Any> {
        val matcher = matcherDsl.decode(readBody(request), resolveFormat(request))
        if (page == null && size == null) {
            return ResponseEntity.ok(store.selectFromPool(matcher))
        }
        val paged = store.selectFromPool(matcher, PageRequest.of(page, size))
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
            content = [Content(schema = Schema(implementation = Entity::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun create(@RequestBody body: EntityWriteBody): ResponseEntity<Any> {
        val entity = body.toEntity()
        val result = store.write(Graph(entities = mutableListOf(entity)))
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(entity)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a pool entity by id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Pool entity",
            content = [Content(schema = Schema(implementation = Entity::class))],
        ),
        ApiResponse(responseCode = "404", description = "Entity not found"),
    )
    fun get(
        @Parameter(description = "Pool entity id") @PathVariable id: UUID,
    ): ResponseEntity<Entity> {
        val entity = store.getEntity(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(entity)
    }

    @GetMapping("/{id}/versions/stats")
    @Operation(
        summary = "Entity version stats: total count + newest recent N",
        description = "Cheap history probe for UIs: full version count plus the newest [recent] summaries.",
    )
    @ApiResponse(responseCode = "200", description = "Version count and newest summaries")
    fun versionStats(
        @Parameter(description = "Pool entity id") @PathVariable id: UUID,
        @Parameter(description = "How many of the newest version summaries to include")
        @RequestParam(defaultValue = "5") recent: Int,
    ) = store.entityVersionStats(id, recent)

    @GetMapping("/{id}/graphs")
    @Operation(
        summary = "Live graphs containing this entity (HEAD membership only)",
        description = "Ignores deep-version pins. [total] is unfiltered live count; [items] may be " +
            "filtered by q (open-graph text match) and limited. Sorted by graph updatedAt desc.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Envelope with `items` (graph headers) and `total`"),
        ApiResponse(responseCode = "404", description = "Entity not found"),
    )
    fun listLiveGraphs(
        @Parameter(description = "Pool entity id") @PathVariable id: UUID,
        @Parameter(description = "Open-graph text filter over graph id and annotation key/value")
        @RequestParam(required = false) q: String?,
        @Parameter(description = "Maximum number of graph headers in `items`; `total` stays unfiltered")
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<Any> {
        if (store.getEntity(id) == null) {
            return ResponseEntity.notFound().build()
        }
        val result = store.listLiveGraphHeadersForEntity(id, q, limit)
        return ResponseEntity.ok(
            mapOf(
                "items" to result.items,
                "total" to result.total,
            ),
        )
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "List entity deep-capture versions, newest first")
    @ApiResponse(responseCode = "200", description = "Entity version summaries")
    fun listVersions(
        @Parameter(description = "Pool entity id") @PathVariable id: UUID,
    ) = store.listEntityVersions(id)

    @PostMapping("/{id}/compact")
    @Operation(
        summary = "Compact orphan entity version rows",
        description = "Deletes entity version rows not pinned by any graph freeze and not equal to live head_version.",
    )
    @ApiResponse(responseCode = "200", description = "`removed`: number of version rows deleted")
    fun compact(
        @Parameter(description = "Pool entity id") @PathVariable id: UUID,
    ): Map<String, Int> =
        mapOf("removed" to store.compactEntity(id))

    @GetMapping("/{id}/versions/{version}")
    @Operation(summary = "Fetch one entity deep-capture version")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Frozen entity at that version"),
        ApiResponse(responseCode = "404", description = "Entity or version not found"),
    )
    fun getVersion(
        @Parameter(description = "Pool entity id") @PathVariable id: UUID,
        @Parameter(description = "Entity version number") @PathVariable version: Long,
    ): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(store.getEntityVersion(id, version))
        } catch (ex: org.poc.objs.api.domain.GraphException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to (ex.message ?: ex.code), "code" to ex.code),
            )
        }

    @PutMapping("/{id}")
    @Operation(summary = "Update payload/annotations/type/schemaVersion of an existing pool entity")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Updated pool entity",
            content = [Content(schema = Schema(implementation = Entity::class))],
        ),
        ApiResponse(responseCode = "404", description = "Entity not found"),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun update(
        @Parameter(description = "Pool entity id; wins over any `id` in the body") @PathVariable id: UUID,
        @RequestBody body: EntityWriteBody,
    ): ResponseEntity<Any> {
        if (store.getEntity(id) == null) {
            return ResponseEntity.notFound().build()
        }
        val entity = body.toEntity().also { it.id = id }
        val result = store.write(Graph(entities = mutableListOf(entity)))
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
        ApiResponse(
            responseCode = "404",
            description = "Entity not found",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Delete rejected",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun delete(
        @Parameter(description = "Pool entity id") @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val result = store.deleteEntity(id)
        if (!result.isValid) {
            val notFound = result.issues.any { it.code == "ENTITY_NOT_FOUND" }
            val status = if (notFound) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            return ResponseEntity.status(status).body(result)
        }
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ValidationResult> =
        ResponseEntity.badRequest().body(ex.result)

    private fun EntityWriteBody.toEntity() = Entity(
        id = id,
        type = type,
        schemaVersion = schemaVersion,
        payload = payload,
        annotations = annotations,
    )

    private fun readBody(request: HttpServletRequest): String =
        request.inputStream.readBytes().toString(StandardCharsets.UTF_8)

    private fun resolveFormat(request: HttpServletRequest): MatcherFormat {
        val subtype = request.contentType?.let { MediaType.parseMediaType(it) }?.subtype?.lowercase().orEmpty()
        return if (subtype.contains("yaml") || subtype.contains("yml")) {
            MatcherFormat.YAML
        } else {
            MatcherFormat.JSON
        }
    }
}
