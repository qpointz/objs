package org.poc.objs.api.match

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import java.util.UUID

/**
 * Lightweight entity row for matching. Scalar fields are always available; JSON maps may be
 * raw/lazy and deserialize only when accessed.
 */
interface EntityMatchCandidate {
    val id: UUID?
    val type: String
    val schemaVersion: String
    val annotations: MutableMap<String, String>
    val payload: MutableMap<String, Any?>

    /**
     * True when every [filter] entry is present with equal value.
     * Default walks [annotations]; raw-backed candidates may avoid full map materialization.
     */
    fun annotationsMatchAll(filter: Map<String, String>): Boolean =
        filter.all { (key, value) -> annotations[key] == value }

    fun toDomain(): Entity = Entity(
        id = id,
        type = type,
        schemaVersion = schemaVersion,
        payload = payload,
        annotations = annotations,
    )
}

/**
 * Lightweight edge row for matching. Properties may remain raw/lazy until accessed.
 */
interface EdgeMatchCandidate {
    val id: UUID?
    val source: UUID
    val target: UUID
    val role: String
    val type: String?
    val schemaVersion: String?
    val properties: MutableMap<String, Any?>?

    fun toDomain(): Edge = Edge(
        id = id,
        source = source,
        target = target,
        role = role,
        type = type,
        schemaVersion = schemaVersion,
        properties = properties,
    )
}

/** Eager candidate wrapping an existing domain entity. */
class EntityDomainCandidate(
    private val entity: Entity,
) : EntityMatchCandidate {
    override val id: UUID? get() = entity.id
    override val type: String get() = entity.type
    override val schemaVersion: String get() = entity.schemaVersion
    override val annotations: MutableMap<String, String> get() = entity.annotations
    override val payload: MutableMap<String, Any?> get() = entity.payload
    override fun toDomain(): Entity = entity
}

/** Eager candidate wrapping an existing domain edge. */
class EdgeDomainCandidate(
    private val edge: Edge,
) : EdgeMatchCandidate {
    override val id: UUID? get() = edge.id
    override val source: UUID get() = edge.source
    override val target: UUID get() = edge.target
    override val role: String get() = edge.role
    override val type: String? get() = edge.type
    override val schemaVersion: String? get() = edge.schemaVersion
    override val properties: MutableMap<String, Any?>? get() = edge.properties
    override fun toDomain(): Edge = edge
}
