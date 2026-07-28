package org.poc.objs.sbom.model

import org.poc.objs.core.typed.EntityTypeMeta
import org.poc.objs.core.typed.TypedEntity

const val SCHEMA_VERSION = "1.0.0"

data class ComponentPayload(
    val name: String,
    val version: String,
    val ecosystem: String,
    val kind: String,
    val coordinates: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object ComponentType {
    val meta = EntityTypeMeta(type = "Component", schemaVersion = SCHEMA_VERSION)

    fun entity(
        payload: ComponentPayload,
        id: java.util.UUID? = null,
        annotations: MutableMap<String, String> = mutableMapOf(),
    ): TypedEntity<ComponentPayload> = TypedEntity(
        meta = meta,
        payloadType = ComponentPayload::class.java,
        id = id,
        payload = payload,
        annotations = annotations,
    )
}

/** Shared canonical edge properties (G-S34). */
data class CanonicalEdgePayload(
    val createdAt: String? = null,
    val source: String? = null,
    val confidence: Double? = null,
    val attributes: Map<String, Any?>? = null,
)

object CanonicalEdgeType {
    val meta = EntityTypeMeta(type = "CanonicalEdge", schemaVersion = SCHEMA_VERSION)
}
