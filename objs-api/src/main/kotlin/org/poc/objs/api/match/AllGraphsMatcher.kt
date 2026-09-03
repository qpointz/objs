package org.poc.objs.api.match

/**
 * DSL **`all: true`** — stage-0 graph scope that selects **every** graph.
 *
 * Selection unions each graph's stored member entities and graph-local edges, **distinct by id**
 * (an entity in multiple graphs appears once; an edge belongs to one graph so ids are unique).
 * Orphan pool entities (no membership) are not included — they are not in any graph.
 *
 * May stand alone or lead a chain (later stages typically `obj-expr` filters).
 */
object AllGraphsMatcher : Matcher {
    /** Header scope only; does not filter entity candidates directly. */
    override fun matches(candidate: EntityMatchCandidate): Boolean = true
}
