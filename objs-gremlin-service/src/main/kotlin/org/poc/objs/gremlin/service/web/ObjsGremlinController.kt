package org.poc.objs.gremlin.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.GraphException
import org.poc.objs.core.match.MatcherDsl
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.validation.ValidationException
import org.poc.objs.core.validation.ValidationResult
import org.poc.objs.gremlin.core.GremlinEngine
import org.poc.objs.gremlin.core.GremlinEvalException
import org.poc.objs.gremlin.core.GremlinItem
import org.poc.objs.gremlin.core.GremlinResult
import org.poc.objs.gremlin.core.materialize.EnvelopeMaterializationStrategy
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Matcher → subgraph1 (Explorer parity) → gremlin-lang traversal → projected result.
 */
@RestController
@RequestMapping("/api/v1/objs")
@Tag(name = "traverse")
class ObjsGremlinController(
    private val store: GraphStore,
    private val engine: GremlinEngine,
    private val matcherDsl: MatcherDsl = MatcherDsl.create(),
) {
    @PostMapping("/graph/traverse/gremlin")
    @Operation(
        summary = "Select subgraph by matcher DSL, then evaluate gremlin-lang",
        description = "Body requires `matcher` (same DSL as POST /graphs/query) and `script`. " +
            "When `graphId` is set, selects that graph via selectInGraph (optional `graphVersion` pin) " +
            "and uses matcher as an in-graph filter (obj-expr true = all members). " +
            "Without graphId, matcher must start with all / graph-expr / graphs-in.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Projected Gremlin result",
            content = [Content(schema = Schema(implementation = GremlinResult::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher, script, options, or evaluation failure",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun traverse(@RequestBody body: GremlinTraverseRequest): ResponseEntity<Any> {
        if (body.script.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "script must not be blank"))
        }
        return try {
            val matcher = matcherDsl.decodeNode(body.matcher, "$.matcher")
            val result =
                if (body.graphId != null) {
                    val subgraph =
                        if (body.graphVersion != null) {
                            store.selectInGraphVersion(body.graphId, body.graphVersion, matcher)
                        } else {
                            store.selectInGraph(body.graphId, matcher)
                        }
                    engine.eval(
                        subgraph = subgraph,
                        script = body.script,
                        bindings = body.bindings,
                        strategy = body.strategy ?: EnvelopeMaterializationStrategy.NAME,
                        options = body.traversalOptions,
                    )
                } else {
                    engine.selectAndEval(
                        store = store,
                        matcher = matcher,
                        script = body.script,
                        bindings = body.bindings,
                        strategy = body.strategy ?: EnvelopeMaterializationStrategy.NAME,
                        options = body.traversalOptions,
                    )
                }
            ResponseEntity.ok(result.toApiMap())
        } catch (ex: ValidationException) {
            ResponseEntity.badRequest().body(ex.result)
        } catch (ex: GraphException) {
            val status =
                if (ex.code == "GRAPH_VERSION_NOT_FOUND" || ex.code == "GRAPH_NOT_FOUND") {
                    HttpStatus.NOT_FOUND
                } else {
                    HttpStatus.BAD_REQUEST
                }
            ResponseEntity.status(status).body(mapOf("error" to (ex.message ?: ex.code), "code" to ex.code))
        } catch (ex: GremlinEvalException) {
            ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: "evaluation failed")))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: "bad request")))
        }
    }
}

/** Jackson-friendly map shape matching STORY envelope (kind-tagged items). */
internal fun GremlinResult.toApiMap(): Map<String, Any?> =
    linkedMapOf(
        "primary" to primary,
        "items" to items.map { it.toApiMap() },
        "contents" to contents,
        "views" to linkedMapOf(
            "graph" to views.graph,
            "table" to views.table?.let {
                linkedMapOf("columns" to it.columns, "rows" to it.rows)
            },
            "scalar" to views.scalar,
        ),
        "meta" to linkedMapOf(
            "strategy" to meta.strategy,
            "language" to meta.language,
            "subgraph1Stats" to linkedMapOf(
                "entities" to meta.subgraph1Stats.entities,
                "edges" to meta.subgraph1Stats.edges,
            ),
            "subgraph2Stats" to meta.subgraph2Stats?.let {
                linkedMapOf("entities" to it.entities, "edges" to it.edges)
            },
            "resultCount" to meta.resultCount,
            "durationMs" to meta.durationMs,
        ),
    )

private fun GremlinItem.toApiMap(): Map<String, Any?> =
    when (this) {
        is GremlinItem.Vertex -> linkedMapOf("kind" to kind, "value" to value)
        is GremlinItem.Edge -> linkedMapOf("kind" to kind, "value" to value)
        is GremlinItem.Path -> linkedMapOf(
            "kind" to kind,
            "value" to linkedMapOf(
                "labels" to value.labels,
                "objects" to value.objects.map { it.toApiMap() },
            ),
        )
        is GremlinItem.MapValue -> linkedMapOf("kind" to kind, "value" to value)
        is GremlinItem.Scalar -> linkedMapOf("kind" to kind, "value" to value)
        is GremlinItem.ListValue -> linkedMapOf(
            "kind" to kind,
            "value" to value.map { it.toApiMap() },
        )
    }
