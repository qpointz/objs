package org.poc.objs.jgrapht.core.analysis

import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector
import org.poc.objs.api.domain.ResolvedGraphFragment
import org.poc.objs.jgrapht.core.GraphAlgorithmIds
import org.poc.objs.jgrapht.core.GraphAnalysisStats
import org.poc.objs.jgrapht.core.GraphCycleAnalysis
import org.poc.objs.jgrapht.core.GraphCycleComponent
import org.poc.objs.api.domain.ResolvedGraphMaterialization
import org.poc.objs.jgrapht.core.UuidOrdering
import org.poc.objs.jgrapht.core.materialize.GenericGraphEdge
import org.poc.objs.jgrapht.core.materialize.GenericGraphVertex
import org.poc.objs.jgrapht.core.materialize.GenericJGraphTMaterializer
import org.poc.objs.jgrapht.core.materialize.MaterializedJGraphT
import java.util.UUID

/**
 * Directed cycle-region analysis using strongly connected components.
 *
 * Multi-node SCCs are cyclic. Singleton SCCs are cyclic only when they contain a self-loop.
 */
class DirectedCycleRegionAnalyzer(
    private val materializer: GenericJGraphTMaterializer = GenericJGraphTMaterializer(),
) {
    fun analyze(fragment: ResolvedGraphFragment): GraphCycleAnalysis {
        ResolvedGraphMaterialization.requireMaterializable(fragment)
        val materialized = materializer.materialize(fragment)
        return analyze(materialized)
    }

    fun analyze(
        materialized: MaterializedJGraphT<GenericGraphVertex, GenericGraphEdge>,
    ): GraphCycleAnalysis {
        val fragment = materialized.fragment
        val inspector = KosarajuStrongConnectivityInspector(materialized.graph)
        val components = inspector.stronglyConnectedSets().mapNotNull { vertices ->
            toCycleComponent(vertices, materialized)
        }.sortedWith(compareBy(UuidOrdering::compare) { it.id })

        return GraphCycleAnalysis(
            algorithm = GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS,
            components = components,
            stats = GraphAnalysisStats(
                entityCount = fragment.entities.size,
                edgeCount = fragment.edges.size,
                cyclicComponentCount = components.size,
            ),
            diagnostics = fragment.diagnostics,
        )
    }

    private fun toCycleComponent(
        vertices: Set<GenericGraphVertex>,
        materialized: MaterializedJGraphT<GenericGraphVertex, GenericGraphEdge>,
    ): GraphCycleComponent? {
        val entityIds = UuidOrdering.sorted(vertices.map { it.entityId })
        if (entityIds.isEmpty()) {
            return null
        }

        val entityIdSet = entityIds.toSet()
        val internalEdges = materialized.fragment.edges.filter { edge ->
            edge.source in entityIdSet && edge.target in entityIdSet
        }
        val cyclic = entityIds.size > 1 || internalEdges.any { it.source == it.target }
        if (!cyclic) {
            return null
        }

        val edgeIds = UuidOrdering.sorted(internalEdges.mapNotNull { it.id })
        return GraphCycleComponent(
            id = entityIds.first(),
            entityIds = entityIds,
            edgeIds = edgeIds,
        )
    }
}
