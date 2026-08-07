package org.poc.objs.core.match

/**
 * Which JSON columns are required while collecting / filtering entity candidates.
 * Survivors hydrate deferred columns before [BoMEntityMatchCandidate.toDomain].
 */
data class BoMEntityColumnProjection(
    val includePayload: Boolean,
    val includeAnnotations: Boolean,
) {
    companion object {
        /**
         * Annotations are needed when any stage runs as an in-memory filter.
         * Payload is needed when any [BoMObjExprMatcher] may evaluate `p` (local eval or filters).
         */
        fun forPlan(plan: BoMEntitySelectionPlan): BoMEntityColumnProjection {
            val needAnnotations = plan.localEval || plan.filters.isNotEmpty()
            val needPayload = plan.filters.any { it is BoMObjExprMatcher }
            return BoMEntityColumnProjection(
                includePayload = needPayload,
                includeAnnotations = needAnnotations,
            )
        }
    }
}
