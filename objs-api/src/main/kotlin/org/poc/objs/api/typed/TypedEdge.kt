package org.poc.objs.api.typed

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.PropertiesPolicy
import java.util.UUID

/** Typed edge with optional property payload, converted at the API boundary. */
class TypedEdge<R : Any>(
    val meta: TypedEdgeMeta,
    val propertiesType: Class<R>? = null,
    var id: UUID? = null,
    var properties: R? = null,
) {
    fun toEdge(sourceId: UUID, targetId: UUID, mapper: PayloadMapper): Edge {
        val props: MutableMap<String, Any?>? = when {
            meta.propertiesPolicy == PropertiesPolicy.NONE -> null
            properties == null -> if (meta.emptyPropertiesAllowed) mutableMapOf() else null
            else -> {
                val map = mapper.toMap(properties!!)
                map.entries.removeIf { it.value == null }
                if (map.isEmpty() && meta.emptyPropertiesAllowed) mutableMapOf() else map
            }
        }
        return Edge(
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
