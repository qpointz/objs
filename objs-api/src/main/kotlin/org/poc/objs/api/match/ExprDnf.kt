package org.poc.objs.api.match

/**
 * Tiny boolean algebra over atomic predicates, normalized to DNF
 * (OR of AND-groups) for SQL pushdown.
 */
internal sealed interface BoolExpr<A> {
    data class Atom<A>(val value: A) : BoolExpr<A>
    data class And<A>(val parts: List<BoolExpr<A>>) : BoolExpr<A> {
        init {
            require(parts.isNotEmpty()) { "And must have parts" }
        }
    }
    data class Or<A>(val parts: List<BoolExpr<A>>) : BoolExpr<A> {
        init {
            require(parts.isNotEmpty()) { "Or must have parts" }
        }
    }
}

/** Convert [expr] to disjunctive normal form: each inner list is an AND of atoms. */
internal fun <A> toDnf(expr: BoolExpr<A>): List<List<A>> =
    when (expr) {
        is BoolExpr.Atom -> listOf(listOf(expr.value))
        is BoolExpr.And -> {
            var acc = listOf(emptyList<A>())
            for (part in expr.parts) {
                val right = toDnf(part)
                acc = acc.flatMap { leftGroup ->
                    right.map { rightGroup -> leftGroup + rightGroup }
                }
            }
            acc
        }
        is BoolExpr.Or -> expr.parts.flatMap { toDnf(it) }
    }
