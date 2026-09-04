package org.poc.objs.policy.core

import org.poc.objs.policy.api.ApplicabilityDecision
import org.poc.objs.policy.api.ApplicabilityKinds
import org.poc.objs.policy.api.ApplicabilitySelector
import org.poc.objs.policy.api.ApplicabilityVerdict
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyEvaluationContext

/**
 * S1 default gate: blank / missing or [ApplicabilityKinds.ALWAYS_APPLY] → in scope.
 * Other kinds are not implemented here — [DefaultPolicyEvaluator] records ERROR.
 */
object AlwaysApplyApplicabilitySelector : ApplicabilitySelector {
    override fun decide(context: PolicyEvaluationContext, policy: Policy): ApplicabilityVerdict {
        val kind = policy.applicabilityKind?.trim().orEmpty()
        return if (kind.isEmpty() || kind.equals(ApplicabilityKinds.ALWAYS_APPLY, ignoreCase = true)) {
            ApplicabilityVerdict(ApplicabilityDecision.IN_SCOPE)
        } else {
            ApplicabilityVerdict(
                decision = ApplicabilityDecision.NOT_APPLICABLE,
                reason = "Unsupported applicabilityKind '$kind' (handled as ERROR by evaluator)",
            )
        }
    }

    fun isSupportedKind(policy: Policy): Boolean {
        val kind = policy.applicabilityKind?.trim().orEmpty()
        return kind.isEmpty() || kind.equals(ApplicabilityKinds.ALWAYS_APPLY, ignoreCase = true)
    }
}
