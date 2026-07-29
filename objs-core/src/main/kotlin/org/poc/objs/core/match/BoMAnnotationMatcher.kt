package org.poc.objs.core.match

import org.poc.objs.core.domain.BoMEntity

/**
 * Compatibility extension point for annotation matching strategies (G-2).
 * Prefer [BoMMatcher] / [BoMPushableMatcher] / [BoMNonPushableMatcher] for new code.
 */
fun interface BoMAnnotationMatcher {
    /** Returns true if [entity] satisfies this matcher. */
    fun matches(entity: BoMEntity): Boolean
}

/**
 * Default match-all strategy: every filter key/value must be present on the entity.
 * Extra annotations on the entity are allowed.
 *
 * This matcher is pushable: stores may compile [expression] to SQL while preserving the
 * same Kotlin evaluation semantics.
 */
class MatchAllAnnotationMatcher(
    val filter: Map<String, String>,
) : BoMPushableMatcher() {
    override val expression: BoMMatchExpression =
        BoMMatchExpression.matchAllAnnotations(filter)

    fun matches(entity: BoMEntity): Boolean =
        matches(BoMEntityDomainCandidate(entity))
}
