package org.poc.objs.gremlin.service.web

import org.poc.objs.gremlin.core.BoMGremlinTraversalOptions
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
data class BoMGremlinTraverseRequest(
    val matcher: JsonNode,
    val script: String,
    val bindings: Map<String, Any?>? = null,
    val strategy: String? = null,
    val traversalOptions: BoMGremlinTraversalOptions? = null,
)
