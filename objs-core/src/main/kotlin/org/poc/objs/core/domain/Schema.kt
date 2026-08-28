package org.poc.objs.core.domain

import org.poc.objs.api.domain.AllowedEdgeRule

import com.fasterxml.jackson.annotation.JsonInclude

/** How a catalog schema is used by the graph model. */
enum class SchemaUsage {
    /** Entity payload schema (`Entity.type` + `schemaVersion`). */
    ENTITY,

    /** Edge properties schema (`Edge.type` + `schemaVersion` when policy is SCHEMA). */
    EDGE_PROPERTIES,
}

/** Authoritative object-schema DSL definition keyed by [type] + [version]. */
data class Schema(
    val type: String,
    val version: String,
    val contentSchema: SchemaNode,
    val usage: SchemaUsage = SchemaUsage.ENTITY,
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val tags: List<String> = emptyList(),
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val attributes: Map<String, String> = emptyMap(),
) {
    val key: SchemaKey get() = SchemaKey(type, version)

    /** Generate the JSON Schema projection used for payload validation and external tooling. */
    fun toJsonSchema(): Map<String, Any?> = JsonSchema.from(this)
}

data class SchemaKey(val type: String, val version: String)

/**
 * Central schema repository API for entity payloads and edge properties (G-6/G-8).
 * Consumers depend on this interface; implementations may be in-memory or persistent.
 */
interface SchemaCatalog {
    fun register(schema: Schema)
    fun get(type: String, version: String): Schema?
    fun get(key: SchemaKey): Schema?
    fun contains(type: String, version: String): Boolean
    fun all(): Collection<Schema>
    fun listByType(type: String): List<Schema>
    fun types(): Set<String>
    /** @return true if an entry was removed */
    fun remove(type: String, version: String): Boolean
    fun clear()

    /**
     * Reload this catalog from its durable store when applicable.
     * No-op for pure in-memory catalogs.
     */
    fun refreshFromStore() {}
}

/**
 * Allowed-edge catalog API (G-7). Not in catalog → deny.
 *
 * Lookup prefers the **most specific** matching rule (fewest wildcards). Exact
 * `(Person, knows, Person)` wins over `(*, knows, *)` when both match.
 */
interface AllowedEdgeCatalog {
    fun register(rule: AllowedEdgeRule)
    fun find(sourceType: String, role: String, targetType: String): AllowedEdgeRule?
    fun all(): Collection<AllowedEdgeRule>
    /** @return true if a rule with that exact triple key was removed */
    fun remove(sourceType: String, role: String, targetType: String): Boolean
    fun clear()

    /**
     * Reload this catalog from its durable store when applicable.
     * No-op for pure in-memory catalogs.
     */
    fun refreshFromStore() {}
}
