package org.poc.objs.core.domain

/**
 * Collision policy for [org.poc.objs.core.persistence.BoMNamedGraphStore.mergeGraph].
 *
 * Detect overlap with [nodeKey] / [edgeKey]; choose a survivor with [onDuplicateNode] /
 * [onDuplicateEdge]. Not identity-twin grouping (FB-2).
 */
interface GraphMergePolicy {
    fun nodeKey(entity: BoMEntity): Any

    fun edgeKey(edge: BoMEdge): Any

    fun onDuplicateNode(kept: BoMEntity, incoming: BoMEntity): BoMEntity

    fun onDuplicateEdge(kept: BoMEdge, incoming: BoMEdge): BoMEdge
}

/**
 * Default merge policy: node key = entity id; edge key = `(source, role, target)`;
 * keep first in caller order; do not merge property maps.
 */
open class FirstSeenGraphMergePolicy : GraphMergePolicy {
    override fun nodeKey(entity: BoMEntity): Any =
        requireNotNull(entity.id) { "member entity missing id" }

    override fun edgeKey(edge: BoMEdge): Any = Triple(edge.source, edge.role, edge.target)

    override fun onDuplicateNode(kept: BoMEntity, incoming: BoMEntity): BoMEntity = kept

    override fun onDuplicateEdge(kept: BoMEdge, incoming: BoMEdge): BoMEdge = kept
}
