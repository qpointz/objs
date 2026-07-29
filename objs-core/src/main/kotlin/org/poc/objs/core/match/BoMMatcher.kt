package org.poc.objs.core.match

import org.poc.objs.core.domain.BoMEntity

/**
 * Common matching contract. Stores route [BoMPushableMatcher] through SQL when possible and
 * scan every other matcher via raw/lazy candidates.
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
 * Pushable matcher: exposes a structured [expression] that stores may compile to SQL.
 * Expression evaluation remains available for semantic parity and non-SQL backends.
 */
abstract class BoMPushableMatcher : BoMMatcher {
    abstract val expression: BoMMatchExpression

    override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
        expression.matches(candidate)
}

/**
 * Non-pushable matcher: always evaluated in memory against lightweight candidates.
 */
abstract class BoMNonPushableMatcher : BoMMatcher

/**
 * Compatibility adapter for existing [BoMAnnotationMatcher] lambdas/custom implementations.
 * Adapters are treated as non-pushable scans.
 */
class BoMAnnotationMatcherAdapter(
    private val matcher: BoMAnnotationMatcher,
) : BoMNonPushableMatcher() {
    override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
        matcher.matches(candidate.toDomain())
}

fun BoMAnnotationMatcher.asBoMMatcher(): BoMMatcher = when (this) {
    is BoMMatcher -> this
    else -> BoMAnnotationMatcherAdapter(this)
}

fun BoMMatcher.asAnnotationMatcher(): BoMAnnotationMatcher = when (this) {
    is BoMAnnotationMatcher -> this
    else -> BoMAnnotationMatcher { entity -> matches(BoMEntityDomainCandidate(entity)) }
}
