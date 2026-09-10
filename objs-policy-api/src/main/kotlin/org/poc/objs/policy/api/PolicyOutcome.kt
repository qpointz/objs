package org.poc.objs.policy.api

/** Per-policy evaluation status. Authoritative unit of result; not suite roll-up. */
enum class PolicyOutcomeStatus {
    PASS,
    FAIL,
    /** Could not execute correctly (engine/body/unknown-kind/runtime). Formerly `ERROR`. */
    EXEC_ERROR,
    NOT_APPLICABLE,
    ;

    companion object {
        /** Parse status token; legacy archive `ERROR` → [EXEC_ERROR] (G-P54v). */
        fun parseToken(raw: String): PolicyOutcomeStatus {
            val t = raw.trim().uppercase()
            if (t == "ERROR") return EXEC_ERROR
            return valueOf(t)
        }
    }
}

/**
 * One policy's outcome. Always cites executed [policyName] + [policySerial].
 */
data class PolicyOutcome(
    val policyName: String,
    val policySerial: Long,
    val engineKind: String,
    val status: PolicyOutcomeStatus,
    val notApplicableReason: String? = null,
    val findings: List<Finding> = emptyList(),
    val message: String? = null,
)

/**
 * Flat evaluation result. [outcomes] are authoritative; [overall] is an optional convenience
 * badge (see [aggregateOverall]) and must not be treated as suite semantics.
 */
data class EvaluationResult(
    val outcomes: List<PolicyOutcome>,
    val overall: PolicyOutcomeStatus? = null,
)

/**
 * Optional flat aggregate: `EXEC_ERROR > FAIL > PASS > NOT_APPLICABLE`.
 * Empty list → `NOT_APPLICABLE`.
 */
fun aggregateOverall(outcomes: List<PolicyOutcome>): PolicyOutcomeStatus {
    if (outcomes.isEmpty()) {
        return PolicyOutcomeStatus.NOT_APPLICABLE
    }
    if (outcomes.any { it.status == PolicyOutcomeStatus.EXEC_ERROR }) {
        return PolicyOutcomeStatus.EXEC_ERROR
    }
    if (outcomes.any { it.status == PolicyOutcomeStatus.FAIL }) {
        return PolicyOutcomeStatus.FAIL
    }
    if (outcomes.any { it.status == PolicyOutcomeStatus.PASS }) {
        return PolicyOutcomeStatus.PASS
    }
    return PolicyOutcomeStatus.NOT_APPLICABLE
}
