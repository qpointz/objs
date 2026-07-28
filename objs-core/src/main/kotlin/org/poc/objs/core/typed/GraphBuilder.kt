package org.poc.objs.core.typed

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import java.util.UUID

/** Handle to an entity added to a [GraphBuilder]. */
data class NodeRef(val id: UUID, val key: String? = null)

/**
 * Assembles a [BoMGraph] from typed entities and edges.
 * Assigns provisional UUIDs when missing so edges can be declared before persist.
 */
class GraphBuilder(
    private val defaultAnnotations: Map<String, String> = emptyMap(),
) {
    private val entities = linkedMapOf<UUID, BoMEntity>()
    private val edges = mutableListOf<BoMEdge>()
    private val keys = mutableMapOf<String, UUID>()

    fun add(entity: TypedEntity<*>, key: String? = null): NodeRef {
        if (entity.id == null) {
            entity.id = UUID.randomUUID()
        }
        val id = requireNotNull(entity.id)
        entity.annotations = mergeAnnotations(defaultAnnotations, entity.annotations)
        val bom = entity.toBoMEntity()
        entities[id] = bom
        if (key != null) {
            keys[key] = id
        }
        return NodeRef(id, key)
    }

    fun add(entity: BoMEntity, key: String? = null): NodeRef {
        if (entity.id == null) {
            entity.id = UUID.randomUUID()
        }
        val id = requireNotNull(entity.id)
        entity.annotations = mergeAnnotations(defaultAnnotations, entity.annotations)
        entities[id] = entity
        if (key != null) {
            keys[key] = id
        }
        return NodeRef(id, key)
    }

    fun ref(key: String): NodeRef {
        val id = keys[key] ?: error("Unknown local key: $key")
        return NodeRef(id, key)
    }

    fun edge(source: NodeRef, role: String, target: NodeRef, typed: TypedEdge<*>? = null): GraphBuilder {
        val e = typed?.toBoMEdge(source.id, target.id)
            ?: BoMEdge(source = source.id, target = target.id, role = role)
        if (e.id == null) {
            e.id = UUID.randomUUID()
        }
        edges += e
        return this
    }

    fun edge(source: NodeRef, typed: TypedEdge<*>, target: NodeRef): GraphBuilder =
        edge(source, typed.meta.role, target, typed)

    fun build(): BoMGraph = BoMGraph(
        entities = entities.values.toMutableList(),
        edges = edges.toMutableList(),
    )
}
