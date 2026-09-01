package org.poc.objs.jgrapht.core.analysis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.jgrapht.core.GraphAlgorithmIds
import org.poc.objs.jgrapht.core.UuidOrdering
import org.poc.objs.jgrapht.core.testsupport.GraphFragmentFixtures
import java.util.UUID

class DirectedCycleRegionAnalyzerTest {
    private val analyzer = DirectedCycleRegionAnalyzer()

    @Test
    fun shouldReturnEmptyComponentsForAcyclicGraph() {
        val analysis = analyzer.analyze(GraphFragmentFixtures.acyclic())
        assertThat(analysis.algorithm).isEqualTo(GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS)
        assertThat(analysis.components).isEmpty()
        assertThat(analysis.stats.cyclicComponentCount).isZero()
        assertThat(analysis.stats.entityCount).isEqualTo(2)
    }

    @Test
    fun shouldDetectTwoNodeCycleWithSmallestEntityIdAsComponentId() {
        val analysis = analyzer.analyze(GraphFragmentFixtures.twoNodeCycle())
        assertThat(analysis.components).hasSize(1)
        val component = analysis.components.single()
        assertThat(component.id).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        assertThat(component.entityIds).containsExactly(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
        )
        assertThat(component.edgeIds).containsExactly(
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            UUID.fromString("00000000-0000-0000-0000-000000000102"),
        )
    }

    @Test
    fun shouldDetectSingletonSelfLoop() {
        val analysis = analyzer.analyze(GraphFragmentFixtures.selfLoop())
        assertThat(analysis.components).hasSize(1)
        val component = analysis.components.single()
        assertThat(component.entityIds).containsExactly(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
        )
        assertThat(component.edgeIds).containsExactly(
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
        )
    }

    @Test
    fun shouldOrderComponentsByUnsignedUuid() {
        val low = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val high = UUID.fromString("80000000-0000-0000-0000-000000000001")
        val fragment = DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(
                    Entity(low, "Component", "1"),
                    Entity(high, "Component", "1"),
                ),
                edges = listOf(
                    Edge(UUID.randomUUID(), source = low, target = low, role = "loop"),
                    Edge(UUID.randomUUID(), source = high, target = high, role = "loop"),
                ),
            ),
        )

        val analysis = analyzer.analyze(fragment)

        assertThat(analysis.components.map { it.id }).isEqualTo(
            UuidOrdering.sorted(listOf(low, high)),
        )
    }
}
