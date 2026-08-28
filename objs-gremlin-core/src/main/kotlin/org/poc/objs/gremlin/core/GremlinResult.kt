package org.poc.objs.gremlin.core

import org.poc.objs.api.domain.GraphContents

/**
 * Projected Gremlin evaluation result. Never exposes TinkerPop types.
 */
data class GremlinResult(
    val primary: String,
    val items: List<GremlinItem>,
    val contents: GraphContents?,
    val views: GremlinViews,
    val meta: GremlinMeta,
) {
    fun contentsOrNull(): GraphContents? = contents
}

data class GremlinViews(
    val graph: GraphContents? = null,
    val table: GremlinTable? = null,
    val scalar: Any? = null,
)

data class GremlinTable(
    val columns: List<String>,
    val rows: List<List<Any?>>,
)

data class GremlinMeta(
    val strategy: String,
    val language: String,
    val subgraph1Stats: GremlinGraphStats,
    val subgraph2Stats: GremlinGraphStats?,
    val resultCount: Int,
    val durationMs: Long,
)

data class GremlinGraphStats(
    val entities: Int,
    val edges: Int,
)

sealed class GremlinItem {
    abstract val kind: String

    data class Vertex(val value: Map<String, Any?>) : GremlinItem() {
        override val kind: String = "vertex"
    }

    data class Edge(val value: Map<String, Any?>) : GremlinItem() {
        override val kind: String = "edge"
    }

    data class Path(val value: GremlinPathValue) : GremlinItem() {
        override val kind: String = "path"
    }

    data class MapValue(val value: Map<String, Any?>) : GremlinItem() {
        override val kind: String = "map"
    }

    data class Scalar(val value: Any?) : GremlinItem() {
        override val kind: String = "scalar"
    }

    data class ListValue(val value: List<GremlinItem>) : GremlinItem() {
        override val kind: String = "list"
    }
}

data class GremlinPathValue(
    val labels: List<Set<String>>,
    val objects: List<GremlinItem>,
)

class GremlinEvalException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
