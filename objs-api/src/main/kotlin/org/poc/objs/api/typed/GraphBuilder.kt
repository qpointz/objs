package org.poc.objs.api.typed

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.GraphFragment
import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.api.domain.ResolvedGraphFragment
import java.util.UUID

/** Handle to an entity added to a [GraphBuilder]. */
data class NodeRef(val id: UUID, val key: String? = null)

/**
 * Assembles a [Graph] from typed entities and edges.
 *
 * UUID is the only identity. Missing UUIDs receive provisional values, while registering an
 * entity or edge with an existing UUID is rejected.
 */
class GraphBuilder(
    private val payloadMapper: PayloadMapper,
    private val defaultAnnotations: Map<String, String> = emptyMap(),
) {
    private val entities = linkedMapOf<UUID, Entity>()
    private val edges = linkedMapOf<UUID, Edge>()
    private val keys = mutableMapOf<String, UUID>()

    fun add(entity: TypedEntity<*>, key: String? = null): NodeRef {
        if (entity.id == null) {
            entity.id = UUID.randomUUID()
        }
        val id = requireNotNull(entity.id)
        entity.annotations = mergeAnnotations(defaultAnnotations, entity.annotations)
        return add(entity.toEntity(payloadMapper), key)
    }

    fun add(entity: Entity, key: String? = null): NodeRef {
        if (entity.id == null) {
            entity.id = UUID.randomUUID()
        }
        val id = requireNotNull(entity.id)
        check(entities.putIfAbsent(id, entity) == null) {
            "Duplicate entity UUID: $id"
        }
        entity.annotations = mergeAnnotations(defaultAnnotations, entity.annotations)
        if (key != null) {
            check(keys.putIfAbsent(key, id) == null) {
                "Duplicate local key: $key"
            }
        }
        return NodeRef(id, key)
    }

    fun add(edge: Edge): GraphBuilder {
        if (edge.id == null) {
            edge.id = UUID.randomUUID()
        }
        val id = requireNotNull(edge.id)
        check(edges.putIfAbsent(id, edge) == null) {
            "Duplicate edge UUID: $id"
        }
        return this
    }

    fun ref(key: String): NodeRef {
        val id = keys[key] ?: error("Unknown local key: $key")
        return NodeRef(id, key)
    }

    fun edge(source: NodeRef, role: String, target: NodeRef, typed: TypedEdge<*>? = null): GraphBuilder {
        val edge = typed?.toEdge(source.id, target.id, payloadMapper)
            ?: Edge(source = source.id, target = target.id, role = role)
        return add(edge)
    }

    fun edge(source: NodeRef, typed: TypedEdge<*>, target: NodeRef): GraphBuilder =
        edge(source, typed.meta.role, target, typed)

    fun build(): Graph = Graph(
        entities = entities.values.toMutableList(),
        edges = edges.values.toMutableList(),
    )

    /** Exposes the assembled typed values at the shared fragment boundary. */
    fun buildFragment(): GraphFragment = build()

    /** Resolves the complete typed fragment after all [NodeRef] endpoints have been captured. */
    @JvmOverloads
    fun buildResolved(policy: GraphFragmentPolicy = DefaultGraphFragmentPolicy): ResolvedGraphFragment =
        policy.resolve(build())
}
