package org.poc.objs.policy.api

/** Per-policy evaluation status. Authoritative unit of result; not suite roll-up. */
enum class PolicyOutcomeStatus {
    PASS,
    FAIL,
    ERROR,
    NOT_APPLICABLE,
}

/**
 * One policy's outcome. Always cites executed [policyName] + serial [policyVersion].
 */
data class PolicyOutcome(
    val policyName: String,
    val policyVersion: Long,
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
 * Optional flat aggregate: `ERROR > FAIL > PASS > NOT_APPLICABLE`.
 * Empty list → `NOT_APPLICABLE`.
 */
fun aggregateOverall(outcomes: List<PolicyOutcome>): PolicyOutcomeStatus {
    if (outcomes.isEmpty()) {
        return PolicyOutcomeStatus.NOT_APPLICABLE
    }
    if (outcomes.any { it.status == PolicyOutcomeStatus.ERROR }) {
        return PolicyOutcomeStatus.ERROR
    }
    if (outcomes.any { it.status == PolicyOutcomeStatus.FAIL }) {
        return PolicyOutcomeStatus.FAIL
    }
    if (outcomes.any { it.status == PolicyOutcomeStatus.PASS }) {
        return PolicyOutcomeStatus.PASS
    }
    return PolicyOutcomeStatus.NOT_APPLICABLE
}
