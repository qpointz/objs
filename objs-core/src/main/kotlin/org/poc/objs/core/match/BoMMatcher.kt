package org.poc.objs.core.match

import org.poc.objs.core.domain.BoMEntity

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

/**
 * Adapter for legacy [BoMAnnotationMatcher] lambdas. Always filter-only (no SQL source).
 * Uses candidate annotations directly; payload is not required for selection matching.
 */
class BoMAnnotationMatcherAdapter(
    private val matcher: BoMAnnotationMatcher,
) : BoMMatcher {
    override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
        matcher.matches(
            org.poc.objs.core.domain.BoMEntity(
                id = candidate.id,
                type = candidate.type,
                schemaVersion = candidate.schemaVersion,
                annotations = candidate.annotations,
                payload = mutableMapOf(),
            ),
        )
}

fun BoMAnnotationMatcher.asBoMMatcher(): BoMMatcher = when (this) {
    is BoMMatcher -> this
    else -> BoMAnnotationMatcherAdapter(this)
}

fun BoMMatcher.asAnnotationMatcher(): BoMAnnotationMatcher = when (this) {
    is BoMAnnotationMatcher -> this
    else -> BoMAnnotationMatcher { entity -> matches(BoMEntityDomainCandidate(entity)) }
}
