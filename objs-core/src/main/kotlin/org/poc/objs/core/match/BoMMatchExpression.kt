package org.poc.objs.core.match

/**
 * Structured matcher predicate. Pushable matchers expose expressions; stores may compile
 * supported shapes to SQL and fall back to [matches] evaluation.
 */
sealed interface BoMMatchExpression {
    fun matches(candidate: BoMEntityMatchCandidate): Boolean

    data class AnnotationEquals(
        val key: String,
        val value: String,
    ) : BoMMatchExpression {
        override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
            candidate.annotations[key] == value
    }

    data class And(
        val expressions: List<BoMMatchExpression>,
    ) : BoMMatchExpression {
        override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
            expressions.all { it.matches(candidate) }
    }

    companion object {
        fun annotationEquals(key: String, value: String): AnnotationEquals =
            AnnotationEquals(key, value)

        fun and(vararg expressions: BoMMatchExpression): And =
            And(expressions.toList())

        fun matchAllAnnotations(filter: Map<String, String>): BoMMatchExpression =
            if (filter.isEmpty()) {
                And(emptyList())
            } else {
                And(filter.map { (key, value) -> AnnotationEquals(key, value) })
            }
    }
}
