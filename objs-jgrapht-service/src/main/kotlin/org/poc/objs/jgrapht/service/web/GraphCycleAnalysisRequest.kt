package org.poc.objs.jgrapht.service.web

import io.swagger.v3.oas.annotations.media.Schema
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
@Schema(
    description = "Fragment selection for directed cycle analysis (same matcher DSL as graph query " +
        "and Gremlin traverse)",
)
data class GraphCycleAnalysisRequest(
    @field:Schema(description = "Matcher DSL document selecting the entities and edges to analyze")
    val matcher: JsonNode,
    @field:Schema(description = "Scope selection to this graph; the matcher then filters within it")
    val graphId: java.util.UUID? = null,
    @field:Schema(description = "Deep graph version pin; requires `graphId`")
    val graphVersion: Long? = null,
    @field:Schema(description = "Algorithm name from GET /graph/algorithms/capabilities; omit for the default")
    val algorithm: String? = null,
    @field:Schema(
        description = "Materialization mode from GET /graph/algorithms/capabilities; omit for the default",
        example = "GENERIC",
    )
    val materialization: String? = null,
)
