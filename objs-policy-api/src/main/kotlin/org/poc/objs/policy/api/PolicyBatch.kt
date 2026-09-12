package org.poc.objs.policy.api

import org.poc.objs.api.domain.GraphFragment

/**
 * One batch subject: opaque [subjectKey] (caller-interpreted) + prebuilt [fragment].
 * G-P41b.
 */
data class BatchSubject(
    val subjectKey: String,
    val fragment: GraphFragment,
)

/**
 * What to run for every subject in a batch. G-P41b.
 */
sealed class BatchTarget {
    data class Suite(
        val suite: PolicySuite,
        val scope: SuiteEvaluateScope = SuiteEvaluateScope.Full,
    ) : BatchTarget()

    data class PolicyRefs(
        val refs: List<PolicyRef>,
    ) : BatchTarget()
}

/**
 * Per-subject cell in a [BatchEvaluationResult]. G-P42b.
 */
sealed class BatchSubjectResult {
    abstract val subjectKey: String

    data class Ok(
        override val subjectKey: String,
        val flat: EvaluationResult? = null,
        val suite: SuiteEvaluationResult? = null,
    ) : BatchSubjectResult() {
        init {
            require(flat != null || suite != null) {
                "Ok cell must carry flat or suite result"
            }
        }
    }

    data class Error(
        override val subjectKey: String,
        val message: String,
    ) : BatchSubjectResult()
}

/**
 * Ordered pack of per-subject cells (same order as input subjects). G-P41b / G-P42b.
 */
data class BatchEvaluationResult(
    val cells: List<BatchSubjectResult>,
    val diagnostics: List<String> = emptyList(),
)

/**
 * Caller-facing batch port. Delegates plan+run to [PolicyBatchExecutor]. G-P41b / G-P43b.
 */
interface PolicyBatchEvaluator {
    fun evaluateBatch(
        subjects: List<BatchSubject>,
        target: BatchTarget,
    ): BatchEvaluationResult
}

/**
 * Plans (resolve target → work) and executes a batch. G-P43b.
 */
interface PolicyBatchExecutor {
    fun execute(
        subjects: List<BatchSubject>,
        target: BatchTarget,
    ): BatchEvaluationResult
}
