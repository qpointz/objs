package org.poc.objs.jgrapht.core.materialize

import org.jgrapht.Graph
import org.poc.objs.api.domain.ResolvedGraphFragment
import org.poc.objs.jgrapht.core.JGraphTGraphFactory
import org.poc.objs.api.domain.ResolvedGraphMaterialization
import java.util.UUID

/** Caller-owned JGraphT graph populated from one resolved fragment. */
data class MaterializedJGraphT<V, E>(
    val graph: Graph<V, E>,
    val verticesByEntityId: Map<UUID, V>,
    val edgesById: Map<UUID, E>,
    val fragment: ResolvedGraphFragment,
)

class GenericJGraphTMaterializer(
    private val factory: JGraphTGraphFactory<GenericGraphVertex, GenericGraphEdge> = DefaultJGraphTGraphFactory(),
) {
    fun materialize(fragment: ResolvedGraphFragment): MaterializedJGraphT<GenericGraphVertex, GenericGraphEdge> =
        materialize(fragment, factory)

    fun <V, E> materialize(
        fragment: ResolvedGraphFragment,
        factory: JGraphTGraphFactory<V, E>,
    ): MaterializedJGraphT<V, E> {
        ResolvedGraphMaterialization.requireMaterializable(fragment)
        val graph = factory.createGraph()
        val verticesByEntityId = linkedMapOf<UUID, V>()
        for (entity in fragment.entities) {
            val vertex = factory.createVertex(entity)
            graph.addVertex(vertex)
            verticesByEntityId[requireNotNull(entity.id)] = vertex
        }

        val edgesById = linkedMapOf<UUID, E>()
        for (edge in fragment.edges) {
            val source = verticesByEntityId[edge.source]
                ?: throw IllegalArgumentException("Edge ${edge.id} source ${edge.source} not in fragment entities")
            val target = verticesByEntityId[edge.target]
                ?: throw IllegalArgumentException("Edge ${edge.id} target ${edge.target} not in fragment entities")
            val graphEdge = factory.createEdge(edge, source, target)
            graph.addEdge(source, target, graphEdge)
            edgesById[requireNotNull(edge.id)] = graphEdge
        }

        return MaterializedJGraphT(
            graph = graph,
            verticesByEntityId = verticesByEntityId,
            edgesById = edgesById,
            fragment = fragment,
        )
    }
}
