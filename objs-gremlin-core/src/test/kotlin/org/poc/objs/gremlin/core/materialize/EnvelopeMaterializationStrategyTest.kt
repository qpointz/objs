package org.poc.objs.gremlin.core.materialize

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import java.util.UUID

class EnvelopeMaterializationStrategyTest {

    private val materializer = GremlinMaterializer()

    @Test
    fun shouldMaterializeVerticesAndEdges_whenEnvelope() {
        val a = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val b = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val e = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val subgraph = DefaultGraphFragmentPolicy.resolve(
            GraphContents(
            entities = listOf(
                Entity(
                    id = a,
                    type = "Component",
                    schemaVersion = "1.0.0",
                    payload = mutableMapOf(
                        "name" to "lib",
                        "meta" to mutableMapOf("nested" to true, "tags" to listOf("x", "y")),
                    ),
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
                    type = "Dep",
                    schemaVersion = "1.0.0",
                    properties = mutableMapOf("scope" to "runtime"),
                ),
            ),
        ),
        )

        val graph = materializer.materialize(subgraph)
        assertThat(graph.vertices().asSequence().count()).isEqualTo(2)
        assertThat(graph.edges().asSequence().count()).isEqualTo(1)

        val va = graph.vertices(a).next()
        assertThat(va.label()).isEqualTo("Component")
        assertThat(va.value<String>(EnvelopeMaterializationStrategy.PROP_SCHEMA_VERSION)).isEqualTo("1.0.0")
        @Suppress("UNCHECKED_CAST")
        val payload = va.value<Map<String, Any?>>(EnvelopeMaterializationStrategy.PROP_PAYLOAD)
        assertThat(payload["name"]).isEqualTo("lib")
        @Suppress("UNCHECKED_CAST")
        val meta = payload["meta"] as Map<String, Any?>
        assertThat(meta["nested"]).isEqualTo(true)
        assertThat(meta["tags"]).isEqualTo(listOf("x", "y"))
        assertThat(va.value<Map<String, String>>(EnvelopeMaterializationStrategy.PROP_ANNOTATIONS))
            .containsEntry("env", "test")

        val edge = graph.edges(e).next()
        assertThat(edge.label()).isEqualTo("DEPENDS_ON")
        assertThat(edge.outVertex().id()).isEqualTo(a)
        assertThat(edge.inVertex().id()).isEqualTo(b)
        assertThat(edge.value<String>(EnvelopeMaterializationStrategy.PROP_TYPE)).isEqualTo("Dep")
        assertThat(edge.value<Map<String, Any?>>(EnvelopeMaterializationStrategy.PROP_PROPERTIES))
            .containsEntry("scope", "runtime")
    }

    @Test
    fun shouldDefaultToEnvelope_whenStrategyOmitted() {
        val id = UUID.randomUUID()
        val graph = materializer.materialize(
            DefaultGraphFragmentPolicy.resolve(
                GraphContents(
                    entities = listOf(Entity(id = id, type = "X", schemaVersion = "1")),
                    edges = emptyList(),
                ),
            ),
        )
        assertThat(graph.vertices(id).hasNext()).isTrue()
    }

    @Test
    fun shouldFail_whenUnknownStrategy() {
        assertThatThrownBy {
            materializer.materialize(
                DefaultGraphFragmentPolicy.resolve(
                    GraphContents(entities = emptyList(), edges = emptyList()),
                ),
                strategy = "flatten",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown materialization strategy")
    }

    @Test
    fun shouldFail_whenEntityIdNull() {
        assertThatThrownBy {
            materializer.materialize(
                DefaultGraphFragmentPolicy.resolve(
                    GraphContents(
                        entities = listOf(Entity(type = "X", schemaVersion = "1")),
                        edges = emptyList(),
                    ),
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("null id")
    }
}
