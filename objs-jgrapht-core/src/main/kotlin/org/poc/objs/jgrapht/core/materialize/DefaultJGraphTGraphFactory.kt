package org.poc.objs.jgrapht.core.materialize

import org.jgrapht.Graph
import org.jgrapht.graph.DirectedPseudograph
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.jgrapht.core.JGraphTGraphFactory

/** Default GENERIC factory using an unweighted [DirectedPseudograph]. */
class DefaultJGraphTGraphFactory : JGraphTGraphFactory<GenericGraphVertex, GenericGraphEdge> {
    override fun createGraph(): Graph<GenericGraphVertex, GenericGraphEdge> =
        DirectedPseudograph(GenericGraphEdge::class.java)

    override fun createVertex(entity: Entity): GenericGraphVertex =
        GenericGraphVertex(
            entityId = requireNotNull(entity.id) { "Entity type=${entity.type} has null id; cannot materialize" },
            entity = entity,
        )

    override fun createEdge(edge: Edge, source: GenericGraphVertex, target: GenericGraphVertex): GenericGraphEdge =
        GenericGraphEdge(
            edgeId = requireNotNull(edge.id) { "Edge role=${edge.role} has null id; cannot materialize" },
            edge = edge,
            role = edge.role,
        )
}
