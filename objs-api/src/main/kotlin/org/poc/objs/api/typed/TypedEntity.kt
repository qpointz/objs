package org.poc.objs.api.typed

import org.poc.objs.api.domain.EdgeCardinality
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.PropertiesPolicy
import java.util.UUID

/**
 * Typed view over an [Entity]. The payload is application-owned and is converted only at the
 * boundary using the caller-supplied [PayloadMapper].
 */
open class TypedEntity<P : Any>(
    val meta: EntityTypeMeta,
    val payloadType: Class<P>,
    var id: UUID? = null,
    var payload: P,
    var annotations: MutableMap<String, String> = mutableMapOf(),
) {
    fun annotate(key: String, value: String): TypedEntity<P> {
        annotations[key] = value
        return this
    }

    fun withAnnotations(map: Map<String, String>): TypedEntity<P> {
        annotations.putAll(map)
        return this
    }

    fun toEntity(mapper: PayloadMapper): Entity = Entity(
        id = id,
        type = meta.type,
        schemaVersion = meta.schemaVersion,
        payload = mapper.toMap(payload),
        annotations = annotations.toMutableMap(),
    )

    fun syncFrom(entity: Entity, mapper: PayloadMapper) {
        require(entity.type == meta.type) {
            "Expected type ${meta.type}, got ${entity.type}"
        }
        require(entity.schemaVersion == meta.schemaVersion) {
            "Expected schemaVersion ${meta.schemaVersion}, got ${entity.schemaVersion}"
        }
        id = entity.id
        payload = mapper.fromMap(entity.payload, payloadType)
        annotations = entity.annotations.toMutableMap()
    }

    companion object {
        fun <P : Any> fromEntity(
            entity: Entity,
            meta: EntityTypeMeta,
            payloadType: Class<P>,
            mapper: PayloadMapper,
        ): TypedEntity<P> {
            require(entity.type == meta.type) {
                "Expected type ${meta.type}, got ${entity.type}"
            }
            require(entity.schemaVersion == meta.schemaVersion) {
                "Expected schemaVersion ${meta.schemaVersion}, got ${entity.schemaVersion}"
            }
            return TypedEntity(
                meta = meta,
                payloadType = payloadType,
                id = entity.id,
                payload = mapper.fromMap(entity.payload, payloadType),
                annotations = entity.annotations.toMutableMap(),
            )
        }
    }
}

/** Vocabulary-agnostic annotation merge: [defaults] then [overrides] win. */
fun mergeAnnotations(
    defaults: Map<String, String>,
    overrides: Map<String, String> = emptyMap(),
): MutableMap<String, String> =
    (defaults + overrides).toMutableMap()

data class EntityTypeMeta(
    val type: String,
    val schemaVersion: String,
    val schemaResource: String? = null,
)

data class TypedEdgeMeta(
    val role: String,
    val sourceType: String,
    val targetType: String,
    val propertiesPolicy: PropertiesPolicy = PropertiesPolicy.NONE,
    val propertiesMeta: EntityTypeMeta? = null,
    val emptyPropertiesAllowed: Boolean = true,
    val cardinality: EdgeCardinality = EdgeCardinality.UNSPECIFIED,
)
