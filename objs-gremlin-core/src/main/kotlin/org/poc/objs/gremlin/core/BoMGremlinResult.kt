package org.poc.objs.gremlin.core

import org.poc.objs.core.domain.BoMGraphContents

/**
 * Projected Gremlin evaluation result. Never exposes TinkerPop types.
 */
data class BoMGremlinResult(
    val primary: String,
    val items: List<BoMGremlinItem>,
    val contents: BoMGraphContents?,
    val views: BoMGremlinViews,
    val meta: BoMGremlinMeta,
) {
    fun contentsOrNull(): BoMGraphContents? = contents
}

data class BoMGremlinViews(
    val graph: BoMGraphContents? = null,
    val table: BoMGremlinTable? = null,
    val scalar: Any? = null,
)

data class BoMGremlinTable(
    val columns: List<String>,
    val rows: List<List<Any?>>,
)

data class BoMGremlinMeta(
    val strategy: String,
    val language: String,
    val subgraph1Stats: BoMGremlinGraphStats,
    val subgraph2Stats: BoMGremlinGraphStats?,
    val resultCount: Int,
    val durationMs: Long,
)

data class BoMGremlinGraphStats(
    val entities: Int,
    val edges: Int,
)

sealed class BoMGremlinItem {
    abstract val kind: String

    data class Vertex(val value: Map<String, Any?>) : BoMGremlinItem() {
        override val kind: String = "vertex"
    }

    data class Edge(val value: Map<String, Any?>) : BoMGremlinItem() {
        override val kind: String = "edge"
    }

    data class Path(val value: BoMGremlinPathValue) : BoMGremlinItem() {
        override val kind: String = "path"
    }

    data class MapValue(val value: Map<String, Any?>) : BoMGremlinItem() {
        override val kind: String = "map"
    }

    data class Scalar(val value: Any?) : BoMGremlinItem() {
        override val kind: String = "scalar"
    }

    data class ListValue(val value: List<BoMGremlinItem>) : BoMGremlinItem() {
        override val kind: String = "list"
    }
}

data class BoMGremlinPathValue(
    val labels: List<Set<String>>,
    val objects: List<BoMGremlinItem>,
)

class BoMGremlinEvalException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
