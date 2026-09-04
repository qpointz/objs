package org.poc.objs.policy.api

import org.poc.objs.api.domain.GraphFragment

/**
 * Fixed orchestrator contract: `evaluate(fragment, policyRefs)` always runs applicability.
 * Suite / batch shapes are wrappers over this API (later stories).
 */
interface PolicyEvaluator {
    fun evaluate(fragment: GraphFragment, policyRefs: List<PolicyRef>): EvaluationResult

    /** Same resolve → wiring → gate path as evaluate; no engine side effects. */
    fun applicability(
        fragment: GraphFragment,
        policyRefs: List<PolicyRef>,
    ): List<PolicyApplicabilityOutcome>
}
