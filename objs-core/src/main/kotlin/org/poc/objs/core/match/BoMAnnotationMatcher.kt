package org.poc.objs.core.match

import org.poc.objs.core.domain.BoMEntity

/**
 * Compatibility extension point for annotation matching strategies (G-2).
 * Prefer [BoMMatcher] / [BoMSourceCapableMatcher] for new code.
 *
 * Note: this fun-interface is **not** the DSL `anno` matcher. DSL `anno` is
 * [MatchAllAnnotationMatcher], which is source-capable on PostgreSQL.
 */
fun interface BoMAnnotationMatcher {
    /** Returns true if [entity] satisfies this matcher. */
    fun matches(entity: BoMEntity): Boolean
}

/**
 * Default match-all strategy (DSL key `anno`): every filter key/value must be present on the entity.
 * Extra annotations on the entity are allowed.
 *
 * On PostgreSQL, [toCandidateSource] compiles the filter to JSONB `@>` containment so matching
 * runs in SQL; [matches] remains the in-memory semantic for filters / non-Postgres backends.
 */
class MatchAllAnnotationMatcher(
    val filter: Map<String, String>,
) : BoMMatcher, BoMSourceCapableMatcher {
    val expression: BoMMatchExpression =
        BoMMatchExpression.matchAllAnnotations(filter)

    override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
        candidate.annotationsMatchAll(filter)

    fun matches(entity: BoMEntity): Boolean =
        matches(BoMEntityDomainCandidate(entity))

    override fun toCandidateSource(backend: BoMEntityCandidateBackend): BoMCandidateSource? {
        val equals = BoMMatchExpression.annotationEqualsMap(expression) ?: return null
        return backend.annotationContainmentSource(equals)
    }
}
