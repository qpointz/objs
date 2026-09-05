package org.poc.objs.policy.core

import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.GraphFragment
import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.api.domain.GraphFragmentDiagnosticSeverity
import org.poc.objs.policy.api.ApplicabilityDecision
import org.poc.objs.policy.api.ApplicabilitySelector
import org.poc.objs.policy.api.ApplicabilityVerdict
import org.poc.objs.policy.api.EvaluationResult
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyApplicabilityOutcome
import org.poc.objs.policy.api.PolicyContextWirer
import org.poc.objs.policy.api.PolicyEngine
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyEvaluationContext
import org.poc.objs.policy.api.PolicyEvaluationException
import org.poc.objs.policy.api.PolicyEvaluator
import org.poc.objs.policy.api.PolicyOutcome
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.aggregateOverall

/**
 * S1 orchestrator: resolve → PolicyContextWiring → gated evaluate / applicability preview.
 */
class DefaultPolicyEvaluator(
    private val repository: PolicyRepository,
    private val fragmentPolicy: GraphFragmentPolicy = DefaultGraphFragmentPolicy,
    private val wirers: List<PolicyContextWirer> = emptyList(),
    private val applicabilitySelector: ApplicabilitySelector = AlwaysApplyApplicabilitySelector,
    private val engines: Map<String, PolicyEngine> = mapOf(PolicyEngineKinds.CUSTOM to CustomPolicyEngine),
    private val includeOverall: Boolean = true,
) : PolicyEvaluator {

    override fun evaluate(fragment: GraphFragment, policyRefs: List<PolicyRef>): EvaluationResult {
        val context = prepareContext(fragment)
        val outcomes = policyRefs.map { ref -> evaluateOne(context, ref) }
        return EvaluationResult(
            outcomes = outcomes,
            overall = if (includeOverall) aggregateOverall(outcomes) else null,
        )
    }

    /** Evaluate already-resolved [policies] (playground dirty-buffer path). */
    override fun evaluatePolicies(fragment: GraphFragment, policies: List<Policy>): EvaluationResult {
        val context = prepareContext(fragment)
        val outcomes = policies.map { policy -> evaluateResolved(context, policy) }
        return EvaluationResult(
            outcomes = outcomes,
            overall = if (includeOverall) aggregateOverall(outcomes) else null,
        )
    }

    override fun applicability(
        fragment: GraphFragment,
        policyRefs: List<PolicyRef>,
    ): List<PolicyApplicabilityOutcome> {
        val context = prepareContext(fragment)
        return policyRefs.map { ref ->
            val policy = repository.resolve(ref)
            if (policy == null) {
                PolicyApplicabilityOutcome(
                    policyName = refLabel(ref),
                    policySerial = refSerialHint(ref),
                    engineKind = "",
                    verdict = ApplicabilityVerdict(
                        decision = ApplicabilityDecision.NOT_APPLICABLE,
                        reason = "Policy not found: ${refLabel(ref)}",
                    ),
                )
            } else if (!isApplicabilitySupported(policy)) {
                PolicyApplicabilityOutcome(
                    policyName = policy.name,
                    policySerial = policy.serial,
                    engineKind = policy.engineKind,
                    verdict = ApplicabilityVerdict(
                        decision = ApplicabilityDecision.NOT_APPLICABLE,
                        reason = "Unsupported applicabilityKind '${policy.applicabilityKind}'",
                    ),
                )
            } else {
                PolicyApplicabilityOutcome(
                    policyName = policy.name,
                    policySerial = policy.serial,
                    engineKind = policy.engineKind,
                    verdict = applicabilitySelector.decide(context, policy),
                )
            }
        }
    }

    private fun prepareContext(fragment: GraphFragment): PolicyEvaluationContext {
        val resolved = fragmentPolicy.resolve(fragment)
        if (resolved.hasErrors()) {
            val detail = resolved.diagnostics
                .filter { it.severity == GraphFragmentDiagnosticSeverity.ERROR }
                .joinToString("; ") { it.message }
                .ifBlank { "fragment resolve reported ERROR diagnostics" }
            throw PolicyEvaluationException("Refuse policy evaluate: $detail")
        }
        val context = PolicyEvaluationContext(fragment = resolved)
        wirers.forEach { it.wire(context) }
        return context
    }

    private fun evaluateOne(context: PolicyEvaluationContext, ref: PolicyRef): PolicyOutcome {
        val policy = repository.resolve(ref)
        if (policy == null) {
            return PolicyOutcome(
                policyName = refLabel(ref),
                policySerial = refSerialHint(ref),
                engineKind = "",
                status = PolicyOutcomeStatus.ERROR,
                message = "Policy not found: ${refLabel(ref)}",
            )
        }

        return evaluateResolved(context, policy)
    }

    private fun evaluateResolved(context: PolicyEvaluationContext, policy: Policy): PolicyOutcome {
        if (!isApplicabilitySupported(policy)) {
            return PolicyOutcome(
                policyName = policy.name,
                policySerial = policy.serial,
                engineKind = policy.engineKind,
                status = PolicyOutcomeStatus.ERROR,
                message = "Unsupported applicabilityKind '${policy.applicabilityKind}'",
            )
        }

        val verdict = applicabilitySelector.decide(context, policy)
        if (verdict.decision == ApplicabilityDecision.NOT_APPLICABLE) {
            return PolicyOutcome(
                policyName = policy.name,
                policySerial = policy.serial,
                engineKind = policy.engineKind,
                status = PolicyOutcomeStatus.NOT_APPLICABLE,
                notApplicableReason = verdict.reason,
            )
        }

        val engine = engines[policy.engineKind]
        if (engine == null) {
            return PolicyOutcome(
                policyName = policy.name,
                policySerial = policy.serial,
                engineKind = policy.engineKind,
                status = PolicyOutcomeStatus.ERROR,
                message = "No PolicyEngine registered for engineKind '${policy.engineKind}'",
            )
        }

        return try {
            val result = engine.evaluate(context, policy)
            val status = when (result.status) {
                PolicyOutcomeStatus.PASS,
                PolicyOutcomeStatus.FAIL,
                PolicyOutcomeStatus.ERROR,
                -> result.status
                PolicyOutcomeStatus.NOT_APPLICABLE -> PolicyOutcomeStatus.ERROR
            }
            PolicyOutcome(
                policyName = policy.name,
                policySerial = policy.serial,
                engineKind = policy.engineKind,
                status = status,
                findings = result.findings,
                message = when {
                    result.status == PolicyOutcomeStatus.NOT_APPLICABLE ->
                        result.message ?: "Engine must not return NOT_APPLICABLE"
                    else -> result.message
                },
            )
        } catch (ex: Exception) {
            PolicyOutcome(
                policyName = policy.name,
                policySerial = policy.serial,
                engineKind = policy.engineKind,
                status = PolicyOutcomeStatus.ERROR,
                message = ex.message ?: ex::class.simpleName,
            )
        }
    }

    private fun isApplicabilitySupported(policy: Policy): Boolean {
        if (applicabilitySelector !== AlwaysApplyApplicabilitySelector) {
            return true
        }
        return AlwaysApplyApplicabilitySelector.isSupportedKind(policy)
    }

    private fun refLabel(ref: PolicyRef): String =
        when (ref) {
            is PolicyRef.ById -> "id=${ref.id}"
            is PolicyRef.ByName -> ref.name
        }

    private fun refSerialHint(ref: PolicyRef): Long =
        when (ref) {
            is PolicyRef.ById -> 0L
            is PolicyRef.ByName -> ref.serial ?: 0L
        }
}
