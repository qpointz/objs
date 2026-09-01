package org.poc.objs.jgrapht.service.web

import tools.jackson.databind.JsonNode

/**
 * Cycle analysis request using the same matcher DSL as graph query / Gremlin traverse.
 *
 * Example:
 * ```json
 * {
 *   "matcher": { "all": true },
 *   "materialization": "GENERIC"
 * }
 * ```
 */
data class GraphCycleAnalysisRequest(
    val matcher: JsonNode,
    val graphId: java.util.UUID? = null,
    val graphVersion: Long? = null,
    val algorithm: String? = null,
    val materialization: String? = null,
)
