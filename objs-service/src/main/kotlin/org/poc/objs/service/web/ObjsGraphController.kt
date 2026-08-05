package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.match.BoMMatcherDsl
import org.poc.objs.core.match.BoMMatcherFormat
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.seed.CanonicalSeedSerializer
import org.poc.objs.core.seed.GRAPH_SEED_KINDS
import org.poc.objs.core.seed.SeedImportException
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Graph HTTP API under `/api/v1/objs/graph`.
 *
 * Matching/query uses [queryGraph] only (`POST /graph/query`).
 */
@RestController
@RequestMapping("/api/v1/objs")
@Tag(name = "graph")
class ObjsGraphController(
    private val store: BoMGraphStore,
    private val seedImporter: SeedImporter,
    private val seedSerializer: CanonicalSeedSerializer,
    private val matcherDsl: BoMMatcherDsl = BoMMatcherDsl.create(),
) {
    @PutMapping("/graph")
    @Operation(
        summary = "Mutate a graph batch (upsert entities/edges and/or delete by id)",
        description = "Body is BoMGraphMutation: `{ upsert: { entities, edges }, delete: { entities, edges } }` " +
            "where delete lists are ids. Empty delete = upsert-only. Deletes and upserts run in one transaction.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Persisted upsert graph with ids assigned (delete lists not echoed)",
            content = [Content(schema = Schema(implementation = BoMGraph::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
    )
    fun putGraph(@RequestBody mutation: BoMGraphMutation): ResponseEntity<Any> {
        val result = store.mutate(mutation)
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.ok(mutation.graph())
    }

    @PostMapping("/graph/validate")
    @Operation(
        summary = "Dry-run validate a graph mutation (no persist)",
        description = "Accepts the same BoMGraphMutation body as PUT (upserts and optional deletes).",
    )
    @ApiResponse(responseCode = "200", description = "Validation result (may be invalid)")
    fun validateGraph(@RequestBody mutation: BoMGraphMutation): BoMValidationResult =
        store.validateMutation(mutation)

    @PostMapping(
        "/graph/import",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Import Graph seed documents (MERGE, transactional)")
    fun importGraph(
        @RequestParam format: String,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Any> {
        if (format != ObjsIoFormats.SEEDS) {
            return ObjsIoFormats.unknownFormat(format)
        }
        val yaml = file.bytes.toString(Charsets.UTF_8)
        return try {
            ResponseEntity.ok(seedImporter.importYaml(yaml, GRAPH_SEED_KINDS))
        } catch (ex: SeedImportException) {
            ResponseEntity.badRequest().body(ex.result)
        }
    }

    @GetMapping("/graph/export")
    @Operation(
        summary = "Export a bounded Graph seed document",
        description = "Requires a non-empty annotation filter; never dumps the entire graph.",
    )
    fun exportGraph(
        @RequestParam format: String,
        @RequestParam params: Map<String, String>,
    ): ResponseEntity<Any> {
        if (format != ObjsIoFormats.SEEDS) {
            return ObjsIoFormats.unknownFormat(format)
        }
        val filters = params.filterKeys { it != "format" }
        if (filters.isEmpty()) {
            return ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "FILTER_EMPTY",
                        message = "Annotation filter required; refusing to export entire graph",
                    ),
                ),
            )
        }
        val subgraph = store.selectSubgraphMatchAll(filters)
        if (subgraph.entities.isEmpty() && subgraph.edges.isEmpty()) {
            val yaml = seedSerializer.serializeCatalogs(
                includeSchemas = false,
                includeEdgeRules = false,
                graphs = emptyList(),
            )
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, ObjsIoFormats.YAML_MEDIA_TYPE)
                .body(yaml)
        }
        val name = "export/" + filters.entries.sortedBy { it.key }
            .joinToString(",") { "${it.key}=${it.value}" }
        val entityKeys = subgraph.entities.associate { entity ->
            val id = requireNotNull(entity.id)
            id to id.toString()
        }
        val edgeKeys = subgraph.edges.associate { edge ->
            val id = requireNotNull(edge.id)
            id to id.toString()
        }
        val yaml = seedSerializer.serializeCatalogs(
            includeSchemas = false,
            includeEdgeRules = false,
            graphs = listOf(
                CanonicalSeedSerializer.GraphExport(
                    name = name,
                    entities = subgraph.entities,
                    edges = subgraph.edges,
                    entityKeys = entityKeys,
                    edgeKeys = edgeKeys,
                ),
            ),
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, ObjsIoFormats.YAML_MEDIA_TYPE)
            .body(yaml)
    }

    @PostMapping(
        "/graph/query",
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            "application/yaml",
            "text/yaml",
            "application/x-yaml",
        ],
    )
    @Operation(summary = "Select induced subgraph by matcher DSL (JSON or YAML)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Subgraph",
            content = [Content(schema = Schema(implementation = BoMSubgraph::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher DSL or expression",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
    )
    fun queryGraph(request: HttpServletRequest): ResponseEntity<Any> {
        val body = request.inputStream.readBytes().toString(StandardCharsets.UTF_8)
        val format = resolveFormat(request.contentType?.let { MediaType.parseMediaType(it) })
        return try {
            val matcher = matcherDsl.decode(body, format)
            ResponseEntity.ok(store.selectSubgraph(matcher))
        } catch (ex: BoMValidationException) {
            ResponseEntity.badRequest().body(ex.result)
        }
    }

    @DeleteMapping("/graph")
    @Operation(
        summary = "Batch delete entities and/or edges",
        description = "Deprecated shim: prefer PUT /graph with delete.entities / delete.edges.",
        deprecated = true,
    )
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

    private fun resolveFormat(contentType: MediaType?): BoMMatcherFormat {
        val subtype = contentType?.subtype?.lowercase().orEmpty()
        return if (subtype.contains("yaml") || subtype.contains("yml")) {
            BoMMatcherFormat.YAML
        } else {
            BoMMatcherFormat.JSON
        }
    }

    @Schema(description = "Batch delete request")
    data class GraphDeleteRequest(
        val entityIds: List<UUID>? = null,
        val edgeIds: List<UUID>? = null,
    )
}
