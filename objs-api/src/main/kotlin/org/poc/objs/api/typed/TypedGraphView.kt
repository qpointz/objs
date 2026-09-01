package org.poc.objs.api.typed

import org.poc.objs.api.AmbiguousRelationException
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.GraphFragment
import java.util.UUID
import java.util.stream.Stream

/** Exact-version application binding used only when a caller wants hydrated payloads. */
fun interface TypedEntityBinding {
    fun hydrate(entity: Entity, mapper: PayloadMapper): Any
}

/** Application-owned exact `(type, schemaVersion)` binding lookup. */
fun interface TypedEntityBindingRegistry {
    fun find(type: String, schemaVersion: String): TypedEntityBinding?
}

/** Direction used by generic relation navigation. */
enum class RelationDirection {
    OUTBOUND,
    INBOUND,
    BOTH,
}

/** Immutable, Java-compatible collection returned by read navigation. */
class TypedCollection<T> private constructor(
    private val values: List<T>,
) : Iterable<T> {
    val size: Int get() = values.size

    fun isEmpty(): Boolean = values.isEmpty()

    fun asList(): List<T> = values

    operator fun get(index: Int): T = values[index]

    fun stream(): Stream<T> = values.stream()

    override fun iterator(): Iterator<T> = values.iterator()

    companion object {
        @JvmStatic
        fun <T> of(values: Iterable<T>): TypedCollection<T> = TypedCollection(values.toList())
    }
}

/** Schema-agnostic identity/payload handle for a read node. */
data class EntityRef<P : Any>(
    val id: UUID?,
    val type: String,
    val schemaVersion: String,
    val payload: P? = null,
)

/** A directed edge and its resolved-or-raw endpoints. */
data class RelationEdgeView(
    val edge: Edge,
    val source: ReadNode?,
    val target: ReadNode?,
) {
    val sourceId: UUID get() = edge.source
    val targetId: UUID get() = edge.target
    val role: String get() = edge.role
    val properties: Map<String, Any?>? get() = edge.properties
}

/** Read-only node facade over one snapshot entity. */
class ReadNode internal constructor(
    private val view: TypedGraphView,
    val entity: Entity,
    val hydratedPayload: Any?,
) {
    val id: UUID? get() = entity.id
    val type: String get() = entity.type
    val schemaVersion: String get() = entity.schemaVersion

    fun ref(): EntityRef<Any> = EntityRef(
        id = entity.id,
        type = entity.type,
        schemaVersion = entity.schemaVersion,
        payload = hydratedPayload,
    )

    @JvmOverloads
    fun edges(
        role: String? = null,
        direction: RelationDirection = RelationDirection.OUTBOUND,
    ): TypedCollection<RelationEdgeView> = view.edgesFor(this, role, direction)

    fun outboundEdges(role: String): TypedCollection<RelationEdgeView> =
        edges(role, RelationDirection.OUTBOUND)

    fun inboundEdges(role: String): TypedCollection<RelationEdgeView> =
        edges(role, RelationDirection.INBOUND)

    fun targets(role: String): TypedCollection<ReadNode> =
        TypedCollection.of(outboundEdges(role).asList().mapNotNull { it.target })

    fun sources(role: String): TypedCollection<ReadNode> =
        TypedCollection.of(inboundEdges(role).asList().mapNotNull { it.source })

    @JvmOverloads
    fun singular(
        role: String,
        direction: RelationDirection = RelationDirection.OUTBOUND,
    ): ReadNode? {
        val endpoints = edges(role, direction).asList().map {
            when (direction) {
                RelationDirection.INBOUND -> it.source
                RelationDirection.OUTBOUND -> it.target
                RelationDirection.BOTH -> if (it.source?.id == id) it.target else it.source
            }
        }
        val resolved = endpoints.filterNotNull()
        if (resolved.size > 1) {
            throw AmbiguousRelationException(id, role, resolved.size)
        }
        return resolved.firstOrNull()
    }
}

/**
 * Immutable in-memory graph view. It snapshots raw entities and edges and never persists or
 * mutates the supplied graph.
 */
class TypedGraphView private constructor(
    fragment: GraphFragment,
    private val bindings: TypedEntityBindingRegistry?,
    private val mapper: PayloadMapper?,
) {
    private val entities: List<Entity> = fragment.entities.map {
        it.copy(
            payload = it.payload.toMutableMap(),
            annotations = it.annotations.toMutableMap(),
        )
    }
    private val edges: List<Edge> = fragment.edges.map {
        it.copy(properties = it.properties?.toMutableMap())
    }
    private val nodesById = linkedMapOf<UUID, ReadNode>()

    init {
        entities.forEach { entity ->
            val id = entity.id ?: return@forEach
            nodesById[id] = ReadNode(this, entity, hydrate(entity))
        }
    }

    fun allNodes(): TypedCollection<ReadNode> = TypedCollection.of(nodesById.values)

    fun allEdges(): TypedCollection<RelationEdgeView> =
        TypedCollection.of(edges.map { relationEdge(it) })

    fun nodes(type: String): TypedCollection<ReadNode> =
        TypedCollection.of(nodesById.values.filter { it.type == type })

    fun nodes(type: String, schemaVersion: String): TypedCollection<ReadNode> =
        TypedCollection.of(nodesById.values.filter {
            it.type == type && it.schemaVersion == schemaVersion
        })

    fun node(id: UUID): ReadNode? = nodesById[id]

    internal fun edgesFor(
        node: ReadNode,
        role: String?,
        direction: RelationDirection,
    ): TypedCollection<RelationEdgeView> {
        val id = node.id ?: return TypedCollection.of(emptyList())
        val selected = edges.filter { edge ->
            val roleMatches = role == null || edge.role == role
            val directionMatches = when (direction) {
                RelationDirection.OUTBOUND -> edge.source == id
                RelationDirection.INBOUND -> edge.target == id
                RelationDirection.BOTH -> edge.source == id || edge.target == id
            }
            roleMatches && directionMatches
        }
        return TypedCollection.of(selected.map { relationEdge(it) })
    }

    private fun relationEdge(edge: Edge): RelationEdgeView =
        RelationEdgeView(
            edge = edge,
            source = nodesById[edge.source],
            target = nodesById[edge.target],
        )

    private fun hydrate(entity: Entity): Any? =
        if (bindings == null || mapper == null) {
            null
        } else {
            bindings.find(entity.type, entity.schemaVersion)?.hydrate(entity, mapper)
        }

    companion object {
        @JvmStatic
        fun from(fragment: GraphFragment): TypedGraphView = TypedGraphView(fragment, null, null)

        @JvmStatic
        fun from(graph: Graph): TypedGraphView = from(graph as GraphFragment)

        @JvmStatic
        @JvmOverloads
        fun from(
            fragment: GraphFragment,
            bindings: TypedEntityBindingRegistry?,
            mapper: PayloadMapper? = null,
        ): TypedGraphView = TypedGraphView(fragment, bindings, mapper)

        @JvmStatic
        @JvmOverloads
        fun from(
            graph: Graph,
            bindings: TypedEntityBindingRegistry?,
            mapper: PayloadMapper? = null,
        ): TypedGraphView = from(graph as GraphFragment, bindings, mapper)
    }
}
