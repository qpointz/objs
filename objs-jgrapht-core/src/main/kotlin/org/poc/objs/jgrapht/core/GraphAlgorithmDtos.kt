package org.poc.objs.jgrapht.core

/** Stable algorithm identifiers exposed to REST and workbench consumers. */
object GraphAlgorithmIds {
    const val DIRECTED_CYCLE_REGIONS = "directed-cycle-regions"
}

/** Supported graph materialization modes. */
object GraphMaterializationModes {
    const val GENERIC = "GENERIC"
    const val TYPED = "TYPED"
}

data class GraphAlgorithmCapabilities(
    val algorithms: List<GraphAlgorithmCapability>,
)

data class GraphAlgorithmCapability(
    val id: String,
    val materializationModes: List<String>,
)

data class GraphCycleAnalysis(
    val algorithm: String,
    val components: List<GraphCycleComponent>,
    val stats: GraphAnalysisStats,
    val diagnostics: List<org.poc.objs.api.domain.GraphFragmentDiagnostic> = emptyList(),
)

data class GraphCycleComponent(
    val id: java.util.UUID,
    val entityIds: List<java.util.UUID>,
    val edgeIds: List<java.util.UUID>,
)

data class GraphAnalysisStats(
    val entityCount: Int,
    val edgeCount: Int,
    val cyclicComponentCount: Int,
)

object GraphAlgorithmCatalog {
    @JvmStatic
    fun capabilities(): GraphAlgorithmCapabilities =
        GraphAlgorithmCapabilities(
            algorithms = listOf(
                GraphAlgorithmCapability(
                    id = GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS,
                    materializationModes = listOf(GraphMaterializationModes.GENERIC),
                ),
            ),
        )
}
