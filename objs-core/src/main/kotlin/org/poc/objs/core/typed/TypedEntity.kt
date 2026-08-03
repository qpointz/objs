package org.poc.objs.core.typed

import org.poc.objs.core.domain.BoMEdgeCardinality
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMPropertiesPolicy
import java.util.UUID

/**
 * Typed view over a [BoMEntity]: composition, not subclassing.
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

    fun toBoMEntity(): BoMEntity = BoMEntity(
        id = id,
        type = meta.type,
        schemaVersion = meta.schemaVersion,
        payload = PayloadMapper.toMap(payload),
        annotations = annotations.toMutableMap(),
    )

    fun syncFrom(entity: BoMEntity) {
        require(entity.type == meta.type) {
            "Expected type ${meta.type}, got ${entity.type}"
        }
        require(entity.schemaVersion == meta.schemaVersion) {
            "Expected schemaVersion ${meta.schemaVersion}, got ${entity.schemaVersion}"
        }
        id = entity.id
        payload = PayloadMapper.fromMap(entity.payload, payloadType)
        annotations = entity.annotations.toMutableMap()
    }

    companion object {
        fun <P : Any> fromBoMEntity(
            entity: BoMEntity,
            meta: EntityTypeMeta,
            payloadType: Class<P>,
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
                payload = PayloadMapper.fromMap(entity.payload, payloadType),
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

data class TypedEdgeMeta(
    val role: String,
    val sourceType: String,
    val targetType: String,
    val propertiesPolicy: BoMPropertiesPolicy = BoMPropertiesPolicy.NONE,
    val propertiesMeta: EntityTypeMeta? = null,
    val emptyPropertiesAllowed: Boolean = true,
    val cardinality: BoMEdgeCardinality = BoMEdgeCardinality.UNSPECIFIED,
)
