package org.poc.objs.core.typed

import org.poc.objs.core.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.core.domain.Schema
import org.poc.objs.core.domain.SchemaCatalog
import org.poc.objs.core.domain.SchemaField
import org.poc.objs.core.domain.SchemaNode
import org.poc.objs.core.domain.SchemaType
import org.poc.objs.core.domain.SchemaUsage

/**
 * Bundle of schemas and allow-list rules for registration into in-memory catalogs.
 */
data class RegistryPack(
    val schemas: List<Schema> = emptyList(),
    val edgeRules: List<AllowedEdgeRule> = emptyList(),
) {
    fun registerInto(schemasCatalog: SchemaCatalog, edgesCatalog: AllowedEdgeCatalog) {
        schemas.forEach { schemasCatalog.register(it) }
        edgeRules.forEach { edgesCatalog.register(it) }
    }

    operator fun plus(other: RegistryPack): RegistryPack = RegistryPack(
        schemas = schemas + other.schemas,
        edgeRules = edgeRules + other.edgeRules,
    )

    companion object {
        fun objectSchema(
            type: String,
            version: String,
            title: String = type,
            description: String = "$type payload",
            fields: List<SchemaField>,
            usage: SchemaUsage = SchemaUsage.ENTITY,
            attributes: Map<String, String> = emptyMap(),
        ): Schema = Schema(
            type = type,
            version = version,
            contentSchema = SchemaNode(
                type = SchemaType.OBJECT,
                title = title,
                description = description,
                fields = fields,
            ),
            usage = usage,
            attributes = attributes,
        )
    }
}
