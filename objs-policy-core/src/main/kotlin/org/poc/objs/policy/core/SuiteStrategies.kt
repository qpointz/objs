package org.poc.objs.policy.core

import org.poc.objs.policy.api.ExecutionStrategy
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.RollUpChild
import org.poc.objs.policy.api.RollUpResult
import org.poc.objs.policy.api.SuiteExecutionStrategyKinds
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderRollUpModes
import org.poc.objs.policy.api.SuiteRollUpStrategy
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds

/**
 * Built-in suite roll-up engine (kind [SuiteRollUpStrategyKinds.BUILTIN]).
 * Interprets each folder's [SuiteFolder.rollUpMode] + [SuiteFolder.severityConfig].
 */
object BuiltinSuiteRollUpStrategy : SuiteRollUpStrategy {
    override val kind: String = SuiteRollUpStrategyKinds.BUILTIN

    override fun rollUp(folder: SuiteFolder, votingChildren: List<RollUpChild>): RollUpResult {
        val status = when (folder.rollUpMode.trim().uppercase()) {
            SuiteFolderRollUpModes.ANY_PASS -> statusAnyPass(votingChildren)
            SuiteFolderRollUpModes.ALL_PASS -> statusAllPass(votingChildren)
            else -> throw IllegalArgumentException(
                "Unknown folder rollUpMode '${folder.rollUpMode}' (use ALL_PASS or ANY_PASS)",
            )
        }
        return RollUpResult(status, severityFor(folder, status, votingChildren))
    }
}

private fun statusAllPass(votingChildren: List<RollUpChild>): PolicyOutcomeStatus {
    if (votingChildren.isEmpty()) return PolicyOutcomeStatus.NOT_APPLICABLE
    if (votingChildren.any { it.status == PolicyOutcomeStatus.ERROR }) return PolicyOutcomeStatus.ERROR
    if (votingChildren.any { it.status == PolicyOutcomeStatus.FAIL }) return PolicyOutcomeStatus.FAIL
    if (votingChildren.any { it.status == PolicyOutcomeStatus.PASS }) return PolicyOutcomeStatus.PASS
    return PolicyOutcomeStatus.NOT_APPLICABLE
}

private fun statusAnyPass(votingChildren: List<RollUpChild>): PolicyOutcomeStatus {
    if (votingChildren.isEmpty()) return PolicyOutcomeStatus.NOT_APPLICABLE
    if (votingChildren.any { it.status == PolicyOutcomeStatus.ERROR }) return PolicyOutcomeStatus.ERROR
    if (votingChildren.any { it.status == PolicyOutcomeStatus.PASS }) return PolicyOutcomeStatus.PASS
    if (votingChildren.any { it.status == PolicyOutcomeStatus.FAIL }) return PolicyOutcomeStatus.FAIL
    return PolicyOutcomeStatus.NOT_APPLICABLE
}

/** Shared severity rules for built-in roll-ups (G-P29s). */
private fun severityFor(
    folder: SuiteFolder,
    status: PolicyOutcomeStatus,
    votingChildren: List<RollUpChild>,
): String? {
    val poolMax = votingChildren.mapNotNull { it.severity }.maxByOrNull { SeverityRank.rank(it) }
    return when (status) {
        PolicyOutcomeStatus.FAIL, PolicyOutcomeStatus.ERROR ->
            folder.severityConfig?.trim()?.takeIf { it.isNotEmpty() } ?: poolMax
        PolicyOutcomeStatus.PASS -> poolMax
        PolicyOutcomeStatus.NOT_APPLICABLE -> null
    }
}

object SeverityRank {
    fun rank(severity: String): Int =
        when (severity.trim().uppercase()) {
            "CRITICAL" -> 5
            "HIGH" -> 4
            "MEDIUM", "MODERATE" -> 3
            "LOW" -> 2
            "INFO", "INFORMATIONAL" -> 1
            else -> 0
        }
}

object DedupeExecutionStrategy : ExecutionStrategy {
    override val kind: String = SuiteExecutionStrategyKinds.DEDUPE

    override fun selectForEvaluate(resolved: List<Policy>): List<Policy> {
        val seen = linkedSetOf<java.util.UUID>()
        return resolved.filter { seen.add(it.id) }
    }
}

object NoDedupeExecutionStrategy : ExecutionStrategy {
    override val kind: String = SuiteExecutionStrategyKinds.NO_DEDUPE

    override fun selectForEvaluate(resolved: List<Policy>): List<Policy> = resolved
}

object SuiteStrategies {
    private val rollUps: Map<String, SuiteRollUpStrategy> =
        listOf(BuiltinSuiteRollUpStrategy).associateBy { it.kind }
    private val executions: Map<String, ExecutionStrategy> =
        listOf(DedupeExecutionStrategy, NoDedupeExecutionStrategy).associateBy { it.kind }

    fun rollUp(kind: String): SuiteRollUpStrategy =
        rollUps[kind] ?: throw IllegalArgumentException("Unknown rollUpStrategyKind: $kind")

    fun execution(kind: String): ExecutionStrategy =
        executions[kind] ?: throw IllegalArgumentException("Unknown executionStrategyKind: $kind")
}
