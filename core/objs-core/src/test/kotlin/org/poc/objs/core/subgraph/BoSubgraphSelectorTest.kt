package org.poc.objs.core.subgraph

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoEdge
import org.poc.objs.core.domain.BoEntity
import org.poc.objs.core.domain.BoGraph
import org.poc.objs.core.domain.UuidV7
import org.poc.objs.core.match.MatchAllAnnotationMatcher

class BoSubgraphSelectorTest {
    @Test
    fun shouldSelectMatchAllAndInducedEdges() {
        val a = UuidV7.generate()
        val b = UuidV7.generate()
        val c = UuidV7.generate()
        val graph = BoGraph(
            entities = mutableListOf(
                BoEntity(id = a, type = "T", version = "1", annotations = mutableMapOf("item" to "X", "src" to "ui")),
                BoEntity(id = b, type = "T", version = "1", annotations = mutableMapOf("item" to "X", "src" to "ui")),
                BoEntity(id = c, type = "T", version = "1", annotations = mutableMapOf("item" to "X", "src" to "auto")),
            ),
            edges = mutableListOf(
                BoEdge(source = a, target = b, role = "rel"),
                BoEdge(source = a, target = c, role = "rel"),
            ),
        )
        val sub = BoSubgraphSelector.select(graph, MatchAllAnnotationMatcher(mapOf("item" to "X", "src" to "ui")))
        assertThat(sub.entities).hasSize(2)
        assertThat(sub.edges).hasSize(1)
        assertThat(sub.edges[0].source).isEqualTo(a)
        assertThat(sub.edges[0].target).isEqualTo(b)
    }

    @Test
    fun shouldAllowExtraAnnotationsOnEntity() {
        val a = UuidV7.generate()
        val entity = BoEntity(
            id = a,
            type = "T",
            version = "1",
            annotations = mutableMapOf("item" to "X", "extra" to "1"),
        )
        val graph = BoGraph(mutableListOf(entity), mutableListOf())
        val sub = BoSubgraphSelector.selectMatchAll(graph, mapOf("item" to "X"))
        assertThat(sub.entities).hasSize(1)
    }

    @Test
    fun shouldExcludeEdgeWhenOnlyOneEndpointMatches() {
        val a = UuidV7.generate()
        val b = UuidV7.generate()
        val graph = BoGraph(
            entities = mutableListOf(
                BoEntity(id = a, type = "T", version = "1", annotations = mutableMapOf("k" to "v")),
                BoEntity(id = b, type = "T", version = "1", annotations = mutableMapOf("k" to "other")),
            ),
            edges = mutableListOf(BoEdge(source = a, target = b, role = "rel")),
        )
        val sub = BoSubgraphSelector.selectMatchAll(graph, mapOf("k" to "v"))
        assertThat(sub.entities).hasSize(1)
        assertThat(sub.edges).isEmpty()
    }
}
