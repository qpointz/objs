package org.poc.objs.core.subgraph

import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.match.BoMAnnotationMatcher
import org.poc.objs.core.match.BoMEdgeDomainCandidate
import org.poc.objs.core.match.BoMEntityDomainCandidate
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.match.MatchAllAnnotationMatcher
import org.poc.objs.core.match.asBoMMatcher
import java.util.UUID

/**
 * Selects an induced subgraph: matched entities + edges whose source and target are both selected
 * (or otherwise accepted by [BoMMatcher.matchesEdge]).
 */
object BoMSubgraphSelector {
    fun select(graph: BoMGraph, matcher: BoMMatcher): BoMSubgraph {
        val entities = graph.entities
            .map { BoMEntityDomainCandidate(it) }
            .filter { matcher.matches(it) }
            .map { it.toDomain() }
        val ids: Set<UUID> = entities.mapNotNull { it.id }.toSet()
        val edges = graph.edges
            .map { BoMEdgeDomainCandidate(it) }
            .filter { matcher.matchesEdge(it, ids) }
            .map { it.toDomain() }
        return BoMSubgraph(entities = entities, edges = edges)
    }

    fun select(graph: BoMGraph, matcher: BoMAnnotationMatcher): BoMSubgraph =
        select(graph, matcher.asBoMMatcher())

    fun selectMatchAll(graph: BoMGraph, filter: Map<String, String>): BoMSubgraph =
        select(graph, MatchAllAnnotationMatcher(filter))
}
