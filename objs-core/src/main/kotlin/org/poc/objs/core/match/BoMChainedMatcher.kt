package org.poc.objs.core.match

/**
 * Ordered composite matcher. Callers treat this as a normal [BoMMatcher]; store executors may
 * decompose children for first-stage pushdown.
 */
class BoMChainedMatcher(
    matchers: List<BoMMatcher>,
) : BoMMatcher {
    val matchers: List<BoMMatcher> = matchers.toList()

    init {
        require(matchers.isNotEmpty()) { "Chained matcher requires at least one child" }
    }

    override fun matches(candidate: BoMEntityMatchCandidate): Boolean =
        matchers.all { it.matches(candidate) }

    override fun matchesEdge(
        candidate: BoMEdgeMatchCandidate,
        selectedEntityIds: Set<java.util.UUID>,
    ): Boolean = matchers.last().matchesEdge(candidate, selectedEntityIds)
}
