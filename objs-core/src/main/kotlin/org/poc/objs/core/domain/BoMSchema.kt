package org.poc.objs.core.domain

/**
 * JSON Schema entry keyed by [type] + [version] in the central catalog.
 */
data class BoMSchema(
    val type: String,
    val version: String,
    /** JSON Schema document as a JSON object map (draft used by networknt validator). */
    val schema: Map<String, Any?>,
) {
    val key: BoMSchemaKey get() = BoMSchemaKey(type, version)
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
