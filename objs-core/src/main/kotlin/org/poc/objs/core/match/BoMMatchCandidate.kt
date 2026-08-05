package org.poc.objs.core.match

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import java.util.UUID

/**
 * Lightweight entity row for matching. Scalar fields are always available; JSON maps may be
 * raw/lazy and deserialize only when accessed.
 */
interface BoMEntityMatchCandidate {
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

    fun toDomain(): BoMEntity = BoMEntity(
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
interface BoMEdgeMatchCandidate {
    val id: UUID?
    val source: UUID
    val target: UUID
    val role: String
    val type: String?
    val schemaVersion: String?
    val properties: MutableMap<String, Any?>?

    fun toDomain(): BoMEdge = BoMEdge(
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
class BoMEntityDomainCandidate(
    private val entity: BoMEntity,
) : BoMEntityMatchCandidate {
    override val id: UUID? get() = entity.id
    override val type: String get() = entity.type
    override val schemaVersion: String get() = entity.schemaVersion
    override val annotations: MutableMap<String, String> get() = entity.annotations
    override val payload: MutableMap<String, Any?> get() = entity.payload
    override fun toDomain(): BoMEntity = entity
}

/** Eager candidate wrapping an existing domain edge. */
class BoMEdgeDomainCandidate(
    private val edge: BoMEdge,
) : BoMEdgeMatchCandidate {
    override val id: UUID? get() = edge.id
    override val source: UUID get() = edge.source
    override val target: UUID get() = edge.target
    override val role: String get() = edge.role
    override val type: String? get() = edge.type
    override val schemaVersion: String? get() = edge.schemaVersion
    override val properties: MutableMap<String, Any?>? get() = edge.properties
    override fun toDomain(): BoMEdge = edge
}
