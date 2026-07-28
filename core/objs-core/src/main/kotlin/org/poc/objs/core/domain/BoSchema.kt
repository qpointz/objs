package org.poc.objs.core.domain

/**
 * JSON Schema entry keyed by [type] + [version] in the central catalog.
 */
data class BoSchema(
    val type: String,
    val version: String,
    /** JSON Schema document as a JSON object map (draft used by networknt validator). */
    val schema: Map<String, Any?>,
) {
    val key: BoSchemaKey get() = BoSchemaKey(type, version)
}

data class BoSchemaKey(val type: String, val version: String)

/**
 * In-memory central schema repository for entity payloads and edge properties (G-6/G-8).
 */
class BoSchemaCatalog {
    private val schemas = linkedMapOf<BoSchemaKey, BoSchema>()

    fun register(schema: BoSchema) {
        schemas[schema.key] = schema
    }

    fun get(type: String, version: String): BoSchema? = schemas[BoSchemaKey(type, version)]

    fun get(key: BoSchemaKey): BoSchema? = schemas[key]

    fun contains(type: String, version: String): Boolean = schemas.containsKey(BoSchemaKey(type, version))

    fun all(): Collection<BoSchema> = schemas.values.toList()

    fun clear() = schemas.clear()
}

/** How edge properties behave for an allow-list rule (G-7). */
enum class BoPropertiesPolicy {
    /** Bare edge — no properties. */
    NONE,

    /** Properties validated against schema (type+version on the edge). */
    SCHEMA,
}

/**
 * Allowed-edge allow-list entry: directed (sourceType, role, targetType) + properties policy.
 */
data class BoAllowedEdgeRule(
    val sourceType: String,
    val role: String,
    val targetType: String,
    val propertiesPolicy: BoPropertiesPolicy = BoPropertiesPolicy.NONE,
    /** When [propertiesPolicy] is [BoPropertiesPolicy.SCHEMA], whether empty properties are allowed. */
    val emptyPropertiesAllowed: Boolean = true,
)

/**
 * In-memory allow-list catalog (G-7). Not in catalog → deny.
 */
class BoAllowedEdgeCatalog {
    private val rules = linkedMapOf<Triple<String, String, String>, BoAllowedEdgeRule>()

    fun register(rule: BoAllowedEdgeRule) {
        rules[Triple(rule.sourceType, rule.role, rule.targetType)] = rule
    }

    fun find(sourceType: String, role: String, targetType: String): BoAllowedEdgeRule? =
        rules[Triple(sourceType, role, targetType)]

    fun all(): Collection<BoAllowedEdgeRule> = rules.values.toList()

    fun clear() = rules.clear()
}
