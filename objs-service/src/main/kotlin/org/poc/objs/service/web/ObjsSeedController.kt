package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.seed.CanonicalSeedSerializer
import org.poc.objs.core.seed.SeedImportException
import org.poc.objs.core.seed.SeedImportResult
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Seed import/export under `/api/v1/objs/seeds`.
 * Graph export requires a non-empty annotation filter (never unbounded load-all).
 */
@RestController
@RequestMapping("/api/v1/objs/seeds")
@Tag(name = "seeds")
class ObjsSeedController(
    private val importer: SeedImporter,
    private val serializer: CanonicalSeedSerializer,
    private val schemas: BoMSchemaCatalog,
    private val edgeRules: BoMAllowedEdgeCatalog,
    private val graphStore: BoMGraphStore,
) {
    @PostMapping(
        "/import",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Import multi-document seed YAML (MERGE, transactional)")
    fun importSeeds(@RequestPart("file") file: MultipartFile): ResponseEntity<SeedImportResult> {
        val yaml = file.bytes.toString(Charsets.UTF_8)
        return try {
            ResponseEntity.ok(importer.importYaml(yaml))
        } catch (ex: SeedImportException) {
            ResponseEntity.badRequest().body(ex.result)
        }
    }

    @GetMapping("/export")
    @Operation(
        summary = "Export catalogs and a bounded graph as canonical multi-document YAML",
        description = "Always exports ObjectSchema and AllowedEdgeRule catalogs. " +
            "Graph documents are included only when annotation query params are provided.",
    )
    fun exportSeeds(@RequestParam params: Map<String, String>): ResponseEntity<Any> {
        val graphs = if (params.isEmpty()) {
            emptyList()
        } else {
            val subgraph = graphStore.selectSubgraphMatchAll(params)
            if (subgraph.entities.isEmpty() && subgraph.edges.isEmpty()) {
                emptyList()
            } else {
                val name = "export/" + params.entries.sortedBy { it.key }
                    .joinToString(",") { "${it.key}=${it.value}" }
                val entityKeys = subgraph.entities.associate { entity ->
                    val id = requireNotNull(entity.id)
                    id to id.toString()
                }
                val edgeKeys = subgraph.edges.associate { edge ->
                    val id = requireNotNull(edge.id)
                    id to id.toString()
                }
                listOf(
                    CanonicalSeedSerializer.GraphExport(
                        name = name,
                        entities = subgraph.entities,
                        edges = subgraph.edges,
                        entityKeys = entityKeys,
                        edgeKeys = edgeKeys,
                    ),
                )
            }
        }

        val yaml = serializer.serializeCatalogs(
            includeSchemas = true,
            includeEdgeRules = true,
            graphs = graphs,
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, YAML_MEDIA_TYPE)
            .body(yaml)
    }

    @GetMapping("/export/graph")
    @Operation(summary = "Export a bounded Graph seed document (annotation filter required)")
    fun exportGraph(@RequestParam params: Map<String, String>): ResponseEntity<Any> {
        if (params.isEmpty()) {
            return ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "FILTER_EMPTY",
                        message = "Annotation filter required; refusing to export entire graph",
                    ),
                ),
            )
        }
        return exportSeeds(params)
    }

    companion object {
        const val YAML_MEDIA_TYPE = "application/yaml"
    }
}
