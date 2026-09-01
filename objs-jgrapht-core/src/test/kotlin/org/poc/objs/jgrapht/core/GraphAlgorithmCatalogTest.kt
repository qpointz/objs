package org.poc.objs.jgrapht.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GraphAlgorithmCatalogTest {
    @Test
    fun shouldExposeDirectedCycleRegionsCapability() {
        val capabilities = GraphAlgorithmCatalog.capabilities()
        assertThat(capabilities.algorithms).hasSize(1)
        assertThat(capabilities.algorithms.single().id).isEqualTo(GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS)
        assertThat(capabilities.algorithms.single().materializationModes).containsExactly("GENERIC")
    }
}
