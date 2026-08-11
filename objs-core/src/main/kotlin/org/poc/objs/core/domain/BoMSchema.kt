package org.poc.objs.core.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** How a catalog schema is used by the graph model. */
enum class BoMSchemaUsage {
    /** Entity payload schema (`BoMEntity.type` + `schemaVersion`). */
    ENTITY,

    /** Edge properties schema (`BoMEdge.type` + `schemaVersion` when policy is SCHEMA). */
    EDGE_PROPERTIES,
}

/** Authoritative object-schema DSL definition keyed by [type] + [version]. */
data class BoMSchema(
    val type: String,
    val version: String,
    val contentSchema: BoMSchemaNode,
    val usage: BoMSchemaUsage = BoMSchemaUsage.ENTITY,
) {
    val key: BoMSchemaKey get() = BoMSchemaKey(type, version)

    /** Generate the JSON Schema projection used for payload validation and external tooling. */
    fun toJsonSchema(): Map<String, Any?> = BoMJsonSchema.from(this)
}

data class BoMSchemaKey(val type: String, val version: String)

/**
 * Central schema repository API for entity payloads and edge properties (G-6/G-8).
 * Consumers depend on this interface; implementations may be in-memory or persistent.
 */
interface BoMSchemaCatalog {
    fun register(schema: BoMSchema)
    fun get(type: String, version: String): BoMSchema?
    fun get(key: BoMSchemaKey): BoMSchema?
    fun contains(type: String, version: String): Boolean
    fun all(): Collection<BoMSchema>
    fun listByType(type: String): List<BoMSchema>
    fun types(): Set<String>
    /** @return true if an entry was removed */
    fun remove(type: String, version: String): Boolean
    fun clear()
}

/** How edge properties behave for an allow-list rule (G-7). */
enum class BoMPropertiesPolicy {
    /** Bare edge — no properties. */
    NONE,

    /** Properties validated against schema (type+version on the edge). */
    SCHEMA,
}

/**
 * Declared multiplicity on an allowed-edge rule (source → target via role).
 *
 * Schema metadata only — not enforced as an edge-count check at persist.
 * Wire / YAML / JSON values are [wire], not the Kotlin enum names for `1:1` / `1:*`.
 */
enum class BoMEdgeCardinality(val wire: String) {
    /** No declared multiplicity. */
    UNSPECIFIED("UNSPECIFIED"),

    /** Singular target. */
    ONE_TO_ONE("1:1"),

    /** Many targets. */
    ONE_TO_MANY("1:*"),
    ;

    val isSingular: Boolean get() = this == ONE_TO_ONE

    val isMany: Boolean get() = this == ONE_TO_MANY

    @JsonValue
    fun toWire(): String = wire

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromWire(raw: String): BoMEdgeCardinality {
            val trimmed = raw.trim()
            return entries.find { it.wire == trimmed || it.name == trimmed }
                ?: throw IllegalArgumentException("Unknown cardinality: $raw")
        }
    }
}

/**
 * Allowed-edge allow-list entry: directed (sourceType, role, targetType) + properties policy.
 *
 * [sourceType], [role], and [targetType] may be [ANY] (`*`) to match any value in that position.
 * Example: `(* , depends_on , *)` allows a `depends_on` edge between any entity types.
 */
data class BoMAllowedEdgeRule(
    val sourceType: String,
    val role: String,
    val targetType: String,
    val propertiesPolicy: BoMPropertiesPolicy = BoMPropertiesPolicy.NONE,
    /** When [propertiesPolicy] is [BoMPropertiesPolicy.SCHEMA], whether empty properties are allowed. */
    val emptyPropertiesAllowed: Boolean = true,
    /** Property-schema catalog type selected for this relation. */
    val propertiesSchemaType: String? = null,
    /** Property-schema catalog version selected for this relation. */
    val propertiesSchemaVersion: String? = null,
    /** Declared source→target multiplicity; default [BoMEdgeCardinality.UNSPECIFIED]. */
    val cardinality: BoMEdgeCardinality = BoMEdgeCardinality.UNSPECIFIED,
) {
    companion object {
        /** Wildcard token: matches any type or role in that position. */
        const val ANY: String = "*"
    }
}

/**
 * Allowed-edge catalog API (G-7). Not in catalog → deny.
 *
 * Lookup prefers the **most specific** matching rule (fewest wildcards). Exact
 * `(Person, knows, Person)` wins over `(*, knows, *)` when both match.
 */
interface BoMAllowedEdgeCatalog {
    fun register(rule: BoMAllowedEdgeRule)
    fun find(sourceType: String, role: String, targetType: String): BoMAllowedEdgeRule?
    fun all(): Collection<BoMAllowedEdgeRule>
    /** @return true if a rule with that exact triple key was removed */
    fun remove(sourceType: String, role: String, targetType: String): Boolean
    fun clear()
}

// ── Shared matching helpers (used by any implementation) ──

fun BoMAllowedEdgeRule.matches(sourceType: String, role: String, targetType: String): Boolean =
    componentMatches(this.sourceType, sourceType) &&
        componentMatches(this.role, role) &&
        componentMatches(this.targetType, targetType)

private fun componentMatches(pattern: String, value: String): Boolean =
    pattern == BoMAllowedEdgeRule.ANY || pattern == value

/** Higher = more specific (more concrete components). */
fun BoMAllowedEdgeRule.specificity(): Int {
    var score = 0
    if (sourceType != BoMAllowedEdgeRule.ANY) score += 4
    if (role != BoMAllowedEdgeRule.ANY) score += 2
    if (targetType != BoMAllowedEdgeRule.ANY) score += 1
    return score
}

/**
 * Find the most specific matching rule from a collection (shared algorithm).
 */
fun findMostSpecificRule(
    rules: Iterable<BoMAllowedEdgeRule>,
    sourceType: String,
    role: String,
    targetType: String,
): BoMAllowedEdgeRule? {
    var best: BoMAllowedEdgeRule? = null
    var bestScore = -1
    for (rule in rules) {
        if (!rule.matches(sourceType, role, targetType)) continue
        val score = rule.specificity()
        if (score >= bestScore) {
            best = rule
            bestScore = score
        }
    }
    return best
}
