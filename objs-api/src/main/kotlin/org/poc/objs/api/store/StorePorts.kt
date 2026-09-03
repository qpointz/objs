package org.poc.objs.api.store

import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.match.Matcher
import java.util.UUID

/**
 * Read-side store port for graph engines (gremlin, jgrapht, …).
 * Persistence impl: [org.poc.objs.core.persistence.GraphStore] (G-A15).
 */
interface GraphStore {
    /** Graph-scoped select (G-G16). */
    fun select(matcher: Matcher): GraphContents

    fun selectInGraph(graphId: UUID, matcher: Matcher): GraphContents

    fun selectInGraphVersion(graphId: UUID, version: Long, matcher: Matcher): GraphContents
}
