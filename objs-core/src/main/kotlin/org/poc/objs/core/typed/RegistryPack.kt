package org.poc.objs.core.typed

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import tools.jackson.databind.JsonNode
import java.io.InputStream

/**
 * Bundle of schemas and allow-list rules for registration into in-memory catalogs.
 */
data class RegistryPack(
    val schemas: List<BoMSchema> = emptyList(),
    val edgeRules: List<BoMAllowedEdgeRule> = emptyList(),
) {
    fun registerInto(schemasCatalog: BoMSchemaCatalog, edgesCatalog: BoMAllowedEdgeCatalog) {
        schemas.forEach { schemasCatalog.register(it) }
        edgeRules.forEach { edgesCatalog.register(it) }
    }

    operator fun plus(other: RegistryPack): RegistryPack = RegistryPack(
        schemas = schemas + other.schemas,
        edgeRules = edgeRules + other.edgeRules,
    )

    companion object {
        fun schemaFromClasspath(type: String, version: String, resourcePath: String): BoMSchema {
            val stream = requireNotNull(
                RegistryPack::class.java.classLoader.getResourceAsStream(resourcePath)
                    ?: Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath),
            ) { "Schema resource not found: $resourcePath" }
            return schemaFromStream(type, version, stream)
        }

        fun schemaFromStream(type: String, version: String, stream: InputStream): BoMSchema {
            stream.use {
                val tree: JsonNode = PayloadMapper.mapper.readTree(it)
                @Suppress("UNCHECKED_CAST")
                val map = PayloadMapper.mapper.convertValue(tree, MutableMap::class.java) as Map<String, Any?>
                return BoMSchema(type = type, version = version, schema = map)
            }
        }

        fun objectSchema(
            type: String,
            version: String,
            required: List<String>,
            properties: Map<String, Map<String, Any?>>,
            additionalProperties: Boolean = true,
        ): BoMSchema = BoMSchema(
            type = type,
            version = version,
            schema = mapOf(
                "type" to "object",
                "required" to required,
                "additionalProperties" to additionalProperties,
                "properties" to properties,
            ),
        )
    }
}
