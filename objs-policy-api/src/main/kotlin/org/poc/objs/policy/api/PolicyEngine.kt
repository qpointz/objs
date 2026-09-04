package org.poc.objs.policy.api

/**
 * Engine adapter result. Orchestrator copies name/version/engineKind onto [PolicyOutcome].
 * Status should be PASS, FAIL, or ERROR (not NOT_APPLICABLE — that comes from the gate).
 */
data class PolicyEngineResult(
    val status: PolicyOutcomeStatus,
    val findings: List<Finding> = emptyList(),
    val message: String? = null,
)

/** Pluggable evaluation adapter selected by [Policy.engineKind]. */
fun interface PolicyEngine {
    fun evaluate(context: PolicyEvaluationContext, policy: Policy): PolicyEngineResult
}
