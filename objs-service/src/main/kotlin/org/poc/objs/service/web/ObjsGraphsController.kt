package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.GraphMutation
import org.poc.objs.api.domain.MutationMode
import org.poc.objs.api.domain.ResolvedGraph
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.graphMutation
import org.poc.objs.api.GraphOperationException
import org.poc.objs.api.domain.GraphException
import org.poc.objs.api.domain.GraphHeader
import org.poc.objs.api.domain.GraphListItem
import org.poc.objs.api.domain.GraphSpec
import org.poc.objs.api.match.MatcherDsl
import org.poc.objs.api.match.MatcherFormat
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.api.validation.ValidationException
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.api.validation.ValidationResult
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
class ObjsGraphsController(
    private val namedGraphs: NamedGraphStore,
    private val graphStore: GraphStore,
    private val matcherDsl: MatcherDsl = MatcherDsl.create(),
) {
    @Schema(description = "Graph header write body")
    data class GraphWriteBody(
        @field:Schema(description = "Caller-chosen graph id; omit to let the store assign one")
        val id: UUID? = null,
        @field:Schema(description = "Free-form graph annotations (string key/value), e.g. `name`, `env`")
        val annotations: Map<String, String> = emptyMap(),
        @field:Schema(description = "Existing pool entity ids to seed membership with (create only)")
        val entityIds: Set<UUID> = emptySet(),
    )

    @Schema(description = "Graph header response: id + annotations + resolved GraphContents")
    data class GraphResponse(
        @field:Schema(description = "Graph id")
        val id: UUID,
        @field:Schema(description = "Graph annotations")
        val annotations: Map<String, String>,
        @field:Schema(description = "Resolved members and graph-local edges")
        val graph: GraphContents,
    )

    @Schema(description = "Clone body: annotations for the new, independent graph")
    data class CloneBody(
        @field:Schema(description = "Annotations of the clone; source annotations are not copied")
        val annotations: Map<String, String> = emptyMap(),
    )

    @Schema(description = "Create deep graph version; optional createdAt for backdated freeze (G-O1 Option B)")
    data class CreateVersionBody(
        @field:Schema(description = "Annotations stored on the version row")
        val annotations: Map<String, String> = emptyMap(),
        @field:Schema(description = "Backdated freeze instant; omit for now()")
        val createdAt: java.time.Instant? = null,
    )

    @Schema(description = "Open-graph search envelope (G-U10); additive fields may appear later")
    data class GraphSearchResponse(
        @field:Schema(description = "Matching graph headers, best match first")
        val items: List<GraphHeader>,
    )

    @Schema(description = "Compaction outcome")
    data class CompactResult(
        @field:Schema(description = "Number of version rows removed")
        val removed: Int,
    )

    @GetMapping
    @Operation(
        tags = ["graphs"],
        summary = "List graph headers",
        description = "Whole live catalog with member/edge counts; use GET /graphs/search for large stores.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "All live graph headers",
            content = [
                Content(array = ArraySchema(schema = Schema(implementation = GraphListItem::class))),
            ],
        ),
    )
    fun list(): List<GraphListItem> = namedGraphs.list()

    @GetMapping("/search")
    @Operation(
        tags = ["graphs"],
        summary = "Search graph headers (open-graph dialog)",
        description = "G-U10 extensible search contract. Empty `q` without `expr` returns `{ items: [] }` " +
            "(never the full catalog). v1 match: id / UUID-prefix + case-insensitive substring on id and " +
            "annotation key/value; optional `expr` is a graph-expr (AND with `q` when both set). " +
            "Additive query params and response fields may be added later; FTS is out of scope for v1.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Search envelope (empty items when neither q nor expr is given)",
            content = [Content(schema = Schema(implementation = GraphSearchResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid graph-expr",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun search(
        @Parameter(description = "Free text: graph id, UUID prefix, or substring of an annotation key/value")
        @RequestParam(required = false) q: String?,
        @Parameter(description = "Graph-expr predicate over graph annotations; ANDed with `q` when both are set")
        @RequestParam(required = false) expr: String?,
        @Parameter(description = "Maximum number of headers returned")
        @RequestParam(required = false, defaultValue = "15") limit: Int,
    ): GraphSearchResponse = GraphSearchResponse(
        items = namedGraphs.search(q = q, expr = expr, limit = limit),
    )

    @PostMapping
    @Operation(
        tags = ["graphs"],
        summary = "Create a graph header, optionally seeding membership with existing pool entity ids",
        description = "Only attaches entities that already exist in the pool; it never creates them. " +
            "Use POST /entities first, or a graph mutate, to create new entities.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Created graph header with resolved contents",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed (e.g. unknown entity id)",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun create(@RequestBody body: GraphWriteBody): ResponseEntity<GraphResponse> {
        val created = namedGraphs.create(
            GraphSpec(id = body.id, annotations = body.annotations, entityIds = body.entityIds),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(created.toResponse())
    }

    @GetMapping("/{id}")
    @Operation(tags = ["graphs"], summary = "Get graph header + resolved members and graph-local edges")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun get(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
    ): ResponseEntity<GraphResponse> {
        val resolved = namedGraphs.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(resolved.toResponse())
    }

    @PutMapping("/{id}/annotations")
    @Operation(tags = ["graphs"], summary = "Replace graph header annotations (membership unchanged)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Updated graph",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun updateAnnotations(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @RequestBody body: GraphWriteBody,
    ): ResponseEntity<GraphResponse> {
        val updated = namedGraphs.updateAnnotations(id, body.annotations)
        return ResponseEntity.ok(updated.toResponse())
    }

    @PatchMapping("/{id}")
    @Operation(
        tags = ["mutations"],
        summary = "MERGE-mutate this graph (patch)",
        description = "MERGE: `entities.set` / `edges.set` upsert; `entities.unset` detaches membership " +
            "(pool kept); `edges.unset` drops this graph's edges. Omission never deletes. " +
            "Verb sets mode (omit `mode` on wire or it must be MERGE).",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph after mutation",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed, or body `mode` disagrees with the HTTP verb",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun mutateMerge(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @RequestBody mutation: GraphMutation,
    ): ResponseEntity<Any> = mutateWithMode(id, mutation, MutationMode.MERGE)

    @PutMapping("/{id}")
    @Operation(
        tags = ["mutations"],
        summary = "REPLACE-mutate this graph (overwrite contents)",
        description = "REPLACE: `entities.set` + `edges.set` are the full desired membership and " +
            "graph-local edges; unlisted members detach / edges drop. Non-empty `unset` is rejected. " +
            "Verb sets mode (omit `mode` on wire or it must be REPLACE).",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph after mutation",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed, or body `mode` disagrees with the HTTP verb",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun mutateReplace(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @RequestBody mutation: GraphMutation,
    ): ResponseEntity<Any> = mutateWithMode(id, mutation, MutationMode.REPLACE)

    @PatchMapping("/{id}/validate")
    @Operation(
        tags = ["mutations"],
        summary = "Dry-run MERGE validate (no persist)",
        description = "Same body and rules as PATCH /graphs/{id}; returns the ValidationResult instead of persisting.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Validation result (may be invalid)",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Body `mode` disagrees with the HTTP verb",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun validateMerge(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @RequestBody mutation: GraphMutation,
    ): ResponseEntity<Any> = validateWithMode(id, mutation, MutationMode.MERGE)

    @PutMapping("/{id}/validate")
    @Operation(
        tags = ["mutations"],
        summary = "Dry-run REPLACE validate (no persist)",
        description = "Same body and rules as PUT /graphs/{id}; returns the ValidationResult instead of persisting.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Validation result (may be invalid)",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Body `mode` disagrees with the HTTP verb",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun validateReplace(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @RequestBody mutation: GraphMutation,
    ): ResponseEntity<Any> = validateWithMode(id, mutation, MutationMode.REPLACE)

    @PostMapping("/{id}/validate")
    @Operation(
        tags = ["mutations"],
        summary = "Dry-run MERGE validate (no persist)",
        description = "Alias of PATCH /graphs/{id}/validate (MERGE). Prefer PATCH.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Validation result (may be invalid)",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun validatePost(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @RequestBody mutation: GraphMutation,
    ): ResponseEntity<Any> = validateWithMode(id, mutation, MutationMode.MERGE)

    private fun mutateWithMode(
        id: UUID,
        mutation: GraphMutation,
        mode: MutationMode,
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
        mutation: GraphMutation,
        mode: MutationMode,
    ): ResponseEntity<Any> {
        val resolved = resolveVerbMode(mutation, mode) ?: return modeMismatch(mode)
        return ResponseEntity.ok(namedGraphs.validateMutate(id, resolved))
    }

    /** Verb wins; body `mode` may be omitted (defaults MERGE) or must match the verb. */
    private fun resolveVerbMode(mutation: GraphMutation, verbMode: MutationMode): GraphMutation? {
        if (mutation.mode != MutationMode.MERGE && mutation.mode != verbMode) {
            return null
        }
        return mutation.copy(mode = verbMode)
    }

    private fun modeMismatch(verbMode: MutationMode): ResponseEntity<Any> =
        ResponseEntity.badRequest().body(
            ValidationResult.of(
                ValidationIssue(
                    code = "MUTATE_MODE_MISMATCH",
                    message = "Body mode disagrees with HTTP verb (expected $verbMode)",
                ),
            ),
        )

    @DeleteMapping("/{id}")
    @Operation(
        tags = ["graphs"],
        summary = "Softish delete: drop live graph header + membership + edges; history kept",
        description = "Pool entities kept. Deep version rows remain readable via GET …/versions. " +
            "Use DELETE …/destroy to wipe history too.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun delete(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        namedGraphs.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/clear")
    @Operation(
        tags = ["housekeeping"],
        summary = "Clear live HEAD membership and graph-local edges; keep header and history",
        description = "Equivalent to REPLACE mutate with empty sets (`clearGraph`).",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Cleared graph",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Clear rejected",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun clear(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val result = namedGraphs.clearGraph(id)
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.ok(requireNotNull(namedGraphs.get(id)).toResponse())
    }

    @DeleteMapping("/{id}/destroy")
    @Operation(
        tags = ["housekeeping"],
        summary = "Destroy graph: wipe live HEAD and all deep version history",
        description = "Fails with GRAPH_VERSION_IN_USE when app references (e.g. SBOM fingerprints) block removal.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Destroyed"),
        ApiResponse(responseCode = "404", description = "Graph not found"),
        ApiResponse(responseCode = "400", description = "In use or other graph operation failure"),
        ApiResponse(responseCode = "409", description = "GRAPH_VERSION_IN_USE / GRAPH_IN_USE"),
    )
    fun destroy(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        namedGraphs.destroyGraph(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/entities/{entityId}")
    @Operation(
        tags = ["entities"],
        summary = "Attach an existing pool entity to this graph (membership row only)",
        description = "Idempotent; the entity payload is untouched and stays shared with other graphs.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph after attach",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph or entity not found"),
    )
    fun attach(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Pool entity id to attach") @PathVariable entityId: UUID,
    ): ResponseEntity<GraphResponse> {
        namedGraphs.attach(id, entityId)
        return ResponseEntity.ok(requireNotNull(namedGraphs.get(id)).toResponse())
    }

    @DeleteMapping("/{id}/entities/{entityId}")
    @Operation(
        tags = ["entities"],
        summary = "Detach an entity from this graph (pool entity kept)",
        description = "Removes the membership row only; use DELETE /entities/{id} to drop the pool entity.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Detached"),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun detach(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Pool entity id to detach") @PathVariable entityId: UUID,
    ): ResponseEntity<Void> {
        namedGraphs.detach(id, entityId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/edges")
    @Operation(
        tags = ["edges"],
        summary = "Create/upsert an edge in this graph",
        description = "Thin MERGE wrapper: forces graphId from path; body = one mutate edges.set item.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph after create",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun createEdge(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @RequestBody edge: Edge,
    ): ResponseEntity<Any> {
        edge.graphId = id
        return mutateWithMode(
            id,
            graphMutation {
                mode(MutationMode.MERGE)
                edges { set(edge) }
            },
            MutationMode.MERGE,
        )
    }

    @PutMapping("/{id}/edges/{edgeId}")
    @Operation(
        tags = ["edges"],
        summary = "Update an edge in this graph",
        description = "Thin MERGE wrapper; path edgeId wins; rejects body graphId mismatch with path graph.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph after update",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed or graphId mismatch",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun updateEdge(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Edge id; overrides any `id` in the body") @PathVariable edgeId: UUID,
        @RequestBody edge: Edge,
    ): ResponseEntity<Any> {
        if (edge.graphId != null && edge.graphId != id) {
            return ResponseEntity.badRequest().body(
                ValidationResult.of(
                    ValidationIssue(
                        code = "GRAPH_ID_MISMATCH",
                        message = "Edge graphId ${edge.graphId} does not match path graph $id",
                    ),
                ),
            )
        }
        edge.id = edgeId
        edge.graphId = id
        return mutateWithMode(
            id,
            graphMutation {
                mode(MutationMode.MERGE)
                edges { set(edge) }
            },
            MutationMode.MERGE,
        )
    }

    @DeleteMapping("/{id}/edges/{edgeId}")
    @Operation(
        tags = ["edges"],
        summary = "Delete an edge from this graph",
        description = "Same as MERGE edges.unset for this edge id.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph after delete",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun deleteEdge(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Edge id") @PathVariable edgeId: UUID,
    ): ResponseEntity<Any> =
        mutateWithMode(
            id,
            graphMutation {
                mode(MutationMode.MERGE)
                edges { unset(edgeId) }
            },
            MutationMode.MERGE,
        )

    @PostMapping(
        "/{id}/query",
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            "application/yaml",
            "text/yaml",
            "application/x-yaml",
        ],
    )
    @Operation(
        tags = ["query"],
        summary = "Matcher DSL (obj-expr / chained) scoped to this graph's stored members",
        description = "Body is a matcher document in JSON or YAML (pick via Content-Type). Only this " +
            "graph's members are candidates; returned edges are induced graph-local edges.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Graph contents (selected members + induced edges)",
            content = [Content(schema = Schema(implementation = GraphContents::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher DSL or expression",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun queryInGraph(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
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
        tags = ["query"],
        summary = "Matcher DSL over graph headers (requires stage-0 all or graph-expr)",
        description = "Fails closed (400, MATCHER_GRAPH_SCOPE_REQUIRED) unless the matcher is (or starts " +
            "with) all / graph-expr — there is no whole-store-as-graph scan. `all: true` unions every " +
            "graph's members and edges (distinct by id).",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Union of matching graphs' stored members + graph-local edges",
            content = [Content(schema = Schema(implementation = GraphContents::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher DSL, or missing stage-0 all / graph-expr",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun queryGraphs(request: HttpServletRequest): ResponseEntity<Any> {
        val matcher = matcherDsl.decode(readBody(request), resolveFormat(request))
        return ResponseEntity.ok(graphStore.select(matcher))
    }

    @PostMapping("/{id}/clone")
    @Operation(
        tags = ["housekeeping"],
        summary = "Clone this graph's members + edges into a new, independent graph",
        description = "Membership is copied by reference (pool entities are shared); graph-local edges " +
            "are duplicated for the new graph. Version history is not cloned.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "New cloned graph",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(responseCode = "404", description = "Source graph not found"),
    )
    fun clone(
        @Parameter(description = "Source graph id") @PathVariable id: UUID,
        @RequestBody(required = false) body: CloneBody?,
    ): ResponseEntity<GraphResponse> {
        val cloned = namedGraphs.clone(id, body?.annotations ?: emptyMap())
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned.toResponse())
    }

    @PostMapping("/{id}/versions")
    @Operation(
        tags = ["versions"],
        summary = "Snapshot: pin current HEAD as a deep graph version (same graph id)",
        description = "Optional `createdAt` backdates version key + new freeze clocks (Option B). " +
            "Omitted → now. Live entity/edge HEAD clocks are not rewritten.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Created version summary",
            content = [
                Content(
                    schema = Schema(implementation = org.poc.objs.api.domain.GraphVersionSummary::class),
                ),
            ],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
        ApiResponse(responseCode = "400", description = "Snapshot rejected"),
    )
    fun createVersion(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @RequestBody(required = false) body: CreateVersionBody?,
    ): ResponseEntity<org.poc.objs.api.domain.GraphVersionSummary> {
        val created = namedGraphs.createDeepGraphVersion(
            id,
            body?.annotations ?: emptyMap(),
            body?.createdAt ?: java.time.Instant.now(),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/{id}/versions")
    @Operation(tags = ["versions"], summary = "List deep graph versions, newest first")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Version summaries",
            content = [
                Content(
                    array = ArraySchema(
                        schema = Schema(implementation = org.poc.objs.api.domain.GraphVersionSummary::class),
                    ),
                ),
            ],
        ),
        ApiResponse(responseCode = "404", description = "Graph not found"),
    )
    fun listVersions(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
    ) = namedGraphs.listGraphVersions(id)

    @DeleteMapping("/{id}/versions")
    @Operation(
        tags = ["housekeeping"],
        summary = "Purge all deep graph versions; null head_version; live HEAD intact",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Purged"),
        ApiResponse(responseCode = "404", description = "Graph not found"),
        ApiResponse(responseCode = "400", description = "In use or other failure"),
        ApiResponse(responseCode = "409", description = "A version is pinned by an application reference"),
    )
    fun purgeAllVersions(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        namedGraphs.purgeAllGraphVersions(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/versions/{version}")
    @Operation(
        tags = ["versions"],
        summary = "Reconstruct a deep graph version (read-only)",
        description = "Returns the frozen membership, edges, and payloads; live HEAD is not touched.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Reconstructed graph version",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph or version not found"),
    )
    fun getVersion(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Deep graph version number") @PathVariable version: Long,
    ): ResponseEntity<GraphResponse> {
        val resolved = namedGraphs.getGraphVersion(id, version)
        return ResponseEntity.ok(resolved.toResponse())
    }

    @DeleteMapping("/{id}/versions/{version}")
    @Operation(
        tags = ["versions"],
        summary = "Purge one deep graph version (pins + graph version row)",
        description = "Rejects current head_version (GRAPH_VERSION_IS_HEAD). Does not compact entity/edge orphans.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Purged"),
        ApiResponse(responseCode = "404", description = "Graph or version not found"),
        ApiResponse(responseCode = "400", description = "Head version or in-use"),
        ApiResponse(responseCode = "409", description = "GRAPH_VERSION_IS_HEAD / GRAPH_VERSION_IN_USE"),
    )
    fun purgeVersion(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Deep graph version number") @PathVariable version: Long,
    ): ResponseEntity<Void> {
        namedGraphs.purgeGraphVersion(id, version)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/versions/{version}/reset")
    @Operation(
        tags = ["versions"],
        summary = "Travel back: restore HEAD membership, edges, and payloads from a freeze",
        description = "Sets head_version to the target. Optional truncateAfter drops newer freezes. " +
            "Shared-pool rewrite of live entity/edge bytes is intentional.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph after reset",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Reset rejected",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph or version not found"),
    )
    fun resetToVersion(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Deep graph version to restore") @PathVariable version: Long,
        @Parameter(description = "Also purge every freeze newer than the target version")
        @RequestParam(defaultValue = "false") truncateAfter: Boolean,
    ): ResponseEntity<Any> {
        val result = namedGraphs.resetGraphToVersion(id, version, truncateAfter)
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.ok(requireNotNull(namedGraphs.get(id)).toResponse())
    }

    @PostMapping("/{id}/versions/{version}/apply-structure")
    @Operation(
        tags = ["versions"],
        summary = "Apply freeze entity/edge topology; keep live payloads",
        description = "Does not change head_version. Missing live entities/edges fall back to freeze rows.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Resolved graph after applying the frozen topology",
            content = [Content(schema = Schema(implementation = GraphResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Apply rejected",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph or version not found"),
    )
    fun applyStructure(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Deep graph version whose topology is applied") @PathVariable version: Long,
    ): ResponseEntity<Any> {
        val result = namedGraphs.applyGraphVersionStructure(id, version)
        if (!result.isValid) {
            return ResponseEntity.badRequest().body(result)
        }
        return ResponseEntity.ok(requireNotNull(namedGraphs.get(id)).toResponse())
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
    @Operation(
        tags = ["query"],
        summary = "Matcher DSL scoped to a reconstructed deep graph version",
        description = "Same matcher body as POST /graphs/{id}/query, evaluated against the freeze " +
            "rather than live HEAD.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Graph contents from the reconstructed version",
            content = [Content(schema = Schema(implementation = GraphContents::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher DSL or expression",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Graph or version not found"),
    )
    fun queryInGraphVersion(
        @Parameter(description = "Graph id") @PathVariable id: UUID,
        @Parameter(description = "Deep graph version number") @PathVariable version: Long,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        val matcher = matcherDsl.decode(readBody(request), resolveFormat(request))
        return ResponseEntity.ok(graphStore.selectInGraphVersion(id, version, matcher))
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ValidationResult> {
        val notFound = ex.result.issues.any { it.code == "GRAPH_NOT_FOUND" }
        val status = if (notFound) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(ex.result)
    }

    @ExceptionHandler(GraphOperationException::class, GraphException::class)
    fun handleGraphException(ex: GraphOperationException): ResponseEntity<Map<String, String>> {
        val status = when (ex.code) {
            "GRAPH_NOT_FOUND", "GRAPH_VERSION_NOT_FOUND",
            "ENTITY_VERSION_NOT_FOUND", "EDGE_VERSION_NOT_FOUND",
            -> HttpStatus.NOT_FOUND
            "GRAPH_VERSION_IN_USE", "GRAPH_VERSION_IS_HEAD", "GRAPH_IN_USE" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        return ResponseEntity.status(status).body(
            mapOf("error" to (ex.message ?: ex.code), "code" to ex.code),
        )
    }

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

    private fun ResolvedGraph.toResponse() = GraphResponse(
        id = id,
        annotations = annotations,
        graph = contents,
    )
}
