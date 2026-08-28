package org.poc.objs.gremlin.service.web

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
data class GremlinTraverseRequest(
    val matcher: JsonNode,
    val script: String,
    val bindings: Map<String, Any?>? = null,
    val strategy: String? = null,
    val traversalOptions: GremlinTraversalOptions? = null,
    /** When set, select that graph (HEAD or [graphVersion] pin) via selectInGraph*, then eval. */
    val graphId: java.util.UUID? = null,
    val graphVersion: Long? = null,
)
