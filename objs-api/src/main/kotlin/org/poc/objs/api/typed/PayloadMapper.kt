package org.poc.objs.api.typed

import tools.jackson.databind.ObjectMapper

/**
 * Jackson-backed payload codec supplied by the consuming application.
 *
 * The API never creates or configures an ObjectMapper; callers retain control over modules,
 * naming, null handling, and unknown-property behavior.
 */
class PayloadMapper(
    val mapper: ObjectMapper,
) {
    @Suppress("UNCHECKED_CAST")
    fun toMap(value: Any): MutableMap<String, Any?> {
        val map = mapper.convertValue(value, MutableMap::class.java) as MutableMap<String, Any?>
        map.entries.removeIf { it.value == null }
        return map
    }

    fun <T> fromMap(map: Map<String, Any?>, type: Class<T>): T =
        mapper.convertValue(map, type)
}
