package org.poc.objs.policy.api

import org.poc.objs.api.domain.GraphFragment

/**
 * Fixed orchestrator contract: `evaluate(fragment, policyRefs)` always runs applicability.
 * Suite / batch shapes are wrappers over this API (later stories).
 */
interface PolicyEvaluator {
    fun evaluate(fragment: GraphFragment, policyRefs: List<PolicyRef>): EvaluationResult

    /**
     * Evaluate already-materialized [policies] (e.g. playground dirty editor buffer).
     * Default: unsupported — core [org.poc.objs.policy.core.DefaultPolicyEvaluator] implements.
     */
    fun evaluatePolicies(fragment: GraphFragment, policies: List<Policy>): EvaluationResult =
        throw UnsupportedOperationException("evaluatePolicies not supported by this evaluator")

    /** Same resolve → wiring → gate path as evaluate; no engine side effects. */
    fun applicability(
        fragment: GraphFragment,
        policyRefs: List<PolicyRef>,
    ): List<PolicyApplicabilityOutcome>
}
