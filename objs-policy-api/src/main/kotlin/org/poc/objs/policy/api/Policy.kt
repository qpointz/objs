package org.poc.objs.policy.api

import java.util.UUID

/**
 * Foundation policy artefact: metadata + engine kind + opaque evaluation body
 * (+ optional applicability fields). Identified by [name] + immutable serial [version].
 *
 * There is no policy-level enabled flag (suite enablement is a later concern).
 */
data class Policy(
    val id: UUID,
    val name: String,
    val version: Long,
    val engineKind: String,
    val body: String,
    val contentType: String? = null,
    val applicabilityKind: String? = null,
    val applicabilityBody: String? = null,
)

/** Write payload for [PolicyRepository.save] — repository allocates id and serial version. */
data class PolicyWrite(
    val name: String,
    val engineKind: String,
    val body: String,
    val contentType: String? = null,
    val applicabilityKind: String? = null,
    val applicabilityBody: String? = null,
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
