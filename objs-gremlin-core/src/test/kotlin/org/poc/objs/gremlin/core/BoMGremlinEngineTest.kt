package org.poc.objs.gremlin.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMSubgraph
import java.util.UUID

class BoMGremlinEngineTest {

    private val engine = BoMGremlinEngine()

    private val a = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val b = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val e = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")

    private fun sampleSubgraph(): BoMSubgraph =
        BoMSubgraph(
            entities = listOf(
                BoMEntity(
                    id = a,
                    type = "Component",
                    schemaVersion = "1.0.0",
                    payload = mutableMapOf("name" to "lib"),
                    annotations = mutableMapOf("env" to "test"),
                ),
                BoMEntity(
                    id = b,
                    type = "Component",
                    schemaVersion = "1.0.0",
                    payload = mutableMapOf("name" to "app"),
                ),
            ),
            edges = listOf(
                BoMEdge(
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
        assertThat(result.subgraph).isNotNull
        assertThat(result.subgraph!!.entities).hasSize(2)
        assertThat(result.subgraph!!.edges).hasSize(1)
        assertThat(result.subgraph!!.edges[0].id).isEqualTo(e)
        assertThat(result.views.graph).isEqualTo(result.subgraph)
        assertThat(result.meta.language).isEqualTo("gremlin-lang")
        assertThat(result.meta.strategy).isEqualTo("envelope")
    }

    @Test
    fun shouldReturnScalar_whenCount() {
        val result = engine.eval(sampleSubgraph(), "g.V().count()")
        assertThat(result.primary).isEqualTo("scalar")
        assertThat(result.subgraph).isNull()
        assertThat(result.views.scalar).isEqualTo(2L)
    }

    @Test
    fun shouldFail_whenUnknownLanguage() {
        assertThatThrownBy {
            engine.eval(
                sampleSubgraph(),
                "g.V()",
                options = BoMGremlinTraversalOptions(language = "gremlin-groovy"),
            )
        }.isInstanceOf(BoMGremlinEvalException::class.java)
            .hasMessageContaining("Unsupported language")
    }

    @Test
    fun shouldFail_whenInvalidScript() {
        assertThatThrownBy {
            engine.eval(sampleSubgraph(), "g.V().notARealStep()")
        }.isInstanceOf(BoMGremlinEvalException::class.java)
    }

    @Test
    fun shouldFail_whenTimeoutNonPositive() {
        assertThatThrownBy {
            engine.eval(
                sampleSubgraph(),
                "g.V()",
                options = BoMGremlinTraversalOptions(timeoutSeconds = 0),
            )
        }.isInstanceOf(BoMGremlinEvalException::class.java)
            .hasMessageContaining("timeoutSeconds")
    }

    @Test
    fun shouldInduceEdges_whenVertexOnlyHits() {
        val result = engine.eval(sampleSubgraph(), "g.V().hasLabel('Component')")
        assertThat(result.subgraph!!.edges.map { it.id }).containsExactly(e)
    }
}
