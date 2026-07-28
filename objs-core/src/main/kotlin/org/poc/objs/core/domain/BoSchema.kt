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
 *
 * [sourceType], [role], and [targetType] may be [ANY] (`*`) to match any value in that position.
 * Example: `(* , depends_on , *)` allows a `depends_on` edge between any entity types.
 */
data class BoAllowedEdgeRule(
    val sourceType: String,
    val role: String,
    val targetType: String,
    val propertiesPolicy: BoPropertiesPolicy = BoPropertiesPolicy.NONE,
    /** When [propertiesPolicy] is [BoPropertiesPolicy.SCHEMA], whether empty properties are allowed. */
    val emptyPropertiesAllowed: Boolean = true,
) {
    companion object {
        /** Wildcard token: matches any type or role in that position. */
        const val ANY: String = "*"
    }
}

/**
 * In-memory allow-list catalog (G-7). Not in catalog → deny.
 *
 * Lookup prefers the **most specific** matching rule (fewest wildcards). Exact
 * `(Person, knows, Person)` wins over `(*, knows, *)` when both match.
 */
class BoAllowedEdgeCatalog {
    private val rules = linkedMapOf<Triple<String, String, String>, BoAllowedEdgeRule>()

    fun register(rule: BoAllowedEdgeRule) {
        rules[Triple(rule.sourceType, rule.role, rule.targetType)] = rule
    }

    fun find(sourceType: String, role: String, targetType: String): BoAllowedEdgeRule? {
        var best: BoAllowedEdgeRule? = null
        var bestScore = -1
        for (rule in rules.values) {
            if (!rule.matches(sourceType, role, targetType)) continue
            val score = rule.specificity()
            // Higher specificity wins; equal specificity → later registration wins.
            if (score >= bestScore) {
                best = rule
                bestScore = score
            }
        }
        return best
    }

    fun all(): Collection<BoAllowedEdgeRule> = rules.values.toList()

    fun clear() = rules.clear()
}

private fun BoAllowedEdgeRule.matches(sourceType: String, role: String, targetType: String): Boolean =
    componentMatches(this.sourceType, sourceType) &&
        componentMatches(this.role, role) &&
        componentMatches(this.targetType, targetType)

private fun componentMatches(pattern: String, value: String): Boolean =
    pattern == BoAllowedEdgeRule.ANY || pattern == value

/** Higher = more specific (more concrete components). */
private fun BoAllowedEdgeRule.specificity(): Int {
    var score = 0
    if (sourceType != BoAllowedEdgeRule.ANY) score += 4
    if (role != BoAllowedEdgeRule.ANY) score += 2
    if (targetType != BoAllowedEdgeRule.ANY) score += 1
    return score
}
