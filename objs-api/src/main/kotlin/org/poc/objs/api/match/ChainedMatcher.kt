package org.poc.objs.api.match

/**
 * Ordered composite matcher. Callers treat this as a normal [Matcher]; store executors may
 * decompose children for first-stage pushdown.
 */
class ChainedMatcher(
    matchers: List<Matcher>,
) : Matcher {
    val matchers: List<Matcher> = matchers.toList()

    init {
        require(matchers.isNotEmpty()) { "Chained matcher requires at least one child" }
    }

    override fun matches(candidate: EntityMatchCandidate): Boolean =
        matchers.all { it.matches(candidate) }

    override fun matchesEdge(
        candidate: EdgeMatchCandidate,
        selectedEntityIds: Set<java.util.UUID>,
    ): Boolean = matchers.last().matchesEdge(candidate, selectedEntityIds)
}
