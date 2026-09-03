package org.poc.objs.api.domain

/**
 * Builds a single JSON Schema document for the full ontology catalog:
 * latest ENTITY type per name in `$defs` / `definitions`, with optional allow-list edges as
 * relation properties (outbound and optionally inverse / linked).
 */
class FullCatalogJsonSchemaExporter(
    private val schemas: SchemaCatalog,
    private val edgeRules: AllowedEdgeCatalog,
) {
    fun export(options: JsonSchemaExportOptions = JsonSchemaExportOptions.DEFAULT): Map<String, Any?> {
        val latestEntities = latestByType(schemas.all().filter { it.usage == SchemaUsage.ENTITY })
        val defKeyByType = latestEntities.keys.associateWith { jsonSchemaDefKey(it) }
        val dialect = options.dialect

        val defs = linkedMapOf<String, Any?>()
        for ((type, schema) in latestEntities.toSortedMap()) {
            val projected = JsonSchema.from(schema).toMutableMap()
            projected.remove("\$schema")
            val properties = (projected["properties"] as? MutableMap<String, Any?>)
                ?: linkedMapOf<String, Any?>().also { projected["properties"] = it }
            val mutableProps = properties.toMutableMap()
            projected["properties"] = mutableProps

            if (options.includeEdges != JsonSchemaEdgeInclusion.NONE) {
                for (rule in edgeRules.all()) {
                    if (rule.sourceType != type) continue
                    if (rule.sourceType == AllowedEdgeRule.ANY || rule.targetType == AllowedEdgeRule.ANY) {
                        continue
                    }
                    val targetDef = defKeyByType[rule.targetType] ?: continue
                    val propName = relationPropertyName(rule.role, rule.targetType)
                    mutableProps[propName] = outboundRelationPropertySchema(rule, targetDef, dialect)
                }
            }

            if (options.includeEdges == JsonSchemaEdgeInclusion.LINKED) {
                for (rule in edgeRules.all()) {
                    if (rule.targetType != type) continue
                    if (rule.sourceType == AllowedEdgeRule.ANY || rule.targetType == AllowedEdgeRule.ANY) {
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
            options.includeEdges != JsonSchemaEdgeInclusion.NONE &&
            options.includeEdgePropertySchemas
        ) {
            val edgePropKeys = edgeRules.all()
                .filter {
                    it.propertiesPolicy == PropertiesPolicy.SCHEMA &&
                        it.propertiesSchemaType != null &&
                        it.sourceType != AllowedEdgeRule.ANY &&
                        it.targetType != AllowedEdgeRule.ANY
                }
                .mapNotNull { rule ->
                    val t = rule.propertiesSchemaType ?: return@mapNotNull null
                    val v = rule.propertiesSchemaVersion
                    if (v != null) schemas.get(t, v) else latestByType(
                        schemas.listByType(t).filter { it.usage == SchemaUsage.EDGE_PROPERTIES },
                    )[t]
                }
                .distinctBy { it.key }

            for (schema in edgePropKeys.sortedWith(compareBy({ it.type }, { it.version }))) {
                val key = jsonSchemaDefKey(schema.type)
                if (key in defs) continue
                val projected = JsonSchema.from(schema).toMutableMap()
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
    fun exportForCodegen(options: JsonSchemaExportOptions = JsonSchemaExportOptions.DEFAULT): Map<String, Any?> {
        val base = export(options)
        val dialect = options.dialect
        @Suppress("UNCHECKED_CAST")
        val rawDefs = base[dialect.defsKeyword] as Map<String, Any?>
        val defs = linkedMapOf<String, Any?>()
        val latestEntities = latestByType(schemas.all().filter { it.usage == SchemaUsage.ENTITY })
        val latestEdgeProperties = latestByType(
            schemas.all().filter { it.usage == SchemaUsage.EDGE_PROPERTIES },
        )
        val definitionMetadata = codegenDefinitionMetadata(
            latestEntities = latestEntities,
            latestEdgeProperties = latestEdgeProperties,
        )
        for ((key, node) in rawDefs) {
            val projected = (node as Map<String, Any?>).toMutableMap()
            @Suppress("UNCHECKED_CAST")
            val properties = projected["properties"] as? Map<String, Any?> ?: emptyMap()
            projected["properties"] = properties.filterValues { value ->
                @Suppress("UNCHECKED_CAST")
                val metadata = value as? Map<String, Any?>
                metadata?.get("x-objs-direction") == null
            }
            val definition = definitionMetadata.firstOrNull { it["definitionKey"] == key }
            projected["title"] = definition?.get("javaTypeName") ?: key
            defs[key] = projected
        }
        val mutationDefinitions = mutationDefinitions(dialect)
        defs.putAll(mutationDefinitions)
        val properties = linkedMapOf<String, Any?>()
        for (name in rawDefs.keys.sorted()) {
            properties[name] = linkedMapOf("\$ref" to "${dialect.defsRefPrefix}$name")
        }
        val relationMetadata = codegenRelationMetadata(
            latestEntities = latestEntities,
            latestEdgeProperties = latestEdgeProperties,
        )
        val diagnostics = relationMetadata.second
        return linkedMapOf(
            "\$schema" to base["\$schema"],
            "title" to "ObjsCatalog",
            "description" to
                "Codegen root: payload definitions and Objs relation metadata",
            "type" to "object",
            "properties" to properties,
            "additionalProperties" to false,
            "x-objs-export" to "full-catalog-codegen",
            "x-objs-json-schema-options" to base["x-objs-json-schema-options"],
            dialect.defsKeyword to defs,
            "x-objs-relations" to relationMetadata.first,
            "x-objs-codegen" to linkedMapOf(
                "version" to 1,
                "language" to "java",
                "writeProfile" to linkedMapOf(
                    "entityPayload" to "payload-only",
                    "relationProperties" to "read-only",
                ),
                "definitions" to definitionMetadata,
                "schemas" to codegenSchemaVersionMetadata(),
                "mutations" to linkedMapOf(
                    "entity" to linkedMapOf("definitionKey" to "Entity"),
                    "edge" to linkedMapOf("definitionKey" to "Edge"),
                    "entityMutation" to linkedMapOf("definitionKey" to "EntityMutation"),
                    "edgeMutation" to linkedMapOf("definitionKey" to "EdgeMutation"),
                    "graphMutation" to linkedMapOf("definitionKey" to "GraphMutation"),
                ),
                "diagnostics" to diagnostics,
            ),
        )
    }

    private fun outboundRelationPropertySchema(
        rule: AllowedEdgeRule,
        targetDefKey: String,
        dialect: JsonSchemaDialect,
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
        rule: AllowedEdgeRule,
        sourceDefKey: String,
        dialect: JsonSchemaDialect,
    ): Map<String, Any?> {
        val refTarget = linkedMapOf<String, Any?>("\$ref" to "${dialect.defsRefPrefix}$sourceDefKey")
        val inverseSingular = !rule.cardinality.isSingular
        val inverseCardWire = if (inverseSingular) EdgeCardinality.ONE_TO_ONE.wire else EdgeCardinality.ONE_TO_MANY.wire
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
        dialect: JsonSchemaDialect,
    ): Map<String, Any?> =
        if (dialect.exclusiveRef) {
            metadata + linkedMapOf("allOf" to listOf(refTarget))
        } else {
            metadata + refTarget
        }

    private fun codegenDefinitionMetadata(
        latestEntities: Map<String, Schema>,
        latestEdgeProperties: Map<String, Schema>,
    ): List<Map<String, Any?>> {
        val generated = (latestEntities.values + latestEdgeProperties.values)
            .sortedWith(compareBy({ it.type }, { it.usage.name }, { it.version }))
        val definitionKeys = mutableMapOf<String, String>()
        val javaNames = mutableMapOf<String, String>()
        val definitions = mutableListOf<Map<String, Any?>>()

        for (schema in generated) {
            val definitionKey = jsonSchemaDefKey(schema.type)
            val keyOwner = definitionKeys.putIfAbsent(definitionKey, schema.type)
            if (keyOwner != null && keyOwner != schema.type) {
                throw JsonSchemaCodegenException(
                    "Definition key collision '$definitionKey' for types '$keyOwner' and '${schema.type}'",
                )
            }
            val javaTypeName = schemaJavaTypeName(schema)
            val javaOwner = javaNames.putIfAbsent(javaTypeName, "${schema.type}@${schema.version}")
            if (javaOwner != null && javaOwner != "${schema.type}@${schema.version}") {
                throw JsonSchemaCodegenException(
                    "Java type name collision '$javaTypeName' for '$javaOwner' and " +
                        "'${schema.type}@${schema.version}'",
                )
            }
            val latest = when (schema.usage) {
                SchemaUsage.ENTITY -> latestEntities[schema.type]?.version == schema.version
                SchemaUsage.EDGE_PROPERTIES -> latestEdgeProperties[schema.type]?.version == schema.version
            }
            val skip = schema.tags.any { it.trim() == "codegen.java.skip" }
            definitions += linkedMapOf(
                "definitionKey" to definitionKey,
                "kind" to schema.usage.name,
                "type" to schema.type,
                "schemaVersion" to schema.version,
                "latest" to latest,
                "generated" to (latest && !skip),
                "skip" to skip,
                "javaTypeName" to javaTypeName,
                "baseClass" to schemaBaseClass(schema),
                "interfaces" to schemaInterfaces(schema),
                "tags" to schema.tags,
                "attributes" to schema.attributes,
            )
        }
        return definitions
    }

    private fun codegenRelationMetadata(
        latestEntities: Map<String, Schema>,
        latestEdgeProperties: Map<String, Schema>,
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        val definitionsByType = latestEntities.mapValues { (_, schema) -> jsonSchemaDefKey(schema.type) }
        val methodOwners = mutableMapOf<String, String>()
        val diagnostics = mutableListOf<Map<String, Any?>>()
        val relations = mutableListOf<Map<String, Any?>>()

        for (rule in edgeRules.all().sortedWith(compareBy({ it.sourceType }, { it.role }, { it.targetType }))) {
            val sourceSchema = latestEntities[rule.sourceType]
            val targetSchema = latestEntities[rule.targetType]
            val sourceDefinition = definitionsByType[rule.sourceType]
            val targetDefinition = definitionsByType[rule.targetType]
            val property = relationPropertyMetadata(rule, latestEdgeProperties, diagnostics)
            val skip = rule.tags.any { it.trim() == "codegen.java.skip" }
            val noInverse = rule.tags.any { it.trim() == "codegen.java.noInverse" }
            val outboundMethod = relationMethod(rule, "codegen.java.outboundMethod") {
                relationPropertyName(rule.role, rule.targetType)
            }
            val inboundMethod = relationMethod(rule, "codegen.java.inboundMethod") {
                inverseRelationPropertyName(rule.role, rule.sourceType)
            }
            val baseClass = relationBaseClass(rule)
            val sourceStatic = staticRelationBinding(
                rule.sourceType,
                sourceSchema,
                rule.targetType,
                targetSchema,
                baseClass,
            ) && !skip
            val targetStatic = staticRelationBinding(
                rule.targetType,
                targetSchema,
                rule.sourceType,
                sourceSchema,
                baseClass,
            ) && !noInverse && !skip

            if (sourceStatic) registerMethod(methodOwners, rule.sourceType, outboundMethod, rule)
            if (targetStatic) registerMethod(methodOwners, rule.targetType, inboundMethod, rule)

            val outbound = linkedMapOf<String, Any?>(
                "direction" to "outbound",
                "role" to rule.role,
                "targetType" to rule.targetType,
                "targetDefinition" to targetDefinition,
                "targetSchemaVersion" to targetSchema?.version,
                "edgeProperty" to property,
                "method" to outboundMethod,
                "staticBinding" to sourceStatic,
                "readOnly" to false,
            )
            val inbound = if (noInverse) {
                null
            } else {
                linkedMapOf(
                    "direction" to "inbound",
                    "role" to rule.role,
                    "sourceType" to rule.sourceType,
                    "sourceDefinition" to sourceDefinition,
                    "sourceSchemaVersion" to sourceSchema?.version,
                    "edgeProperty" to property,
                    "method" to inboundMethod,
                    "staticBinding" to targetStatic,
                    "readOnly" to true,
                )
            }
            relations += linkedMapOf(
                "sourceType" to rule.sourceType,
                "sourceDefinition" to sourceDefinition,
                "sourceSchemaVersion" to sourceSchema?.version,
                "role" to rule.role,
                "targetType" to rule.targetType,
                "targetDefinition" to targetDefinition,
                "targetSchemaVersion" to targetSchema?.version,
                "propertiesPolicy" to rule.propertiesPolicy.name,
                "emptyPropertiesAllowed" to rule.emptyPropertiesAllowed,
                "propertySchema" to property,
                "cardinality" to rule.cardinality.wire,
                "description" to rule.description,
                "sourceVerb" to rule.sourceVerb,
                "targetVerb" to rule.targetVerb,
                "tags" to rule.tags,
                "attributes" to rule.attributes,
                "navigation" to linkedMapOf(
                    "outbound" to outbound,
                    "inbound" to inbound,
                ),
                "codegen" to linkedMapOf(
                    "skip" to skip,
                    "noInverse" to noInverse,
                    "baseClass" to baseClass,
                    "outboundMethod" to outboundMethod,
                    "inboundMethod" to inboundMethod,
                    "sourceStaticBinding" to sourceStatic,
                    "targetStaticBinding" to targetStatic,
                    "wildcardSource" to (rule.sourceType == AllowedEdgeRule.ANY),
                    "wildcardTarget" to (rule.targetType == AllowedEdgeRule.ANY),
                ),
            )
        }
        return relations to diagnostics
    }

    private fun codegenSchemaVersionMetadata(): List<Map<String, Any?>> =
        schemas.all()
            .sortedWith(compareBy({ it.type }, { it.usage.name }, { it.version }))
            .map { schema ->
                val latest = latestByType(schemas.all().filter { it.usage == schema.usage })[schema.type]
                val skip = schema.tags.any { it.trim() == "codegen.java.skip" }
                linkedMapOf(
                    "type" to schema.type,
                    "schemaVersion" to schema.version,
                    "kind" to schema.usage.name,
                    "definitionKey" to jsonSchemaDefKey(schema.type),
                    "latest" to (latest?.version == schema.version),
                    "skip" to skip,
                    "tags" to schema.tags,
                    "attributes" to schema.attributes,
                )
            }

    private fun relationPropertyMetadata(
        rule: AllowedEdgeRule,
        latestEdgeProperties: Map<String, Schema>,
        diagnostics: MutableList<Map<String, Any?>>,
    ): Map<String, Any?>? {
        if (rule.propertiesPolicy == PropertiesPolicy.NONE) return null
        val requestedType = rule.propertiesSchemaType
        val requestedVersion = rule.propertiesSchemaVersion
        if (requestedType.isNullOrBlank()) {
            diagnostics += relationDiagnostic(
                rule,
                "EDGE_PROPERTIES_SCHEMA_MISSING",
                "SCHEMA relation does not declare a properties schema type",
            )
            return linkedMapOf(
                "resolved" to false,
                "representation" to "generic",
                "type" to null,
                "schemaVersion" to requestedVersion,
                "definitionKey" to null,
            )
        }
        val resolved = if (requestedVersion == null) {
            latestEdgeProperties[requestedType]
        } else {
            schemas.get(requestedType, requestedVersion)
        }
        if (resolved == null) {
            diagnostics += relationDiagnostic(
                rule,
                "EDGE_PROPERTIES_SCHEMA_NOT_FOUND",
                "No edge-property schema for $requestedType@${requestedVersion ?: "latest"}",
            )
        } else if (resolved.usage != SchemaUsage.EDGE_PROPERTIES) {
            diagnostics += relationDiagnostic(
                rule,
                "EDGE_PROPERTIES_SCHEMA_WRONG_USAGE",
                "Schema ${resolved.type}@${resolved.version} is not EDGE_PROPERTIES",
            )
        }
        val isValid = resolved != null && resolved.usage == SchemaUsage.EDGE_PROPERTIES
        return linkedMapOf(
            "resolved" to isValid,
            "representation" to if (isValid) "schema" else "generic",
            "type" to requestedType,
            "schemaVersion" to (requestedVersion ?: resolved?.version),
            "definitionKey" to if (isValid) jsonSchemaDefKey(requestedType) else null,
        )
    }

    private fun relationDiagnostic(
        rule: AllowedEdgeRule,
        code: String,
        message: String,
    ): Map<String, Any?> = linkedMapOf(
        "code" to code,
        "message" to message,
        "sourceType" to rule.sourceType,
        "role" to rule.role,
        "targetType" to rule.targetType,
    )

    private fun mutationDefinitions(dialect: JsonSchemaDialect): Map<String, Map<String, Any?>> {
        fun uuid(): Map<String, Any?> = linkedMapOf("type" to "string", "format" to "uuid")
        fun nullableString(): Map<String, Any?> = linkedMapOf("type" to "string")
        fun objectMap(value: Map<String, Any?> = linkedMapOf("type" to "object")): Map<String, Any?> =
            linkedMapOf(
                "type" to "object",
                "additionalProperties" to value,
            )
        fun array(items: Map<String, Any?>): Map<String, Any?> = linkedMapOf(
            "type" to "array",
            "items" to items,
        )
        fun ref(key: String): Map<String, Any?> = linkedMapOf(
            "\$ref" to "${dialect.defsRefPrefix}$key",
        )
        fun definition(
            title: String,
            properties: Map<String, Any?>,
            required: List<String> = emptyList(),
        ): Map<String, Any?> = linkedMapOf<String, Any?>(
            "title" to title,
            "type" to "object",
            "properties" to properties,
        ).also { if (required.isNotEmpty()) it["required"] = required }

        val entity = definition(
            "Entity",
            linkedMapOf(
                "id" to uuid(),
                "type" to nullableString(),
                "schemaVersion" to nullableString(),
                "payload" to objectMap(),
                "annotations" to objectMap(linkedMapOf("type" to "string")),
                "createdAt" to linkedMapOf("type" to "string", "format" to "date-time"),
                "updatedAt" to linkedMapOf("type" to "string", "format" to "date-time"),
                "headVersion" to linkedMapOf("type" to "integer"),
            ),
            required = listOf("type", "schemaVersion", "payload"),
        )
        val edge = definition(
            "Edge",
            linkedMapOf(
                "id" to uuid(),
                "graphId" to uuid(),
                "source" to uuid(),
                "target" to uuid(),
                "role" to nullableString(),
                "type" to nullableString(),
                "schemaVersion" to nullableString(),
                "properties" to objectMap(),
                "createdAt" to linkedMapOf("type" to "string", "format" to "date-time"),
                "updatedAt" to linkedMapOf("type" to "string", "format" to "date-time"),
                "headVersion" to linkedMapOf("type" to "integer"),
            ),
            required = listOf("source", "target", "role"),
        )
        val entityMutation = definition(
            "EntityMutation",
            linkedMapOf(
                "set" to array(ref("Entity")),
                "unset" to array(uuid()),
            ),
        )
        val edgeMutation = definition(
            "EdgeMutation",
            linkedMapOf(
                "set" to array(ref("Edge")),
                "unset" to array(uuid()),
            ),
        )
        val graphMutation = definition(
            "GraphMutation",
            linkedMapOf(
                "entities" to ref("EntityMutation"),
                "edges" to ref("EdgeMutation"),
                "mode" to linkedMapOf(
                    "type" to "string",
                    "enum" to listOf("MERGE", "REPLACE"),
                ),
            ),
            required = listOf("entities", "edges", "mode"),
        )
        return linkedMapOf(
            "Entity" to entity,
            "Edge" to edge,
            "EntityMutation" to entityMutation,
            "EdgeMutation" to edgeMutation,
            "GraphMutation" to graphMutation,
        )
    }

    private fun schemaJavaTypeName(schema: Schema): String =
        schema.attributes["codegen.java.typeName"]?.let {
            validJavaIdentifier(it, "schema ${schema.type}@${schema.version} codegen.java.typeName")
        } ?: jsonSchemaDefKey(schema.type)

    private fun schemaBaseClass(schema: Schema): String? =
        javaTypeOverride(schema.attributes, "codegen.baseClass", "schema ${schema.type}@${schema.version}")

    private fun schemaInterfaces(schema: Schema): List<String> =
        javaInterfaces(schema.attributes, "schema ${schema.type}@${schema.version}")

    private fun relationBaseClass(rule: AllowedEdgeRule): String? =
        javaTypeOverride(rule.attributes, "codegen.baseClass", "relation ${rule.sourceType}/${rule.role}/${rule.targetType}")

    private fun relationMethod(
        rule: AllowedEdgeRule,
        key: String,
        fallback: () -> String,
    ): String =
        rule.attributes[key]?.let { validJavaIdentifier(it, "relation ${rule.role} $key") } ?: fallback()

    private fun javaTypeOverride(attributes: Map<String, String>, key: String, owner: String): String? =
        attributes[key]?.let { validJavaTypeReference(it, "$owner $key") }

    private fun javaInterfaces(attributes: Map<String, String>, owner: String): List<String> {
        val raw = attributes["codegen.interfaces"] ?: return emptyList()
        if (raw.isBlank()) {
            throw JsonSchemaCodegenException("$owner codegen.interfaces must not be blank")
        }
        return raw.split(",").mapIndexed { index, value ->
            validJavaTypeReference(value, "$owner codegen.interfaces[$index]")
        }.distinct()
    }

    private fun validJavaIdentifier(raw: String, owner: String): String {
        val value = raw.trim()
        if (value.isEmpty()) {
            throw JsonSchemaCodegenException("$owner must not be blank")
        }
        if (!JAVA_IDENTIFIER.matches(value) || value in JAVA_KEYWORDS) {
            throw JsonSchemaCodegenException("$owner is not a valid Java identifier: '$raw'")
        }
        return value
    }

    private fun validJavaTypeReference(raw: String, owner: String): String {
        val value = raw.trim()
        if (value.isEmpty()) {
            throw JsonSchemaCodegenException("$owner must not be blank")
        }
        val parts = value.split(".")
        if (parts.any { !JAVA_IDENTIFIER.matches(it) || it in JAVA_KEYWORDS }) {
            throw JsonSchemaCodegenException("$owner is not a valid Java type: '$raw'")
        }
        return value
    }

    private fun staticRelationBinding(
        endpoint: String,
        endpointSchema: Schema?,
        otherEndpoint: String,
        otherSchema: Schema?,
        baseClass: String?,
    ): Boolean =
        (endpointSchema != null || endpoint == AllowedEdgeRule.ANY && baseClass != null) &&
            (otherSchema != null || otherEndpoint == AllowedEdgeRule.ANY && baseClass != null)

    private fun registerMethod(
        owners: MutableMap<String, String>,
        owner: String,
        method: String,
        rule: AllowedEdgeRule,
    ) {
        val key = "$owner#$method"
        val relation = "(${rule.sourceType}, ${rule.role}, ${rule.targetType})"
        val previous = owners.putIfAbsent(key, relation)
        if (previous != null && previous != relation) {
            throw JsonSchemaCodegenException(
                "Java relation method collision '$method' for '$owner': $previous and $relation",
            )
        }
    }

    companion object {
        private val JAVA_IDENTIFIER = Regex("[A-Za-z_$][A-Za-z0-9_$]*")
        private val JAVA_KEYWORDS = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
            "null", "var", "yield", "record", "sealed", "permits",
        )

        fun latestByType(schemas: Collection<Schema>): Map<String, Schema> =
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

class JsonSchemaCodegenException(message: String) : IllegalArgumentException(message)
