package org.poc.objs.core.domain

import org.springframework.stereotype.Service

/**
 * Builds a single JSON Schema 2020-12 document for the full ontology catalog:
 * latest ENTITY type per name in `$defs`, with directed allow-list edges as
 * relation properties on the source type (singular vs array by cardinality).
 */
@Service
class FullCatalogJsonSchemaExporter(
    private val schemas: BoMSchemaCatalog,
    private val edgeRules: BoMAllowedEdgeCatalog,
) {
    fun export(): Map<String, Any?> {
        val latestEntities = latestByType(schemas.all().filter { BoMSchemaUsage.ENTITY in it.usages })
        val defKeyByType = latestEntities.keys.associateWith { jsonSchemaDefKey(it) }

        val defs = linkedMapOf<String, Any?>()
        for ((type, schema) in latestEntities.toSortedMap()) {
            val projected = BoMJsonSchema.from(schema).toMutableMap()
            projected.remove("\$schema")
            val properties = (projected["properties"] as? MutableMap<String, Any?>)
                ?: linkedMapOf<String, Any?>().also { projected["properties"] = it }
            // Ensure mutable map for additive relation props
            val mutableProps = properties.toMutableMap()
            projected["properties"] = mutableProps

            for (rule in edgeRules.all()) {
                if (rule.sourceType != type) continue
                if (rule.sourceType == BoMAllowedEdgeRule.ANY || rule.targetType == BoMAllowedEdgeRule.ANY) {
                    continue
                }
                val targetDef = defKeyByType[rule.targetType] ?: continue
                val propName = relationPropertyName(rule.role, rule.targetType)
                mutableProps[propName] = relationPropertySchema(rule, targetDef)
            }
            defs[defKeyByType.getValue(type)] = projected
        }

        // Include latest edge-property schemas referenced by concrete rules.
        val edgePropKeys = edgeRules.all()
            .filter {
                it.propertiesPolicy == BoMPropertiesPolicy.SCHEMA &&
                    it.propertiesSchemaType != null &&
                    it.sourceType != BoMAllowedEdgeRule.ANY &&
                    it.targetType != BoMAllowedEdgeRule.ANY
            }
            .mapNotNull { rule ->
                val t = rule.propertiesSchemaType ?: return@mapNotNull null
                val v = rule.propertiesSchemaVersion
                if (v != null) schemas.get(t, v) else latestByType(
                    schemas.listByType(t).filter { BoMSchemaUsage.EDGE_PROPERTIES in it.usages },
                )[t]
            }
            .distinctBy { it.key }

        for (schema in edgePropKeys.sortedWith(compareBy({ it.type }, { it.version }))) {
            val key = jsonSchemaDefKey(schema.type)
            if (key in defs) continue
            val projected = BoMJsonSchema.from(schema).toMutableMap()
            projected.remove("\$schema")
            defs[key] = projected
        }

        return linkedMapOf(
            "\$schema" to BoMJsonSchema.DIALECT,
            "title" to "Objs full catalog",
            "description" to "Latest ENTITY object types with allow-list relations as properties",
            "x-objs-export" to "full-catalog",
            "\$defs" to defs,
        )
    }

    private fun relationPropertySchema(rule: BoMAllowedEdgeRule, targetDefKey: String): Map<String, Any?> {
        val ref = linkedMapOf<String, Any?>("\$ref" to "#/\$defs/$targetDefKey")
        val base = linkedMapOf<String, Any?>(
            "title" to "${rule.role} → ${rule.targetType}",
            "description" to "Allow-list relation ${rule.role} to ${rule.targetType} (${rule.cardinality.wire})",
            "x-objs-role" to rule.role,
            "x-objs-target-type" to rule.targetType,
            "x-objs-cardinality" to rule.cardinality.wire,
        )
        return if (rule.cardinality.isSingular) {
            base + ref
        } else {
            base + linkedMapOf(
                "type" to "array",
                "items" to ref,
            )
        }
    }

    companion object {
        fun latestByType(schemas: Collection<BoMSchema>): Map<String, BoMSchema> =
            schemas
                .groupBy { it.type }
                .mapValues { (_, versions) -> versions.maxBy { it.version } }

        /** `$defs` key / PascalCase identifier for a catalog type name. */
        fun jsonSchemaDefKey(type: String): String =
            type.split(Regex("[^A-Za-z0-9]+"))
                .filter { it.isNotEmpty() }
                .joinToString("") { part ->
                    part.replaceFirstChar { ch -> ch.titlecaseChar() }
                }
                .ifEmpty { "Type" }

        /** camelCase(role + PascalCase(targetType)), e.g. CONTAINS + Component → containsComponent. */
        fun relationPropertyName(role: String, targetType: String): String {
            val roleParts = role.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
            val targetPascal = jsonSchemaDefKey(targetType)
            val roleCamel = when {
                roleParts.isEmpty() -> "rel"
                else -> roleParts.mapIndexed { index, part ->
                    val lower = part.lowercase()
                    if (index == 0) lower else lower.replaceFirstChar { it.titlecaseChar() }
                }.joinToString("")
            }
            return roleCamel + targetPascal
        }
    }
}
