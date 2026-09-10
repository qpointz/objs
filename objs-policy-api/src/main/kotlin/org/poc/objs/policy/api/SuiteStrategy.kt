package org.poc.objs.policy.api

/**
 * Composable suite compute pack (C-34 / G-P56v): leaf reported severity, folder roll-up,
 * and overall status aggregation. Evaluator orchestrates only — no hard-coded aggregation outside
 * a [SuiteStrategy] implementation.
 */
interface SuiteStrategy {
    val kind: String

    fun selectExecution(executionStrategyKind: String): ExecutionStrategy

    fun leafReportedSeverity(outcome: PolicyOutcome): FindingSeverity?

    fun rollUp(
        folder: SuiteFolder,
        votingChildren: List<RollUpChild>,
    ): RollUpResult

    fun aggregateOverall(outcomes: List<PolicyOutcome>): PolicyOutcomeStatus
}

/** Well-known [PolicySuite.suiteStrategyKind] values. */
object SuiteStrategyKinds {
    const val BUILTIN: String = "BUILTIN"
}
