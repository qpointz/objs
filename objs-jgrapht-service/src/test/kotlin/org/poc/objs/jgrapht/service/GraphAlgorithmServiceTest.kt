package org.poc.objs.jgrapht.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.jgrapht.core.GraphAlgorithmIds
import org.poc.objs.jgrapht.core.GraphMaterializationModes

class GraphAlgorithmServiceTest {
    @Test
    fun shouldAdvertiseTypedModeWhenProviderInstalled() {
        val service = GraphAlgorithmService(
            store = org.mockito.Mockito.mock(org.poc.objs.api.store.GraphStore::class.java),
            policy = org.poc.objs.api.domain.DefaultGraphFragmentPolicy,
            cycleAnalyzer = org.poc.objs.jgrapht.core.analysis.DirectedCycleRegionAnalyzer(),
            typedProviders = listOf(JGraphTTypedAnalysisProvider { fragment ->
                org.poc.objs.jgrapht.core.analysis.DirectedCycleRegionAnalyzer().analyze(fragment)
            }),
        )

        val capabilities = service.capabilities()
        assertThat(capabilities.algorithms.single().id).isEqualTo(GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS)
        assertThat(capabilities.algorithms.single().materializationModes)
            .containsExactly(GraphMaterializationModes.GENERIC, GraphMaterializationModes.TYPED)
    }
}
