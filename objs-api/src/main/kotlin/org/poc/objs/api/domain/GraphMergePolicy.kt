package org.poc.objs.api.domain

/**
 * Collision policy for named-graph merge.
 *
 * Detect overlap with [nodeKey] / [edgeKey]; choose a survivor with [onDuplicateNode] /
 * [onDuplicateEdge]. Not identity-twin grouping (FB-2).
 */
interface GraphMergePolicy {
    fun nodeKey(entity: Entity): Any

    fun edgeKey(edge: Edge): Any

    fun onDuplicateNode(kept: Entity, incoming: Entity): Entity

    fun onDuplicateEdge(kept: Edge, incoming: Edge): Edge
}

/**
 * Default merge policy: node key = entity id; edge key = `(source, role, target)`;
 * keep first in caller order; do not merge property maps.
 */
open class FirstSeenGraphMergePolicy : GraphMergePolicy {
    override fun nodeKey(entity: Entity): Any =
        requireNotNull(entity.id) { "member entity missing id" }

    override fun edgeKey(edge: Edge): Any = Triple(edge.source, edge.role, edge.target)

    override fun onDuplicateNode(kept: Entity, incoming: Entity): Entity = kept

    override fun onDuplicateEdge(kept: Edge, incoming: Edge): Edge = kept
}
