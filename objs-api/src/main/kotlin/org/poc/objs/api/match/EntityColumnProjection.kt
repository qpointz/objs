package org.poc.objs.api.match

/**
 * Which JSON columns are required while collecting / filtering entity candidates.
 * Survivors hydrate deferred columns before [EntityMatchCandidate.toDomain].
 */
data class EntityColumnProjection(
    val includePayload: Boolean,
    val includeAnnotations: Boolean,
) {
    companion object {
        /**
         * Annotations are needed when any stage runs as an in-memory filter.
         * Payload is needed when any [ObjExprMatcher] may evaluate `p` (local eval or filters).
         */
        fun forPlan(plan: EntitySelectionPlan): EntityColumnProjection {
            val needAnnotations = plan.localEval || plan.filters.isNotEmpty()
            val needPayload = plan.filters.any { it is ObjExprMatcher }
            return EntityColumnProjection(
                includePayload = needPayload,
                includeAnnotations = needAnnotations,
            )
        }
    }
}
