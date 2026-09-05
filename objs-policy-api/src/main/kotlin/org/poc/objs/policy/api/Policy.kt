package org.poc.objs.policy.api

import java.util.UUID

/**
 * Foundation policy artefact: metadata + engine kind + opaque evaluation body
 * (+ optional applicability fields).
 *
 * Identity: [name] + immutable [serial]. User-managed [version] is the major.minor string
 * (e.g. `1.2`). Full display is typically `[version] · [serial]`. [serial] uses the same
 * timestamp allocation as object head versions.
 */
data class Policy(
    val id: UUID,
    val name: String,
    /** Timestamp serial (object head-version rule); pin / latest key. */
    val serial: Long,
    val engineKind: String,
    val body: String,
    val contentType: String? = null,
    val applicabilityKind: String? = null,
    val applicabilityBody: String? = null,
    val categoryId: UUID,
    val tags: List<String>,
    val annotations: Map<String, String> = emptyMap(),
    /** User-managed major.minor string (e.g. `0.1`). */
    val version: String = "0.1",
    /** Human-readable summary (C-32); empty allowed. */
    val description: String = "",
)

/** Write payload for [PolicyRepository.save] — repository allocates id and [Policy.serial]. */
data class PolicyWrite(
    val name: String,
    val engineKind: String,
    val body: String,
    val categoryId: UUID,
    val tags: List<String>,
    val contentType: String? = null,
    val applicabilityKind: String? = null,
    val applicabilityBody: String? = null,
    val annotations: Map<String, String> = emptyMap(),
    /** User-managed major.minor string (e.g. `0.1`). */
    val version: String = "0.1",
    val description: String = "",
)

/** Well-known [Policy.engineKind] values. Kind is a String (not an enum) for extensibility. */
object PolicyEngineKinds {
    const val CUSTOM: String = "CUSTOM"
    const val DROOLS: String = "DROOLS"
}

/** Well-known [Policy.applicabilityKind] values. */
object ApplicabilityKinds {
    const val ALWAYS_APPLY: String = "ALWAYS_APPLY"
}
