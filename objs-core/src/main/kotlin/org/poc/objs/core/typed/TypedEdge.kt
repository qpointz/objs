package org.poc.objs.core.typed

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMPropertiesPolicy
import java.util.UUID

/**
 * Typed edge with optional property payload, converted to [BoMEdge] at the boundary.
 */
class TypedEdge<R : Any>(
    val meta: TypedEdgeMeta,
    val propertiesType: Class<R>? = null,
    var id: UUID? = null,
    var properties: R? = null,
) {
    fun toBoMEdge(sourceId: UUID, targetId: UUID): BoMEdge {
        val props: MutableMap<String, Any?>? = when {
            meta.propertiesPolicy == BoMPropertiesPolicy.NONE -> null
            properties == null -> if (meta.emptyPropertiesAllowed) mutableMapOf() else null
            else -> {
                val map = PayloadMapper.toMap(properties!!)
                map.entries.removeIf { it.value == null }
                if (map.isEmpty() && meta.emptyPropertiesAllowed) mutableMapOf() else map
            }
        }
        return BoMEdge(
            id = id,
            source = sourceId,
            target = targetId,
            role = meta.role,
            type = meta.propertiesMeta?.type,
            schemaVersion = meta.propertiesMeta?.schemaVersion,
            properties = props,
        )
    }
}
