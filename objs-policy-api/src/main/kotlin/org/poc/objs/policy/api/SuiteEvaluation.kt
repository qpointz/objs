package org.poc.objs.policy.api

import java.util.UUID

/** Scope for [SuiteEvaluator.evaluateSuite] (G-P28s). */
sealed class SuiteEvaluateScope {
    data object Full : SuiteEvaluateScope()

    data class Subfolder(val folderId: UUID) : SuiteEvaluateScope()

    data class SubfolderSet(val folderIds: List<UUID>) : SuiteEvaluateScope()

    data class PolicySubset(val policyIds: List<UUID>) : SuiteEvaluateScope()
}

/**
 * Shared evaluation aggregate identity (flat or suite). App may mint; foundation may assign.
 */
data class PolicyEvaluationMeta(
    val evaluationId: UUID,
    val kind: String,
    val evaluatedAtEpochMs: Long,
    val executionStrategyKind: String? = null,
    val rollUpStrategyKind: String? = null,
    val overallStatus: PolicyOutcomeStatus? = null,
    val overallSeverity: String? = null,
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
    val suiteId: UUID? = null,
    val suiteName: String? = null,
)

object PolicyEvaluationKinds {
    const val FLAT: String = "FLAT"
    const val SUITE: String = "SUITE"
}

data class SuiteFolderResult(
    val folderId: UUID,
    val key: String,
    val name: String,
    val parentFolderId: UUID? = null,
    val participation: SuiteFolderParticipation,
    val status: PolicyOutcomeStatus,
    val severity: String? = null,
    val votes: Boolean,
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
    val children: List<SuiteFolderResult> = emptyList(),
    val leaves: List<SuitePolicyLeafResult> = emptyList(),
)

data class SuitePolicyLeafResult(
    val policyId: UUID,
    val policyName: String,
    val policySerial: Long,
    val policyVersion: String,
    val status: PolicyOutcomeStatus,
    val severity: String? = null,
    /** Index into [SuiteEvaluationResult.outcomes]. */
    val outcomeIndex: Int,
)

/**
 * Suite evaluation result: meta + reporting tree + flat outcomes (G-P11s).
 * No input persist in C-27.
 */
data class SuiteEvaluationResult(
    val meta: PolicyEvaluationMeta,
    val tree: SuiteFolderResult?,
    val outcomes: List<PolicyOutcome>,
)

/** Roll-up of direct voting children → folder status + severity. */
interface SuiteRollUpStrategy {
    val kind: String

    fun rollUp(
        folder: SuiteFolder,
        votingChildren: List<RollUpChild>,
    ): RollUpResult
}

data class RollUpChild(
    val status: PolicyOutcomeStatus,
    val severity: String?,
)

data class RollUpResult(
    val status: PolicyOutcomeStatus,
    val severity: String?,
)

/**
 * How selected policies are executed (dedupe vs not). Independent of suite taxonomy (G-P30s).
 */
interface ExecutionStrategy {
    val kind: String

    /**
     * @return policies to pass to [PolicyEvaluator.evaluate] (order preserved for first occurrence).
     */
    fun selectForEvaluate(resolved: List<Policy>): List<Policy>
}
