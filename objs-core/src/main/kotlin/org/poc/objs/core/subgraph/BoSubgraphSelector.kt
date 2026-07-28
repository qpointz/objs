package org.poc.objs.core.subgraph

import org.poc.objs.core.domain.BoEdge
import org.poc.objs.core.domain.BoEntity
import org.poc.objs.core.domain.BoGraph
import org.poc.objs.core.domain.BoSubgraph
import org.poc.objs.core.match.BoAnnotationMatcher
import java.util.UUID

/**
 * Selects an induced subgraph: matched entities + edges whose source and target are both selected.
 */
object BoSubgraphSelector {
    fun select(graph: BoGraph, matcher: BoAnnotationMatcher): BoSubgraph {
        val entities = graph.entities.filter { matcher.matches(it) }
        val ids: Set<UUID> = entities.mapNotNull { it.id }.toSet()
        // Entities without id cannot be edge endpoints in persisted graphs; still include in entity set.
        val edges = graph.edges.filter { edge ->
            edge.source in ids && edge.target in ids
        }
        return BoSubgraph(entities = entities, edges = edges)
    }

    fun selectMatchAll(graph: BoGraph, filter: Map<String, String>): BoSubgraph =
        select(graph, org.poc.objs.core.match.MatchAllAnnotationMatcher(filter))
}
