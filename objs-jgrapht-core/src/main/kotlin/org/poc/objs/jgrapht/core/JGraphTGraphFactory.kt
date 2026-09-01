package org.poc.objs.jgrapht.core

import org.jgrapht.Graph
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.ResolvedGraphFragment

/** Optional extension for caller-defined JGraphT graph, vertex, and edge construction. */
interface JGraphTGraphFactory<V, E> {
    fun createGraph(): Graph<V, E>

    fun createVertex(entity: Entity): V

    fun createEdge(edge: Edge, source: V, target: V): E
}
