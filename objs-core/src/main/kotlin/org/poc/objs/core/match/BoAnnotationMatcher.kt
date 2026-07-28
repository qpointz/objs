package org.poc.objs.core.match

import org.poc.objs.core.domain.BoEntity

/**
 * Extension point for annotation matching strategies (G-2).
 */
fun interface BoAnnotationMatcher {
    /** Returns true if [entity] satisfies this matcher. */
    fun matches(entity: BoEntity): Boolean
}

/**
 * Default match-all strategy: every filter key/value must be present on the entity.
 * Extra annotations on the entity are allowed.
 */
class MatchAllAnnotationMatcher(
    private val filter: Map<String, String>,
) : BoAnnotationMatcher {
    override fun matches(entity: BoEntity): Boolean =
        filter.all { (key, value) -> entity.annotations[key] == value }
}
