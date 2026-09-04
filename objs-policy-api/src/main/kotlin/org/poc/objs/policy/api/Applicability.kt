package org.poc.objs.policy.api

/** Whether a policy is in scope for the current [PolicyEvaluationContext]. */
enum class ApplicabilityDecision {
    IN_SCOPE,
    NOT_APPLICABLE,
}

data class ApplicabilityVerdict(
    val decision: ApplicabilityDecision,
    val reason: String? = null,
)

/**
 * Per-policy applicability gate. Always invoked inside evaluate (cannot be skipped).
 * Preview via [PolicyEvaluator.applicability] uses the same gate without engines.
 */
fun interface ApplicabilitySelector {
    fun decide(context: PolicyEvaluationContext, policy: Policy): ApplicabilityVerdict
}

/** Per-ref preview row from [PolicyEvaluator.applicability]. */
data class PolicyApplicabilityOutcome(
    val policyName: String,
    val policyVersion: Long,
    val engineKind: String,
    val verdict: ApplicabilityVerdict,
)
