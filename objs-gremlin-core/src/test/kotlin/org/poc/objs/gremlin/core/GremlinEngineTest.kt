package org.poc.objs.gremlin.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import java.util.UUID

class GremlinEngineTest {

    private val engine = GremlinEngine()

    private val a = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val b = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val e = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")

    private fun sampleSubgraph() =
        DefaultGraphFragmentPolicy.resolve(sampleContents())

    private fun sampleContents(): GraphContents =
        GraphContents(
            entities = listOf(
                Entity(
                    id = a,
                    type = "Component",
                    schemaVersion = "1.0.0",
                    payload = mutableMapOf("name" to "lib"),
                    annotations = mutableMapOf("env" to "test"),
                ),
                Entity(
                    id = b,
                    type = "Component",
                    schemaVersion = "1.0.0",
                    payload = mutableMapOf("name" to "app"),
                ),
            ),
            edges = listOf(
                Edge(
                    id = e,
                    source = a,
                    target = b,
                    role = "DEPENDS_ON",
                ),
            ),
        )

    @Test
    fun shouldReturnGraphSubgraph_whenVerticesSelected() {
        val result = engine.eval(sampleSubgraph(), "g.V().hasLabel('Component')")
        assertThat(result.primary).isEqualTo("graph")
        assertThat(result.contents).isNotNull
        assertThat(result.contents!!.entities).hasSize(2)
        assertThat(result.contents!!.edges).hasSize(1)
        assertThat(result.contents!!.edges[0].id).isEqualTo(e)
        assertThat(result.views.graph).isEqualTo(result.contents)
        assertThat(result.meta.language).isEqualTo("gremlin-lang")
        assertThat(result.meta.strategy).isEqualTo("envelope")
    }

    @Test
    fun shouldReturnScalar_whenCount() {
        val result = engine.eval(sampleSubgraph(), "g.V().count()")
        assertThat(result.primary).isEqualTo("scalar")
        assertThat(result.contents).isNull()
        assertThat(result.views.scalar).isEqualTo(2L)
    }

    @Test
    fun shouldFail_whenUnknownLanguage() {
        assertThatThrownBy {
            engine.eval(
                sampleSubgraph(),
                "g.V()",
                options = GremlinTraversalOptions(language = "gremlin-groovy"),
            )
        }.isInstanceOf(GremlinEvalException::class.java)
            .hasMessageContaining("Unsupported language")
    }

    @Test
    fun shouldFail_whenInvalidScript() {
        assertThatThrownBy {
            engine.eval(sampleSubgraph(), "g.V().notARealStep()")
        }.isInstanceOf(GremlinEvalException::class.java)
    }

    @Test
    fun shouldFail_whenTimeoutNonPositive() {
        assertThatThrownBy {
            engine.eval(
                sampleSubgraph(),
                "g.V()",
                options = GremlinTraversalOptions(timeoutSeconds = 0),
            )
        }.isInstanceOf(GremlinEvalException::class.java)
            .hasMessageContaining("timeoutSeconds")
    }

    @Test
    fun shouldReturnGraph_whenGV() {
        val result = engine.eval(sampleSubgraph(), "g.V()")
        assertThat(result.primary).isEqualTo("graph")
        assertThat(result.items).hasSize(2)
        assertThat(result.contents!!.entities).hasSize(2)
        assertThat(result.meta.subgraph1Stats.entities).isEqualTo(2)
        assertThat(result.meta.resultCount).isEqualTo(2)
    }

    @Test
    fun shouldNotReuseClosedGraph_whenSameScriptRepeated() {
        val first = engine.eval(sampleSubgraph(), "g.V()")
        assertThat(first.items).hasSize(2)
        val empty = engine.eval(
            DefaultGraphFragmentPolicy.resolve(GraphContents(entities = emptyList(), edges = emptyList())),
            "g.V()",
        )
        assertThat(empty.items).isEmpty()
        val again = engine.eval(sampleSubgraph(), "g.V()")
        assertThat(again.items).hasSize(2)
        assertThat(again.contents!!.entities).hasSize(2)
    }

    @Test
    fun shouldInduceEdges_whenVertexOnlyHits() {
        val result = engine.eval(sampleSubgraph(), "g.V().hasLabel('Component')")
        assertThat(result.contents!!.edges.map { it.id }).containsExactly(e)
    }
}
