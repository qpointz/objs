package org.poc.objs.core.match

/**
 * Common matching contract. Executors resolve a [BoMCandidateSource] from a
 * [BoMSourceCapableMatcher] when possible, otherwise scan all entities and apply [matches].
 */
interface BoMMatcher {
    fun matches(candidate: BoMEntityMatchCandidate): Boolean

    /**
     * Optional edge filter after entity selection. Default keeps induced edges
     * (source and target both selected).
     */
    fun matchesEdge(candidate: BoMEdgeMatchCandidate, selectedEntityIds: Set<java.util.UUID>): Boolean =
        candidate.source in selectedEntityIds && candidate.target in selectedEntityIds
}
