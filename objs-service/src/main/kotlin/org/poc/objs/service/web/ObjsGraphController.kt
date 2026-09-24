package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.api.domain.GraphMutation
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.core.seed.CanonicalSeedSerializer
import org.poc.objs.api.seed.GRAPH_SEED_KINDS
import org.poc.objs.api.seed.SeedImportException
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.api.validation.ValidationResult
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

/**
 * Graph seed import/export + dry-run validate under `/api/v1/objs/graph` (WI-004: slimmed).
 *
 * The unscoped whole-store-as-graph mutate (`PUT /graph`), query (`POST /graph/query`) and
 * delete (`DELETE /graph`) endpoints were removed — see [ObjsEntitiesController] (pool CRUD) and
 * [ObjsGraphsController] (`/graphs` and its sub-paths: header CRUD, membership, graph-scoped
 * mutate/query).
 */
@RestController
@RequestMapping("/api/v1/objs")
@Tag(name = "seeds", description = "Graph seed validate, import, and export")
class ObjsGraphController(
    private val store: GraphStore,
    private val seedImporter: SeedImporter,
    private val seedSerializer: CanonicalSeedSerializer,
    private val namedGraphs: NamedGraphStore,
) {
    @PostMapping("/graph/validate")
    @Operation(
        summary = "Dry-run validate a graph mutation (no persist)",
        description = "Accepts the same GraphMutation body as graph mutate (entities/edges set and " +
            "optional unset) but never persists; use POST /graphs/{id}/validate for a graph-scoped dry-run.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Validation result (may be invalid)",
        content = [Content(schema = Schema(implementation = ValidationResult::class))],
    )
    fun validateGraph(@RequestBody mutation: GraphMutation): ValidationResult =
        store.validateMutation(mutation)

    @PostMapping(
        "/graph/import",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(
        summary = "Import Graph seed documents (MERGE, transactional)",
        description = "Multipart upload of a seed YAML holding Graph-kind documents. All documents apply " +
            "in one transaction: any validation failure rolls the whole import back.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Import summary (applied seed documents)"),
        ApiResponse(
            responseCode = "400",
            description = "Unknown format, or seed parse/validation failure",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun importGraph(
        @Parameter(description = "Payload format; only `seeds` is supported")
        @RequestParam format: String,
        @Parameter(description = "Seed YAML file holding Graph-kind documents")
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
        summary = "Export one graph as a bounded Graph seed document",
        description = "Requires a `graphId` query param naming an existing graph; never dumps the entire pool.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Seed YAML for the graph's members + graph-local edges",
            content = [Content(mediaType = ObjsIoFormats.YAML_MEDIA_TYPE, schema = Schema(type = "string"))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Missing graphId or unknown format",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun exportGraph(
        @Parameter(description = "Payload format; only `seeds` is supported")
        @RequestParam format: String,
        @Parameter(description = "Graph to export; required — the endpoint refuses to dump the whole pool")
        @RequestParam(required = false) graphId: UUID?,
    ): ResponseEntity<Any> {
        if (format != ObjsIoFormats.SEEDS) {
            return ObjsIoFormats.unknownFormat(format)
        }
        if (graphId == null) {
            return ResponseEntity.badRequest().body(
                ValidationResult.of(
                    ValidationIssue(
                        code = "GRAPH_ID_REQUIRED",
                        message = "graphId query param required; refusing to export the entire pool",
                    ),
                ),
            )
        }
        val resolved = namedGraphs.get(graphId)
            ?: return ResponseEntity.notFound().build()
        val contents = resolved.contents
        val name = "export/graph=$graphId"
        val entityKeys = contents.entities.associate { entity ->
            val id = requireNotNull(entity.id)
            id to id.toString()
        }
        val edgeKeys = contents.edges.associate { edge ->
            val id = requireNotNull(edge.id)
            id to id.toString()
        }
        val yaml = seedSerializer.serializeCatalogs(
            includeSchemas = false,
            includeEdgeRules = false,
            graphs = listOf(
                CanonicalSeedSerializer.GraphExport(
                    name = name,
                    entities = contents.entities,
                    edges = contents.edges,
                    entityKeys = entityKeys,
                    edgeKeys = edgeKeys,
                ),
            ),
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, ObjsIoFormats.YAML_MEDIA_TYPE)
            .body(yaml)
    }
}
