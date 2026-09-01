package org.poc.objs.jgrapht.service

import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.api.domain.GraphMaterializationException
import org.poc.objs.api.domain.ResolvedGraphFragment
import org.poc.objs.core.match.Matcher
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.jgrapht.core.GraphAlgorithmCatalog
import org.poc.objs.jgrapht.core.GraphAlgorithmIds
import org.poc.objs.jgrapht.core.GraphCycleAnalysis
import org.poc.objs.jgrapht.core.GraphMaterializationModes
import org.poc.objs.jgrapht.core.analysis.DirectedCycleRegionAnalyzer
import org.springframework.stereotype.Service
import java.util.UUID

/** Optional extension for JVM typed materialization beyond the default GENERIC path. */
fun interface JGraphTTypedAnalysisProvider {
    fun analyze(fragment: ResolvedGraphFragment): GraphCycleAnalysis
}

@Service
class GraphAlgorithmService(
    private val store: GraphStore,
    private val policy: GraphFragmentPolicy,
    private val cycleAnalyzer: DirectedCycleRegionAnalyzer,
    private val typedProviders: List<JGraphTTypedAnalysisProvider> = emptyList(),
) {
    fun capabilities() =
        GraphAlgorithmCatalog.capabilities().let { base ->
            if (typedProviders.isEmpty()) {
                base
            } else {
                base.copy(
                    algorithms = base.algorithms.map { capability ->
                        capability.copy(
                            materializationModes =
                                (capability.materializationModes + GraphMaterializationModes.TYPED).distinct(),
                        )
                    },
                )
            }
        }

    fun analyzeCycles(
        matcher: Matcher,
        graphId: UUID? = null,
        graphVersion: Long? = null,
        algorithm: String? = null,
        materialization: String? = null,
    ): GraphCycleAnalysis {
        val resolvedAlgorithm = algorithm ?: GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS
        if (resolvedAlgorithm != GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS) {
            throw IllegalArgumentException("Unsupported algorithm '$resolvedAlgorithm'")
        }

        val mode = (materialization ?: GraphMaterializationModes.GENERIC).uppercase()
        val fragment = policy.resolve(selectContents(matcher, graphId, graphVersion))
        if (fragment.hasErrors()) {
            throw GraphMaterializationException(
                fragment.diagnostics.joinToString("; ") { it.message },
                diagnostics = fragment.diagnostics,
            )
        }

        return when (mode) {
            GraphMaterializationModes.GENERIC -> cycleAnalyzer.analyze(fragment)
            GraphMaterializationModes.TYPED -> {
                val provider = typedProviders.firstOrNull()
                    ?: throw IllegalArgumentException("Materialization mode TYPED is not available")
                provider.analyze(fragment)
            }
            else -> throw IllegalArgumentException("Unsupported materialization mode '$mode'")
        }
    }

    private fun selectContents(
        matcher: Matcher,
        graphId: UUID?,
        graphVersion: Long?,
    ): GraphContents =
        when {
            graphId != null && graphVersion != null ->
                store.selectInGraphVersion(graphId, graphVersion, matcher)
            graphId != null ->
                store.selectInGraph(graphId, matcher)
            else ->
                store.select(matcher)
        }
}
