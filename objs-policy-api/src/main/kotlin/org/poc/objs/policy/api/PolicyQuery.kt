package org.poc.objs.policy.api

import java.util.UUID

/**
 * Policy list filters (C-32). No paging.
 * Tag and annotation filters are **all-of** (policy must contain each).
 */
data class PolicyQuery(
    val categoryId: UUID? = null,
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
    /** Case-insensitive substring on [Policy.name]; blank/null = no name filter. */
    val nameContains: String? = null,
)

object PolicyTags {
    /** Trim, lowercase, drop blanks, dedupe (order of first occurrence). */
    fun normalize(tags: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        for (raw in tags) {
            val t = raw.trim().lowercase()
            if (t.isNotEmpty()) seen.add(t)
        }
        return seen.toList()
    }

    fun requireNonEmpty(tags: List<String>): List<String> {
        val normalized = normalize(tags)
        require(normalized.isNotEmpty()) { "Policy tags must be non-empty" }
        return normalized
    }
}
