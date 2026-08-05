package org.poc.objs.core.match

/**
 * Which JSON columns are required while collecting / filtering entity candidates.
 * Payload is never required for current matchers; survivors hydrate deferred columns before
 * [BoMEntityMatchCandidate.toDomain].
 */
data class BoMEntityColumnProjection(
    val includePayload: Boolean,
    val includeAnnotations: Boolean,
) {
    companion object {
        /**
         * Matching never reads payload. Annotations are needed when any stage runs as an
         * in-memory filter ([BoMEntitySelectionPlan.localEval] or non-empty [BoMEntitySelectionPlan.filters]).
         * Pure SQL sources with no later filters omit both JSON columns until survivor hydrate.
         */
        fun forPlan(plan: BoMEntitySelectionPlan): BoMEntityColumnProjection {
            val needAnnotations = plan.localEval || plan.filters.isNotEmpty()
            return BoMEntityColumnProjection(
                includePayload = false,
                includeAnnotations = needAnnotations,
            )
        }
    }
}
