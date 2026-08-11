package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.seed.CanonicalSeedSerializer
import org.poc.objs.core.seed.GRAPH_SEED_KINDS
import org.poc.objs.core.seed.SeedImportException
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
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
@Tag(name = "graph")
class ObjsGraphController(
    private val store: BoMGraphStore,
    private val seedImporter: SeedImporter,
    private val seedSerializer: CanonicalSeedSerializer,
    private val namedGraphs: BoMNamedGraphStore,
) {
    @PostMapping("/graph/validate")
    @Operation(
        summary = "Dry-run validate a graph mutation (no persist)",
        description = "Accepts the same BoMGraphMutation body as the retired PUT /graph (upserts and " +
            "optional deletes) but never persists; use POST /graphs/{id}/validate for a graph-scoped dry-run.",
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
        summary = "Export one graph as a bounded Graph seed document",
        description = "Requires a `graphId` query param naming an existing graph; never dumps the entire pool.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Seed YAML for the graph's members + graph-local edges"),
        ApiResponse(responseCode = "400", description = "Missing graphId or unknown format"),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun exportGraph(
        @RequestParam format: String,
        @RequestParam(required = false) graphId: UUID?,
    ): ResponseEntity<Any> {
        if (format != ObjsIoFormats.SEEDS) {
            return ObjsIoFormats.unknownFormat(format)
        }
        if (graphId == null) {
            return ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue(
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
