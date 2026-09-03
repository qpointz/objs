package org.poc.objs.core.typed

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * Shared Jackson mapper for typed payload ↔ map conversion in persistence/seed apply code.
 * API consumers use [org.poc.objs.api.typed.PayloadMapper] (instance) instead.
 */
object DefaultPayloadMapper {
    val mapper: JsonMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    @Suppress("UNCHECKED_CAST")
    fun toMap(value: Any): MutableMap<String, Any?> {
        val map = mapper.convertValue(value, MutableMap::class.java) as MutableMap<String, Any?>
        map.entries.removeIf { it.value == null }
        return map
    }

    fun <T> fromMap(map: Map<String, Any?>, type: Class<T>): T =
        mapper.convertValue(map, type)
}
