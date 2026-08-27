package org.poc.objs.core.domain

import org.springframework.stereotype.Service

/**
 * Builds a single JSON Schema document for the full ontology catalog:
 * latest ENTITY type per name in `$defs` / `definitions`, with optional allow-list edges as
 * relation properties (outbound and optionally inverse / linked).
 */
@Service
class FullCatalogJsonSchemaExporter(
    private val schemas: BoMSchemaCatalog,
    private val edgeRules: BoMAllowedEdgeCatalog,
) {
    fun export(options: BoMJsonSchemaExportOptions = BoMJsonSchemaExportOptions.DEFAULT): Map<String, Any?> {
        val latestEntities = latestByType(schemas.all().filter { it.usage == BoMSchemaUsage.ENTITY })
        val defKeyByType = latestEntities.keys.associateWith { jsonSchemaDefKey(it) }
        val dialect = options.dialect

        val defs = linkedMapOf<String, Any?>()
        for ((type, schema) in latestEntities.toSortedMap()) {
            val projected = BoMJsonSchema.from(schema).toMutableMap()
            projected.remove("\$schema")
            val properties = (projected["properties"] as? MutableMap<String, Any?>)
                ?: linkedMapOf<String, Any?>().also { projected["properties"] = it }
            val mutableProps = properties.toMutableMap()
            projected["properties"] = mutableProps

            if (options.includeEdges != BoMJsonSchemaEdgeInclusion.NONE) {
                for (rule in edgeRules.all()) {
                    if (rule.sourceType != type) continue
                    if (rule.sourceType == BoMAllowedEdgeRule.ANY || rule.targetType == BoMAllowedEdgeRule.ANY) {
                        continue
                    }
                    val targetDef = defKeyByType[rule.targetType] ?: continue
                    val propName = relationPropertyName(rule.role, rule.targetType)
                    mutableProps[propName] = outboundRelationPropertySchema(rule, targetDef, dialect)
                }
            }

            if (options.includeEdges == BoMJsonSchemaEdgeInclusion.LINKED) {
                for (rule in edgeRules.all()) {
                    if (rule.targetType != type) continue
                    if (rule.sourceType == BoMAllowedEdgeRule.ANY || rule.targetType == BoMAllowedEdgeRule.ANY) {
                        continue
                    }
                    val sourceDef = defKeyByType[rule.sourceType] ?: continue
                    val propName = inverseRelationPropertyName(rule.role, rule.sourceType)
                    mutableProps[propName] = inverseRelationPropertySchema(rule, sourceDef, dialect)
                }
            }

            defs[defKeyByType.getValue(type)] = projected
        }

        if (
            options.includeEdges != BoMJsonSchemaEdgeInclusion.NONE &&
            options.includeEdgePropertySchemas
        ) {
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
                        schemas.listByType(t).filter { it.usage == BoMSchemaUsage.EDGE_PROPERTIES },
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
        }

        return linkedMapOf(
            "\$schema" to dialect.schemaUri,
            "title" to "Objs full catalog",
            "description" to "Latest ENTITY object types with allow-list relations as properties",
            "x-objs-export" to "full-catalog",
            "x-objs-json-schema-options" to options.toWireMap(),
            dialect.defsKeyword to defs,
        )
    }

    /**
     * Same catalog as [export], shaped for object-model codegen tools (e.g. jsonschema2pojo):
     * a synthetic root `type: object` whose properties `$ref` every catalog def entry, and each
     * def `title` set to the def key so class names stay PascalCase identifiers.
     */
    fun exportForCodegen(options: BoMJsonSchemaExportOptions = BoMJsonSchemaExportOptions.DEFAULT): Map<String, Any?> {
        val base = export(options)
        val dialect = options.dialect
        @Suppress("UNCHECKED_CAST")
        val rawDefs = base[dialect.defsKeyword] as Map<String, Any?>
        val defs = linkedMapOf<String, Any?>()
        for ((key, node) in rawDefs) {
            val projected = (node as Map<String, Any?>).toMutableMap()
            projected["title"] = key
            defs[key] = projected
        }
        val properties = linkedMapOf<String, Any?>()
        for (name in defs.keys.sorted()) {
            properties[name] = linkedMapOf("\$ref" to "${dialect.defsRefPrefix}$name")
        }
        return linkedMapOf(
            "\$schema" to base["\$schema"],
            "title" to "ObjsCatalog",
            "description" to
                "Codegen root: each property refs a catalog def (jsonschema2pojo-ready)",
            "type" to "object",
            "properties" to properties,
            "additionalProperties" to false,
            "x-objs-export" to "full-catalog-codegen",
            "x-objs-json-schema-options" to base["x-objs-json-schema-options"],
            dialect.defsKeyword to defs,
        )
    }

    private fun outboundRelationPropertySchema(
        rule: BoMAllowedEdgeRule,
        targetDefKey: String,
        dialect: BoMJsonSchemaDialect,
    ): Map<String, Any?> {
        val refTarget = linkedMapOf<String, Any?>("\$ref" to "${dialect.defsRefPrefix}$targetDefKey")
        val base = linkedMapOf<String, Any?>(
            "title" to "${rule.role} → ${rule.targetType}",
            "description" to "Allow-list relation ${rule.role} to ${rule.targetType} (${rule.cardinality.wire})",
            "x-objs-role" to rule.role,
            "x-objs-target-type" to rule.targetType,
            "x-objs-cardinality" to rule.cardinality.wire,
            "x-objs-direction" to "outbound",
        )
        return if (rule.cardinality.isSingular) {
            mergeRef(base, refTarget, dialect)
        } else {
            base + linkedMapOf(
                "type" to "array",
                "items" to refTarget,
            )
        }
    }

    private fun inverseRelationPropertySchema(
        rule: BoMAllowedEdgeRule,
        sourceDefKey: String,
        dialect: BoMJsonSchemaDialect,
    ): Map<String, Any?> {
        val refTarget = linkedMapOf<String, Any?>("\$ref" to "${dialect.defsRefPrefix}$sourceDefKey")
        val inverseSingular = !rule.cardinality.isSingular
        val inverseCardWire = if (inverseSingular) BoMEdgeCardinality.ONE_TO_ONE.wire else BoMEdgeCardinality.ONE_TO_MANY.wire
        val base = linkedMapOf<String, Any?>(
            "title" to "${rule.role} ← ${rule.sourceType}",
            "description" to
                "Inverse of allow-list relation ${rule.role} from ${rule.sourceType} (${rule.cardinality.wire})",
            "x-objs-role" to rule.role,
            "x-objs-source-type" to rule.sourceType,
            "x-objs-cardinality" to inverseCardWire,
            "x-objs-direction" to "inbound",
        )
        return if (inverseSingular) {
            mergeRef(base, refTarget, dialect)
        } else {
            base + linkedMapOf(
                "type" to "array",
                "items" to refTarget,
            )
        }
    }

    /** Combine metadata with a `$ref` target; draft-07 uses `allOf` so siblings stay meaningful. */
    private fun mergeRef(
        metadata: Map<String, Any?>,
        refTarget: Map<String, Any?>,
        dialect: BoMJsonSchemaDialect,
    ): Map<String, Any?> =
        if (dialect.exclusiveRef) {
            metadata + linkedMapOf("allOf" to listOf(refTarget))
        } else {
            metadata + refTarget
        }

    companion object {
        fun latestByType(schemas: Collection<BoMSchema>): Map<String, BoMSchema> =
            schemas
                .groupBy { it.type }
                .mapValues { (_, versions) -> versions.maxBy { it.version } }

        /** Def key / PascalCase identifier for a catalog type name. */
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

        /** camelCase(role + "From" + PascalCase(sourceType)), e.g. CONTAINS + Database → containsFromDatabase. */
        fun inverseRelationPropertyName(role: String, sourceType: String): String {
            val roleParts = role.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
            val sourcePascal = jsonSchemaDefKey(sourceType)
            val roleCamel = when {
                roleParts.isEmpty() -> "rel"
                else -> roleParts.mapIndexed { index, part ->
                    val lower = part.lowercase()
                    if (index == 0) lower else lower.replaceFirstChar { it.titlecaseChar() }
                }.joinToString("")
            }
            return roleCamel + "From" + sourcePascal
        }
    }
}
