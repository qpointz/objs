package org.poc.objs.core.typed

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaField
import org.poc.objs.core.domain.BoMSchemaNode
import org.poc.objs.core.domain.BoMSchemaType
import org.poc.objs.core.domain.BoMSchemaUsage

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
        fun objectSchema(
            type: String,
            version: String,
            title: String = type,
            description: String = "$type payload",
            fields: List<BoMSchemaField>,
            usages: Set<BoMSchemaUsage> = setOf(BoMSchemaUsage.ENTITY),
        ): BoMSchema = BoMSchema(
            type = type,
            version = version,
            contentSchema = BoMSchemaNode(
                type = BoMSchemaType.OBJECT,
                title = title,
                description = description,
                fields = fields,
            ),
            usages = usages,
        )
    }
}
