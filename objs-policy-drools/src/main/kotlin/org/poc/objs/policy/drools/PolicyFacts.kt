package org.poc.objs.policy.drools

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import java.util.UUID

/**
 * Drools fact for a graph [Entity] (evaluation projection).
 *
 * [schema] mirrors catalog schema identity; when projecting from domain it equals [type].
 */
class EntityFact(
    val id: UUID?,
    val type: String,
    val schema: String,
    val schemaVersion: String,
    val annotations: Map<String, String>,
    val payload: Map<String, Any?> = emptyMap(),
) {
    operator fun get(key: String): Any? = payload[key]

    companion object {
        fun from(entity: Entity): EntityFact =
            EntityFact(
                id = entity.id,
                type = entity.type,
                schema = entity.type,
                schemaVersion = entity.schemaVersion,
                annotations = entity.annotations.toMap(),
                payload = entity.payload.toMap(),
            )
    }
}

/**
 * Drools fact for a graph [Edge] (evaluation projection).
 *
 * Domain edges have no annotations bag; [annotations] is present for a uniform metadata shape
 * (empty when projecting from [Edge]).
 */
class EdgeFact(
    val id: UUID?,
    val source: UUID,
    val target: UUID,
    val role: String,
    val type: String?,
    val schema: String?,
    val schemaVersion: String?,
    val annotations: Map<String, String> = emptyMap(),
    val properties: Map<String, Any?> = emptyMap(),
) {
    operator fun get(key: String): Any? = properties[key]

    companion object {
        fun from(edge: Edge): EdgeFact =
            EdgeFact(
                id = edge.id,
                source = edge.source,
                target = edge.target,
                role = edge.role,
                type = edge.type,
                schema = edge.type,
                schemaVersion = edge.schemaVersion,
                annotations = emptyMap(),
                properties = edge.properties?.toMap() ?: emptyMap(),
            )
    }
}

/**
 * Named bag fact for wired [org.poc.objs.policy.api.PolicyEvaluationContext.facts] entries
 * (and other non-graph objects). Replaces the former `FactMap` name.
 */
class ObjectFact(
    val name: String,
    val values: Map<String, Any?>,
) {
    operator fun get(key: String): Any? = values[key]
}
