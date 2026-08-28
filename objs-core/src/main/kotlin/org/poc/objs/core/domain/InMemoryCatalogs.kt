package org.poc.objs.core.domain

import org.poc.objs.api.domain.*

/**
 * In-memory [SchemaCatalog]: insertion-ordered map keyed by (type, version).
 * Suitable for tests, local profiles, and as the runtime cache layer inside persistent implementations.
 */
open class InMemorySchemaCatalog : SchemaCatalog {
    private val schemas = linkedMapOf<SchemaKey, Schema>()

    override fun register(schema: Schema) {
        val normalized = SchemaNormalizer.normalizeStrict(schema)
        schemas[normalized.key] = normalized
    }

    override fun get(type: String, version: String): Schema? = schemas[SchemaKey(type, version)]

    override fun get(key: SchemaKey): Schema? = schemas[key]

    override fun contains(type: String, version: String): Boolean =
        schemas.containsKey(SchemaKey(type, version))

    override fun all(): Collection<Schema> = schemas.values.toList()

    override fun listByType(type: String): List<Schema> =
        schemas.values.filter { it.type == type }

    override fun types(): Set<String> = schemas.values.map { it.type }.toSet()

    override fun remove(type: String, version: String): Boolean =
        schemas.remove(SchemaKey(type, version)) != null

    override fun clear() = schemas.clear()
}

/**
 * In-memory [AllowedEdgeCatalog]: insertion-ordered map keyed by (source, role, target) triple.
 * Uses [findMostSpecificRule] for wildcard-aware lookup.
 */
open class InMemoryAllowedEdgeCatalog : AllowedEdgeCatalog {
    private val rules = linkedMapOf<Triple<String, String, String>, AllowedEdgeRule>()

    override fun register(rule: AllowedEdgeRule) {
        rules[Triple(rule.sourceType, rule.role, rule.targetType)] = rule
    }

    override fun find(sourceType: String, role: String, targetType: String): AllowedEdgeRule? =
        findMostSpecificRule(rules.values, sourceType, role, targetType)

    override fun all(): Collection<AllowedEdgeRule> = rules.values.toList()

    override fun remove(sourceType: String, role: String, targetType: String): Boolean =
        rules.remove(Triple(sourceType, role, targetType)) != null

    override fun clear() = rules.clear()
}
