package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMMutateMode
import org.poc.objs.core.domain.BoMResolvedGraph
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.BoMGraphException
import org.poc.objs.core.domain.BoMGraphHeader
import org.poc.objs.core.domain.BoMGraphListItem
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.match.BoMMatcherDsl
import org.poc.objs.core.match.BoMMatcherFormat
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Graph HTTP API under `/api/v1/objs/graphs` (WI-004): header CRUD, membership, graph-scoped
 * mutate/query, and optional clone. Replaces the removed unscoped whole-store-as-graph API.
 */
@RestController
@RequestMapping("/api/v1/objs/graphs")
@Tag(name = "graphs")
class ObjsGraphsController(
    private val namedGraphs: BoMNamedGraphStore,
    private val graphStore: BoMGraphStore,
    private val matcherDsl: BoMMatcherDsl = BoMMatcherDsl.create(),
) {
    @Schema(description = "Graph header write body")
    data class GraphWriteBody(
        val id: UUID? = null,
        val annotations: Map<String, String> = emptyMap(),
        val entityIds: Set<UUID> = emptySet(),
    )

    @Schema(description = "Graph header response: id + annotations + resolved BoMGraphContents")
    data class GraphResponse(
        val id: UUID,
        val annotations: Map<String, String>,
        val graph: BoMGraphContents,
    )

    data class CloneBody(
        val annotations: Map<String, String> = emptyMap(),
    )

    @Schema(description = "Open-graph search envelope (G-U10); additive fields may appear later")
    data class GraphSearchResponse(
        val items: List<BoMGraphHeader>,
    )

    @GetMapping
    @Operation(summary = "List graph headers")
    fun list(): List<BoMGraphListItem> = namedGraphs.list()

    @GetMapping("/search")
    @Operation(
        summary = "Search graph headers (open-graph dialog)",
        description = "G-U10 extensible search contract. Empty `q` without `expr` returns `{ items: [] }` " +
            "(never the full catalog). v1 match: id / UUID-prefix + case-insensitive substring on id and " +
            "annotation key/value; optional `expr` is a graph-expr (AND with `q` when both set). " +
            "Additive query params and response fields may be added later; FTS is out of scope for v1.",
    )
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) expr: String?,
        @RequestParam(required = false, defaultValue = "15") limit: Int,
    ): GraphSearchResponse = GraphSearchResponse(
        items = namedGraphs.search(q = q, expr = expr, limit = limit),
    )

    @PostMapping
    @Operation(summary = "Create a graph header, optionally seeding membership with existing pool entity ids")
    fun create(@RequestBody body: GraphWriteBody): ResponseEntity<GraphResponse> {
        val created = namedGraphs.create(
            BoMGraphSpec(id = body.id, annotations = body.annotations, entityIds = body.entityIds),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(created.toResponse())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get graph header + resolved members and graph-local edges")
    fun get(@PathVariable id: UUID): ResponseEntity<GraphResponse> {
        val resolved = namedGraphs.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(resolved.toResponse())
    }

    @PutMapping("/{id}/annotations")
    @Operation(summary = "Replace graph header annotations (membership unchanged)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated graph"),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun updateAnnotations(
        @PathVariable id: UUID,
        @RequestBody body: GraphWriteBody,
    ): ResponseEntity<GraphResponse> {
        val updated = namedGraphs.updateAnnotations(id, body.annotations)
        return ResponseEntity.ok(updated.toResponse())
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "MERGE-mutate this graph (patch)",
        description = "MERGE: `entities.set` / `edges.set` upsert; `entities.unset` detaches membership " +
            "(pool kept); `edges.unset` drops this graph's edges. Omission never deletes. " +
            "Verb sets mode (omit `mode` on wire or it must be MERGE).",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Resolved graph after mutation"),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun mutateMerge(
        @PathVariable id: UUID,
        @RequestBody mutation: BoMGraphMutation,
    ): ResponseEntity<Any> = mutateWithMode(id, mutation, BoMMutateMode.MERGE)

    @PutMapping("/{id}")
    @Operation(
        summary = "REPLACE-mutate this graph (overwrite contents)",
        description = "REPLACE: `entities.set` + `edges.set` are the full desired membership and " +
            "graph-local edges; unlisted members detach / edges drop. Non-empty `unset` is rejected. " +
            "Verb sets mode (omit `mode` on wire or it must be REPLACE).",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Resolved graph after mutation"),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun mutateReplace(
        @PathVariable id: UUID,
        @RequestBody mutation: BoMGraphMutation,
    ): ResponseEntity<Any> = mutateWithMode(id, mutation, BoMMutateMode.REPLACE)

    @PatchMapping("/{id}/validate")
    @Operation(summary = "Dry-run MERGE validate (no persist)")
    fun validateMerge(
        @PathVariable id: UUID,
        @RequestBody mutation: BoMGraphMutation,
    ): ResponseEntity<Any> = validateWithMode(id, mutation, BoMMutateMode.MERGE)

    @PutMapping("/{id}/validate")
    @Operation(summary = "Dry-run REPLACE validate (no persist)")
    fun validateReplace(
        @PathVariable id: UUID,
        @RequestBody mutation: BoMGraphMutation,
    ): ResponseEntity<Any> = validateWithMode(id, mutation, BoMMutateMode.REPLACE)

    @PostMapping("/{id}/validate")
    @Operation(
        summary = "Dry-run MERGE validate (no persist)",
        description = "Alias of PATCH /graphs/{id}/validate (MERGE). Prefer PATCH.",
    )
    fun validatePost(
        @PathVariable id: UUID,
        @RequestBody mutation: BoMGraphMutation,
    ): ResponseEntity<Any> = validateWithMode(id, mutation, BoMMutateMode.MERGE)

    private fun mutateWithMode(
        id: UUID,
        mutation: BoMGraphMutation,
        mode: BoMMutateMode,
    ): ResponseEntity<Any> {
        val resolved = resolveVerbMode(mutation, mode) ?: return modeMismatch(mode)
        val result = namedGraphs.mutate(id, resolved)
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.ok(requireNotNull(namedGraphs.get(id)).toResponse())
    }

    private fun validateWithMode(
        id: UUID,
        mutation: BoMGraphMutation,
        mode: BoMMutateMode,
    ): ResponseEntity<Any> {
        val resolved = resolveVerbMode(mutation, mode) ?: return modeMismatch(mode)
        return ResponseEntity.ok(namedGraphs.validateMutate(id, resolved))
    }

    /** Verb wins; body `mode` may be omitted (defaults MERGE) or must match the verb. */
    private fun resolveVerbMode(mutation: BoMGraphMutation, verbMode: BoMMutateMode): BoMGraphMutation? {
        if (mutation.mode != BoMMutateMode.MERGE && mutation.mode != verbMode) {
            return null
        }
        return mutation.copy(mode = verbMode)
    }

    private fun modeMismatch(verbMode: BoMMutateMode): ResponseEntity<Any> =
        ResponseEntity.badRequest().body(
            BoMValidationResult.of(
                BoMValidationIssue(
                    code = "MUTATE_MODE_MISMATCH",
                    message = "Body mode disagrees with HTTP verb (expected $verbMode)",
                ),
            ),
        )

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete graph header + membership + edges (CASCADE); pool entities kept")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        namedGraphs.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/members/{entityId}")
    @Operation(summary = "Attach an existing pool entity to this graph (membership row only)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Resolved graph after attach"),
        ApiResponse(responseCode = "404", description = "Graph or entity not found"),
    )
    fun attach(
        @PathVariable id: UUID,
        @PathVariable entityId: UUID,
    ): ResponseEntity<GraphResponse> {
        namedGraphs.attach(id, entityId)
        return ResponseEntity.ok(requireNotNull(namedGraphs.get(id)).toResponse())
    }

    @DeleteMapping("/{id}/members/{entityId}")
    @Operation(summary = "Detach an entity from this graph (pool entity kept)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Detached"),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun detach(
        @PathVariable id: UUID,
        @PathVariable entityId: UUID,
    ): ResponseEntity<Void> {
        namedGraphs.detach(id, entityId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping(
        "/{id}/query",
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            "application/yaml",
            "text/yaml",
            "application/x-yaml",
        ],
    )
    @Operation(summary = "Matcher DSL (obj-expr / chained) scoped to this graph's stored members")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Graph contents (selected members + induced edges)",
            content = [Content(schema = Schema(implementation = BoMGraphContents::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher DSL or expression",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun queryInGraph(
        @PathVariable id: UUID,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        val matcher = matcherDsl.decode(readBody(request), resolveFormat(request))
        return ResponseEntity.ok(graphStore.selectInGraph(id, matcher))
    }

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
        summary = "Matcher DSL over graph headers (requires stage-0 all or graph-expr)",
        description = "Fails closed (400, MATCHER_GRAPH_SCOPE_REQUIRED) unless the matcher is (or starts " +
            "with) all / graph-expr — there is no whole-store-as-graph scan. `all: true` unions every " +
            "graph's members and edges (distinct by id).",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Union of matching graphs' stored members + graph-local edges",
            content = [Content(schema = Schema(implementation = BoMGraphContents::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher DSL, or missing stage-0 all / graph-expr",
            content = [Content(schema = Schema(implementation = BoMValidationResult::class))],
        ),
    )
    fun queryGraphs(request: HttpServletRequest): ResponseEntity<Any> {
        val matcher = matcherDsl.decode(readBody(request), resolveFormat(request))
        return ResponseEntity.ok(graphStore.select(matcher))
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone this graph's members + edges into a new, independent graph")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "New cloned graph"),
        ApiResponse(responseCode = "404", description = "Source graph not found"),
    )
    fun clone(
        @PathVariable id: UUID,
        @RequestBody(required = false) body: CloneBody?,
    ): ResponseEntity<GraphResponse> {
        val cloned = namedGraphs.clone(id, body?.annotations ?: emptyMap())
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned.toResponse())
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "Snapshot: pin current HEAD as a deep graph version (same graph id)")
    fun createVersion(
        @PathVariable id: UUID,
        @RequestBody(required = false) body: CloneBody?,
    ): ResponseEntity<org.poc.objs.core.domain.BoMGraphVersionSummary> {
        val created = namedGraphs.createDeepGraphVersion(id, body?.annotations ?: emptyMap())
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "List deep graph versions, newest first")
    fun listVersions(@PathVariable id: UUID) = namedGraphs.listGraphVersions(id)

    @GetMapping("/{id}/versions/{version}")
    @Operation(summary = "Reconstruct a deep graph version (read-only)")
    fun getVersion(
        @PathVariable id: UUID,
        @PathVariable version: Long,
    ): ResponseEntity<GraphResponse> {
        val resolved = namedGraphs.getGraphVersion(id, version)
        return ResponseEntity.ok(resolved.toResponse())
    }

    @PostMapping(
        "/{id}/versions/{version}/query",
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            "application/yaml",
            "text/yaml",
            "application/x-yaml",
        ],
    )
    @Operation(summary = "Matcher DSL scoped to a reconstructed deep graph version")
    fun queryInGraphVersion(
        @PathVariable id: UUID,
        @PathVariable version: Long,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        val matcher = matcherDsl.decode(readBody(request), resolveFormat(request))
        return ResponseEntity.ok(graphStore.selectInGraphVersion(id, version, matcher))
    }

    @ExceptionHandler(BoMValidationException::class)
    fun handleValidation(ex: BoMValidationException): ResponseEntity<BoMValidationResult> {
        val notFound = ex.result.issues.any { it.code == "GRAPH_NOT_FOUND" }
        val status = if (notFound) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(ex.result)
    }

    @ExceptionHandler(BoMGraphException::class)
    fun handleGraphException(ex: BoMGraphException): ResponseEntity<Map<String, String>> {
        val status = when (ex.code) {
            "GRAPH_NOT_FOUND", "GRAPH_VERSION_NOT_FOUND",
            "ENTITY_VERSION_NOT_FOUND", "EDGE_VERSION_NOT_FOUND",
            -> HttpStatus.NOT_FOUND
            else -> HttpStatus.BAD_REQUEST
        }
        return ResponseEntity.status(status).body(
            mapOf("error" to (ex.message ?: ex.code), "code" to ex.code),
        )
    }

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

    private fun BoMResolvedGraph.toResponse() = GraphResponse(
        id = id,
        annotations = annotations,
        graph = contents,
    )
}
