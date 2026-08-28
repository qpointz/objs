package org.poc.objs.api.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue

/** How edge properties behave for a relation. */
enum class PropertiesPolicy {
    /** Bare edge with no properties. */
    NONE,

    /** Properties are associated with a schema type and version. */
    SCHEMA,
}

/**
 * Declared multiplicity on an allowed relation.
 *
 * Cardinality is metadata and is not a persist-time edge-count check.
 */
enum class EdgeCardinality(val wire: String) {
    UNSPECIFIED("UNSPECIFIED"),
    ONE_TO_ONE("1:1"),
    ONE_TO_MANY("1:*"),
    ;

    val isSingular: Boolean get() = this == ONE_TO_ONE
    val isMany: Boolean get() = this == ONE_TO_MANY

    @JsonValue
    fun toWire(): String = wire

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromWire(raw: String): EdgeCardinality {
            val trimmed = raw.trim()
            return entries.find { it.wire == trimmed || it.name == trimmed }
                ?: throw IllegalArgumentException("Unknown cardinality: $raw")
        }
    }
}

/** Directed allow-list relation metadata. */
data class AllowedEdgeRule(
    val sourceType: String,
    val role: String,
    val targetType: String,
    val propertiesPolicy: PropertiesPolicy = PropertiesPolicy.NONE,
    val emptyPropertiesAllowed: Boolean = true,
    val propertiesSchemaType: String? = null,
    val propertiesSchemaVersion: String? = null,
    val cardinality: EdgeCardinality = EdgeCardinality.UNSPECIFIED,
    val description: String? = null,
    val sourceVerb: String? = null,
    val targetVerb: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val tags: List<String> = emptyList(),
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val attributes: Map<String, String> = emptyMap(),
) {
    companion object {
        const val ANY: String = "*"
    }
}

fun AllowedEdgeRule.matches(sourceType: String, role: String, targetType: String): Boolean =
    componentMatches(this.sourceType, sourceType) &&
        componentMatches(this.role, role) &&
        componentMatches(this.targetType, targetType)

private fun componentMatches(pattern: String, value: String): Boolean =
    pattern == AllowedEdgeRule.ANY || pattern == value

fun AllowedEdgeRule.specificity(): Int {
    var score = 0
    if (sourceType != AllowedEdgeRule.ANY) score += 4
    if (role != AllowedEdgeRule.ANY) score += 2
    if (targetType != AllowedEdgeRule.ANY) score += 1
    return score
}

fun findMostSpecificRule(
    rules: Iterable<AllowedEdgeRule>,
    sourceType: String,
    role: String,
    targetType: String,
): AllowedEdgeRule? {
    var best: AllowedEdgeRule? = null
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
