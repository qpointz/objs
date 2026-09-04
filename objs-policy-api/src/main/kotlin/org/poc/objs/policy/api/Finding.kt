package org.poc.objs.policy.api

import java.util.UUID

/**
 * Optional explanation attached to a [PolicyOutcome]. Bindings are soft-validated only.
 * Empty [entities] / [edges] are valid (policy-level statement with no graph locus).
 */
data class Finding(
    val message: String,
    val severity: String? = null,
    val code: String? = null,
    val entities: List<UUID> = emptyList(),
    val edges: List<UUID> = emptyList(),
    val extras: Map<String, Any?> = emptyMap(),
)
