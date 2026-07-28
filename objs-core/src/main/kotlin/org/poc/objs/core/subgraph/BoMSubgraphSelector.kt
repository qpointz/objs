package org.poc.objs.core.subgraph

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.match.BoMAnnotationMatcher
import java.util.UUID

/**
 * Selects an induced subgraph: matched entities + edges whose source and target are both selected.
 */
object BoMSubgraphSelector {
    fun select(graph: BoMGraph, matcher: BoMAnnotationMatcher): BoMSubgraph {
        val entities = graph.entities.filter { matcher.matches(it) }
        val ids: Set<UUID> = entities.mapNotNull { it.id }.toSet()
        // Entities without id cannot be edge endpoints in persisted graphs; still include in entity set.
        val edges = graph.edges.filter { edge ->
            edge.source in ids && edge.target in ids
        }
        return BoMSubgraph(entities = entities, edges = edges)
    }

    fun selectMatchAll(graph: BoMGraph, filter: Map<String, String>): BoMSubgraph =
        select(graph, org.poc.objs.core.match.MatchAllAnnotationMatcher(filter))
}
