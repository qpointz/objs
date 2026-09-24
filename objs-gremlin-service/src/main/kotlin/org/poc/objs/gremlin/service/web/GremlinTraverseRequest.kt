package org.poc.objs.gremlin.service.web

import io.swagger.v3.oas.annotations.media.Schema
import org.poc.objs.gremlin.core.GremlinTraversalOptions
import tools.jackson.databind.JsonNode

/**
 * Traverse request: same matcher DSL as Explorer `POST /graph/query`, plus gremlin-lang script.
 *
 * Example:
 * ```json
 * {
 *   "matcher": { "anno": { "env": "test" } },
 *   "script": "g.V().hasLabel('Component')",
 *   "traversalOptions": { "timeoutSeconds": 60 }
 * }
 * ```
 */
@Schema(
    description = "Matcher-selected subgraph plus a gremlin-lang script to evaluate over it " +
        "(same matcher DSL as POST /graphs/query)",
)
data class GremlinTraverseRequest(
    @field:Schema(
        description = "Matcher DSL document selecting the subgraph. Without `graphId` it must start " +
            "with all / graph-expr / graphs-in; with `graphId` it acts as an in-graph filter.",
    )
    val matcher: JsonNode,
    @field:Schema(
        description = "gremlin-lang script evaluated against the materialized subgraph; must not be blank",
        example = "g.V().hasLabel('Component')",
    )
    val script: String,
    @field:Schema(description = "Named bindings made available to the script")
    val bindings: Map<String, Any?>? = null,
    @field:Schema(description = "Materialization strategy name; defaults to the envelope strategy")
    val strategy: String? = null,
    @field:Schema(description = "Traversal limits such as timeoutSeconds")
    val traversalOptions: GremlinTraversalOptions? = null,
    /** When set, select that graph (HEAD or [graphVersion] pin) via selectInGraph*, then eval. */
    @field:Schema(description = "Scope selection to this graph; the matcher then filters within it")
    val graphId: java.util.UUID? = null,
    @field:Schema(description = "Deep graph version pin; requires `graphId`")
    val graphVersion: Long? = null,
)
