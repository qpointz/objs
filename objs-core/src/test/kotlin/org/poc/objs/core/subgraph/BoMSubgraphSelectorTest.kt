package org.poc.objs.core.subgraph

import java.util.UUID

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.match.MatchAllAnnotationMatcher

class BoMSubgraphSelectorTest {
    @Test
    fun shouldSelectMatchAllAndInducedEdges() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(id = a, type = "T", schemaVersion = "1", annotations = mutableMapOf("item" to "X", "src" to "ui")),
                BoMEntity(id = b, type = "T", schemaVersion = "1", annotations = mutableMapOf("item" to "X", "src" to "ui")),
                BoMEntity(id = c, type = "T", schemaVersion = "1", annotations = mutableMapOf("item" to "X", "src" to "auto")),
            ),
            edges = mutableListOf(
                BoMEdge(source = a, target = b, role = "rel"),
                BoMEdge(source = a, target = c, role = "rel"),
            ),
        )
        val sub = BoMSubgraphSelector.select(graph, MatchAllAnnotationMatcher(mapOf("item" to "X", "src" to "ui")))
        assertThat(sub.entities).hasSize(2)
        assertThat(sub.edges).hasSize(1)
        assertThat(sub.edges[0].source).isEqualTo(a)
        assertThat(sub.edges[0].target).isEqualTo(b)
    }

    @Test
    fun shouldAllowExtraAnnotationsOnEntity() {
        val a = UUID.randomUUID()
        val entity = BoMEntity(
            id = a,
            type = "T",
            schemaVersion = "1",
            annotations = mutableMapOf("item" to "X", "extra" to "1"),
        )
        val graph = BoMGraph(mutableListOf(entity), mutableListOf())
        val sub = BoMSubgraphSelector.selectMatchAll(graph, mapOf("item" to "X"))
        assertThat(sub.entities).hasSize(1)
    }

    @Test
    fun shouldApplyChainedAnnoThenAnnoExprAsFilters() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(id = a, type = "T", schemaVersion = "1", annotations = mutableMapOf("app" to "x", "env" to "prod")),
                BoMEntity(id = b, type = "T", schemaVersion = "1", annotations = mutableMapOf("app" to "x", "env" to "dev")),
            ),
            edges = mutableListOf(),
        )
        val dsl = org.poc.objs.core.match.BoMMatcherDsl.create()
        val matcher = dsl.decode(
            """
            - anno:
                app: x
            - anno-expr: "env == 'prod'"
            """.trimIndent(),
            org.poc.objs.core.match.BoMMatcherFormat.YAML,
        )
        val sub = BoMSubgraphSelector.select(graph, matcher)
        assertThat(sub.entities.map { it.id }).containsExactly(a)
    }

    @Test
    fun shouldExcludeEdgeWhenOnlyOneEndpointMatches() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(id = a, type = "T", schemaVersion = "1", annotations = mutableMapOf("k" to "v")),
                BoMEntity(id = b, type = "T", schemaVersion = "1", annotations = mutableMapOf("k" to "other")),
            ),
            edges = mutableListOf(BoMEdge(source = a, target = b, role = "rel")),
        )
        val sub = BoMSubgraphSelector.selectMatchAll(graph, mapOf("k" to "v"))
        assertThat(sub.entities).hasSize(1)
        assertThat(sub.edges).isEmpty()
    }
}
