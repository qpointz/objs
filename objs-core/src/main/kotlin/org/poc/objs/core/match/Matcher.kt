package org.poc.objs.core.match

/**
 * Common matching contract. Executors resolve a [CandidateSource] from a
 * [SourceCapableMatcher] when possible, otherwise scan all entities and apply [matches].
 */
interface Matcher {
    fun matches(candidate: EntityMatchCandidate): Boolean

    /**
     * Optional edge filter after entity selection. Default keeps induced edges
     * (source and target both selected).
     */
    fun matchesEdge(candidate: EdgeMatchCandidate, selectedEntityIds: Set<java.util.UUID>): Boolean =
        candidate.source in selectedEntityIds && candidate.target in selectedEntityIds
}
