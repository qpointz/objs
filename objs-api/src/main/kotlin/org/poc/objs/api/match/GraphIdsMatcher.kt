package org.poc.objs.api.match

import java.util.UUID

/**
 * DSL **`graphs-in: [uuid,…]`** — stage-0 graph scope that selects an **explicit set** of graphs
 * by id (order of ids does not matter; unknown ids are skipped).
 *
 * Selection unions each graph's stored member entities and graph-local edges, **distinct by id**.
 * Empty list → empty selection. May stand alone or lead a chain (later stages typically `obj-expr`).
 *
 * Domain code (e.g. portfolio → latest version graph ids) supplies the set; core does not know
 * portfolios.
 */
class GraphIdsMatcher(
    graphIds: Collection<UUID>,
) : Matcher {
    val graphIds: List<UUID> = graphIds.distinct()

    /** Header scope only; does not filter entity candidates directly. */
    override fun matches(candidate: EntityMatchCandidate): Boolean = true
}
