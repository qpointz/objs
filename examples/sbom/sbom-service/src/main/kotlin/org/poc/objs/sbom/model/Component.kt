package org.poc.objs.sbom.model

import org.poc.objs.api.typed.EntityTypeMeta
import org.poc.objs.api.typed.TypedEntity

const val SCHEMA_VERSION = "1.0.0"
const val COMPONENT_SCHEMA_V2 = "2.0.0"

data class ComponentPayload(
    val name: String,
    val version: String,
    val ecosystem: String,
    val kind: String,
    val coordinates: String? = null,
    val description: String? = null,
)

/** Lane A latest Component shape (C-35). */
data class ComponentPayloadV2(
    val displayName: String,
    val version: String,
    val ecosystem: String,
    val kind: String,
    val coordinates: String? = null,
    val description: String? = null,
    val tier: String? = null,
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

object ComponentTypeV2 {
    val meta = EntityTypeMeta(type = "Component", schemaVersion = COMPONENT_SCHEMA_V2)

    fun entity(
        payload: ComponentPayloadV2,
        id: java.util.UUID? = null,
        annotations: MutableMap<String, String> = mutableMapOf(),
    ): TypedEntity<ComponentPayloadV2> = TypedEntity(
        meta = meta,
        payloadType = ComponentPayloadV2::class.java,
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
)

object CanonicalEdgeType {
    val meta = EntityTypeMeta(type = "CanonicalEdge", schemaVersion = SCHEMA_VERSION)
}
