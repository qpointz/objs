package org.poc.objs.jgrapht.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.GraphFragmentDiagnosticSeverity
import org.poc.objs.api.domain.GraphMaterializationException
import org.poc.objs.jgrapht.core.materialize.GenericJGraphTMaterializer
import org.poc.objs.jgrapht.core.testsupport.GraphFragmentFixtures
import java.util.UUID

class GenericJGraphTMaterializerTest {
    private val materializer = GenericJGraphTMaterializer()

    @Test
    fun shouldMaterializeDirectedPseudographWithParallelEdgesAndSelfLoops() {
        val a = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val b = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val first = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val second = UUID.fromString("00000000-0000-0000-0000-000000000102")
        val loop = UUID.fromString("00000000-0000-0000-0000-000000000103")
        val fragment = DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(Entity(a, "Component", "1"), Entity(b, "Component", "1")),
                edges = listOf(
                    Edge(first, source = a, target = b, role = "depends_on"),
                    Edge(second, source = a, target = b, role = "depends_on"),
                    Edge(loop, source = a, target = a, role = "depends_on"),
                ),
            ),
        )

        val materialized = materializer.materialize(fragment)

        assertThat(materialized.verticesByEntityId.keys).containsExactlyInAnyOrder(a, b)
        assertThat(materialized.edgesById.keys).containsExactlyInAnyOrder(first, second, loop)
        assertThat(materialized.graph.vertexSet()).hasSize(2)
        assertThat(materialized.graph.edgeSet()).hasSize(3)
    }

    @Test
    fun shouldRejectErrorBearingFragments() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val fragment = DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(
                    Entity(id, "Component", "1", mutableMapOf("name" to "a")),
                    Entity(id, "Component", "1", mutableMapOf("name" to "b")),
                ),
                edges = emptyList(),
            ),
        )

        val ex = org.junit.jupiter.api.assertThrows<GraphMaterializationException> {
            materializer.materialize(fragment)
        }
        assertThat(ex.diagnostics).anyMatch { it.severity == GraphFragmentDiagnosticSeverity.ERROR }
    }

    @Test
    fun shouldPreserveEntityAndEdgeIdentityFromFixtures() {
        val materialized = materializer.materialize(GraphFragmentFixtures.acyclic())
        assertThat(materialized.verticesByEntityId).hasSize(2)
        assertThat(materialized.edgesById).hasSize(1)
        assertThat(materialized.edgesById.values.single().role).isEqualTo("depends_on")
    }
}
