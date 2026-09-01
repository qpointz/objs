package org.poc.objs.jgrapht.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.gremlin.core.materialize.EnvelopeMaterializationStrategy
import org.poc.objs.gremlin.core.materialize.GremlinMaterializer
import org.poc.objs.jgrapht.core.materialize.GenericJGraphTMaterializer
import org.poc.objs.jgrapht.core.testsupport.GraphFragmentFixtures

class EngineTopologyParityTest {
    private val jgrapht = GenericJGraphTMaterializer()
    private val gremlin = GremlinMaterializer()

    @Test
    fun shouldProduceEquivalentTopologyForSharedFixtures() {
        listOf(
            GraphFragmentFixtures.acyclic(),
            GraphFragmentFixtures.twoNodeCycle(),
            GraphFragmentFixtures.selfLoop(),
        ).forEach { fragment ->
            val jGraph = jgrapht.materialize(fragment)
            val tinker = gremlin.materialize(fragment)
            try {
                val jVertices = jGraph.verticesByEntityId.keys.sorted()
                val gVertices = tinker.vertices().asSequence().map { it.id() as java.util.UUID }.sorted().toList()
                assertThat(gVertices).containsExactlyElementsOf(jVertices)

                val jEdges = jGraph.fragment.edges.map { Triple(it.id!!, it.source, it.target) }.sortedBy { it.first.toString() }
                val gEdges = tinker.edges().asSequence().map {
                    Triple(it.id() as java.util.UUID, it.outVertex().id() as java.util.UUID, it.inVertex().id() as java.util.UUID)
                }.sortedBy { it.first.toString() }.toList()
                assertThat(gEdges).containsExactlyElementsOf(jEdges)
            } finally {
                tinker.close()
            }
        }
    }
}
