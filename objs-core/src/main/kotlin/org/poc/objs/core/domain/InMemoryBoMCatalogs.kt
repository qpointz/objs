package org.poc.objs.core.domain

/**
 * In-memory [BoMSchemaCatalog]: insertion-ordered map keyed by (type, version).
 * Suitable for tests, local profiles, and as the runtime cache layer inside persistent implementations.
 */
open class InMemoryBoMSchemaCatalog : BoMSchemaCatalog {
    private val schemas = linkedMapOf<BoMSchemaKey, BoMSchema>()

    override fun register(schema: BoMSchema) {
        schemas[schema.key] = schema
    }

    override fun get(type: String, version: String): BoMSchema? = schemas[BoMSchemaKey(type, version)]

    override fun get(key: BoMSchemaKey): BoMSchema? = schemas[key]

    override fun contains(type: String, version: String): Boolean =
        schemas.containsKey(BoMSchemaKey(type, version))

    override fun all(): Collection<BoMSchema> = schemas.values.toList()

    override fun listByType(type: String): List<BoMSchema> =
        schemas.values.filter { it.type == type }

    override fun types(): Set<String> = schemas.values.map { it.type }.toSet()

    override fun remove(type: String, version: String): Boolean =
        schemas.remove(BoMSchemaKey(type, version)) != null

    override fun clear() = schemas.clear()
}

/**
 * In-memory [BoMAllowedEdgeCatalog]: insertion-ordered map keyed by (source, role, target) triple.
 * Uses [findMostSpecificRule] for wildcard-aware lookup.
 */
open class InMemoryBoMAllowedEdgeCatalog : BoMAllowedEdgeCatalog {
    private val rules = linkedMapOf<Triple<String, String, String>, BoMAllowedEdgeRule>()

    override fun register(rule: BoMAllowedEdgeRule) {
        rules[Triple(rule.sourceType, rule.role, rule.targetType)] = rule
    }

    override fun find(sourceType: String, role: String, targetType: String): BoMAllowedEdgeRule? =
        findMostSpecificRule(rules.values, sourceType, role, targetType)

    override fun all(): Collection<BoMAllowedEdgeRule> = rules.values.toList()

    override fun remove(sourceType: String, role: String, targetType: String): Boolean =
        rules.remove(Triple(sourceType, role, targetType)) != null

    override fun clear() = rules.clear()
}
