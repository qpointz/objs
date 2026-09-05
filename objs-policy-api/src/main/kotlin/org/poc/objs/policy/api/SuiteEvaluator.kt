package org.poc.objs.policy.api

import org.poc.objs.api.domain.GraphFragment
import java.util.UUID

/**
 * Suite evaluate wrapper over flat [PolicyEvaluator.evaluate] (G-P15s).
 */
interface SuiteEvaluator {
    fun evaluateSuite(
        fragment: GraphFragment,
        suite: PolicySuite,
        scope: SuiteEvaluateScope = SuiteEvaluateScope.Full,
        evaluationId: UUID = UUID.randomUUID(),
        evaluatedAtEpochMs: Long = System.currentTimeMillis(),
    ): SuiteEvaluationResult

    /** Effective policy selection for a folder (children + own matchers); for examine / preview. */
    fun effectivePolicies(suite: PolicySuite, folderId: UUID): SuiteEffectiveSet

    /** Effective policies under scope (full suite / subfolder / …). */
    fun resolveSelection(suite: PolicySuite, scope: SuiteEvaluateScope = SuiteEvaluateScope.Full): SuiteEffectiveSet
}

/**
 * Result of expanding matchers for a folder or scope.
 * [missing] lists STATIC_LIST entries that failed to resolve (when not skipMissing).
 */
data class SuiteEffectiveSet(
    val policies: List<Policy>,
    /** policyId → folder that contributed the leaf (last writer wins for placement). */
    val placementByPolicyId: Map<UUID, UUID> = emptyMap(),
    val missing: List<StaticPolicyEntry> = emptyList(),
)

/** Thrown when STATIC_LIST cannot resolve and skipMissing is false. */
class SuiteSelectionException(
    message: String,
    val missing: List<StaticPolicyEntry> = emptyList(),
) : IllegalArgumentException(message)
