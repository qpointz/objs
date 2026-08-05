package org.poc.objs.core.match

/**
 * Structured matcher predicate used by [MatchAllAnnotationMatcher] / [BoMAnnoExprLowerer]
 * for in-memory evaluation and compiling annotation-equality maps into Postgres containment sources.
 */
sealed interface BoMMatchExpression {
    fun matches(candidate: BoMEntityMatchCandidate): Boolean

    data class AnnotationEquals(
        val key: String,
        val value: String,
    ) : BoMMatchExpression {
        override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
            candidate.annotationsMatchAll(mapOf(key to value))
    }

    data class And(
        val expressions: List<BoMMatchExpression>,
    ) : BoMMatchExpression {
        override fun matches(candidate: BoMEntityMatchCandidate): Boolean {
            val equalsMap = annotationEqualsMap(this)
            if (equalsMap != null) {
                return candidate.annotationsMatchAll(equalsMap)
            }
            return expressions.all { it.matches(candidate) }
        }
    }

    data class Or(
        val expressions: List<BoMMatchExpression>,
    ) : BoMMatchExpression {
        override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
            expressions.any { it.matches(candidate) }
    }

    companion object {
        fun annotationEquals(key: String, value: String): AnnotationEquals =
            AnnotationEquals(key, value)

        fun and(vararg expressions: BoMMatchExpression): And =
            And(expressions.toList())

        fun or(vararg expressions: BoMMatchExpression): Or =
            Or(expressions.toList())

        fun matchAllAnnotations(filter: Map<String, String>): BoMMatchExpression =
            if (filter.isEmpty()) {
                And(emptyList())
            } else {
                And(filter.map { (key, value) -> AnnotationEquals(key, value) })
            }

        /**
         * Flattens AnnotationEquals / And into a containment map, or null if the shape is unsupported
         * (e.g. contains [Or]).
         */
        fun annotationEqualsMap(expression: BoMMatchExpression): Map<String, String>? {
            val out = linkedMapOf<String, String>()
            return if (collectAnnotationEquals(expression, out)) out else null
        }

        /**
         * Converts equality / And / Or trees into DNF containment maps suitable for
         * `annotations @> f1 OR annotations @> f2 …`. Returns null if unsupported.
         */
        fun containmentDisjuncts(expression: BoMMatchExpression): List<Map<String, String>>? {
            val dnf = toDnf(expression) ?: return null
            return when (dnf) {
                is AnnotationEquals -> listOf(mapOf(dnf.key to dnf.value))
                is And -> listOf(annotationEqualsMap(dnf) ?: return null)
                is Or -> {
                    if (dnf.expressions.isEmpty()) return null
                    dnf.expressions.map { branch ->
                        annotationEqualsMap(branch) ?: return null
                    }
                }
            }
        }

        private fun collectAnnotationEquals(
            expression: BoMMatchExpression,
            out: MutableMap<String, String>,
        ): Boolean = when (expression) {
            is AnnotationEquals -> {
                out[expression.key] = expression.value
                true
            }
            is And -> expression.expressions.all { collectAnnotationEquals(it, out) }
            is Or -> false
        }

        /** Push Or outward (DNF) over And of equality trees. */
        private fun toDnf(expression: BoMMatchExpression): BoMMatchExpression? = when (expression) {
            is AnnotationEquals -> expression
            is Or -> {
                val parts = ArrayList<BoMMatchExpression>()
                for (child in expression.expressions) {
                    val dnf = toDnf(child) ?: return null
                    when (dnf) {
                        is Or -> parts.addAll(dnf.expressions)
                        else -> parts.add(dnf)
                    }
                }
                when {
                    parts.isEmpty() -> null
                    parts.size == 1 -> parts[0]
                    else -> Or(parts)
                }
            }
            is And -> {
                if (expression.expressions.isEmpty()) {
                    And(emptyList())
                } else {
                    expression.expressions
                        .map { toDnf(it) ?: return null }
                        .reduce { acc, next -> distributeAnd(acc, next) }
                }
            }
        }

        private fun distributeAnd(left: BoMMatchExpression, right: BoMMatchExpression): BoMMatchExpression {
            val leftParts = orBranches(left)
            val rightParts = orBranches(right)
            val products = leftParts.flatMap { l ->
                rightParts.map { r -> mergeAnd(l, r) }
            }
            return when {
                products.isEmpty() -> And(emptyList())
                products.size == 1 -> products[0]
                else -> Or(products)
            }
        }

        private fun orBranches(expression: BoMMatchExpression): List<BoMMatchExpression> =
            when (expression) {
                is Or -> expression.expressions
                else -> listOf(expression)
            }

        private fun mergeAnd(left: BoMMatchExpression, right: BoMMatchExpression): BoMMatchExpression {
            val parts = ArrayList<BoMMatchExpression>()
            fun add(expr: BoMMatchExpression) {
                when (expr) {
                    is And -> parts.addAll(expr.expressions)
                    else -> parts.add(expr)
                }
            }
            add(left)
            add(right)
            return when {
                parts.isEmpty() -> And(emptyList())
                parts.size == 1 -> parts[0]
                else -> And(parts)
            }
        }
    }
}
