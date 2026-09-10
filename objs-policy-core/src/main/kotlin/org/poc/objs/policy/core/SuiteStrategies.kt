package org.poc.objs.policy.core

import org.poc.objs.policy.api.ExecutionStrategy
import org.poc.objs.policy.api.FindingSeverity
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyOutcome
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.RollUpChild
import org.poc.objs.policy.api.RollUpResult
import org.poc.objs.policy.api.SuiteExecutionStrategyKinds
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderRollUpModes
import org.poc.objs.policy.api.SuiteRollUpStrategy
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import org.poc.objs.policy.api.SuiteStrategy
import org.poc.objs.policy.api.SuiteStrategyKinds

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
    if (votingChildren.any { it.status == PolicyOutcomeStatus.EXEC_ERROR }) return PolicyOutcomeStatus.EXEC_ERROR
    if (votingChildren.any { it.status == PolicyOutcomeStatus.FAIL }) return PolicyOutcomeStatus.FAIL
    if (votingChildren.any { it.status == PolicyOutcomeStatus.PASS }) return PolicyOutcomeStatus.PASS
    return PolicyOutcomeStatus.NOT_APPLICABLE
}

private fun statusAnyPass(votingChildren: List<RollUpChild>): PolicyOutcomeStatus {
    if (votingChildren.isEmpty()) return PolicyOutcomeStatus.NOT_APPLICABLE
    if (votingChildren.any { it.status == PolicyOutcomeStatus.EXEC_ERROR }) return PolicyOutcomeStatus.EXEC_ERROR
    if (votingChildren.any { it.status == PolicyOutcomeStatus.PASS }) return PolicyOutcomeStatus.PASS
    if (votingChildren.any { it.status == PolicyOutcomeStatus.FAIL }) return PolicyOutcomeStatus.FAIL
    return PolicyOutcomeStatus.NOT_APPLICABLE
}

/** Shared severity rules for built-in roll-ups (G-P29s / G-P55v). */
private fun severityFor(
    folder: SuiteFolder,
    status: PolicyOutcomeStatus,
    votingChildren: List<RollUpChild>,
): FindingSeverity? {
    val poolMax = votingChildren.mapNotNull { it.severity }.maxByOrNull { SeverityRank.rank(it) }
    val config = FindingSeverity.parseOrNull(folder.severityConfig)
    return when (status) {
        PolicyOutcomeStatus.FAIL, PolicyOutcomeStatus.EXEC_ERROR -> config ?: poolMax
        PolicyOutcomeStatus.PASS -> poolMax
        PolicyOutcomeStatus.NOT_APPLICABLE -> null
    }
}

object SeverityRank {
    fun rank(severity: FindingSeverity): Int =
        when (severity) {
            FindingSeverity.CRITICAL -> 5
            FindingSeverity.HIGH -> 4
            FindingSeverity.MEDIUM -> 3
            FindingSeverity.LOW -> 2
            FindingSeverity.INFO -> 1
        }

    /** Rank canonical token only; unknown → 0. */
    fun rank(severity: String): Int =
        FindingSeverity.parseOrNull(severity)?.let { rank(it) } ?: 0
}

/**
 * Builtin suite compute pack: folder matrices, bare EXEC_ERROR→HIGH leaf severity, overall precedence.
 */
object BuiltinSuiteStrategy : SuiteStrategy {
    override val kind: String = SuiteStrategyKinds.BUILTIN

    override fun selectExecution(executionStrategyKind: String): ExecutionStrategy =
        SuiteStrategies.execution(executionStrategyKind)

    override fun leafReportedSeverity(outcome: PolicyOutcome): FindingSeverity? {
        val fromFindings = outcome.findings.mapNotNull { it.severity }.maxByOrNull { SeverityRank.rank(it) }
        if (fromFindings != null) return fromFindings
        return when (outcome.status) {
            PolicyOutcomeStatus.EXEC_ERROR -> FindingSeverity.HIGH
            else -> null
        }
    }

    override fun rollUp(folder: SuiteFolder, votingChildren: List<RollUpChild>): RollUpResult =
        BuiltinSuiteRollUpStrategy.rollUp(folder, votingChildren)

    override fun aggregateOverall(outcomes: List<PolicyOutcome>): PolicyOutcomeStatus =
        org.poc.objs.policy.api.aggregateOverall(outcomes)
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
    private val suites: Map<String, SuiteStrategy> =
        listOf(BuiltinSuiteStrategy).associateBy { it.kind }

    fun rollUp(kind: String): SuiteRollUpStrategy =
        rollUps[kind] ?: throw IllegalArgumentException("Unknown rollUpStrategyKind: $kind")

    fun execution(kind: String): ExecutionStrategy =
        executions[kind] ?: throw IllegalArgumentException("Unknown executionStrategyKind: $kind")

    fun suite(kind: String): SuiteStrategy =
        suites[kind] ?: throw IllegalArgumentException("Unknown suiteStrategyKind: $kind")
}
