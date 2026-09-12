package org.poc.objs.policy.core

import org.poc.objs.policy.api.BatchEvaluationResult
import org.poc.objs.policy.api.BatchSubject
import org.poc.objs.policy.api.BatchSubjectResult
import org.poc.objs.policy.api.BatchTarget
import org.poc.objs.policy.api.PolicyBatchEvaluator
import org.poc.objs.policy.api.PolicyBatchExecutor
import org.poc.objs.policy.api.PolicyEvaluator
import org.poc.objs.policy.api.SuiteEvaluator

/**
 * Thin facade: [PolicyBatchEvaluator] → [PolicyBatchExecutor]. G-P41b / G-P43b.
 */
class DefaultPolicyBatchEvaluator(
    private val executor: PolicyBatchExecutor,
) : PolicyBatchEvaluator {
    override fun evaluateBatch(
        subjects: List<BatchSubject>,
        target: BatchTarget,
    ): BatchEvaluationResult = executor.execute(subjects, target)
}

/**
 * Plans once, then runs subjects **sequentially** with continue-on-error. G-P42b / G-P43b.
 */
class SequentialPolicyBatchExecutor(
    private val policyEvaluator: PolicyEvaluator,
    private val suiteEvaluator: SuiteEvaluator,
) : PolicyBatchExecutor {

    override fun execute(
        subjects: List<BatchSubject>,
        target: BatchTarget,
    ): BatchEvaluationResult {
        val diagnostics = planDiagnostics(target)
        val cells = subjects.map { subject -> runSubject(subject, target) }
        return BatchEvaluationResult(cells = cells, diagnostics = diagnostics)
    }

    private fun planDiagnostics(target: BatchTarget): List<String> =
        when (target) {
            is BatchTarget.PolicyRefs ->
                if (target.refs.isEmpty()) {
                    listOf("Batch target has zero policy refs")
                } else {
                    emptyList()
                }
            is BatchTarget.Suite -> emptyList()
        }

    private fun runSubject(
        subject: BatchSubject,
        target: BatchTarget,
    ): BatchSubjectResult =
        try {
            when (target) {
                is BatchTarget.Suite -> {
                    val suiteResult =
                        suiteEvaluator.evaluateSuite(
                            subject.fragment,
                            target.suite,
                            target.scope,
                        )
                    BatchSubjectResult.Ok(subjectKey = subject.subjectKey, suite = suiteResult)
                }
                is BatchTarget.PolicyRefs -> {
                    val flat =
                        policyEvaluator.evaluate(subject.fragment, target.refs)
                    BatchSubjectResult.Ok(subjectKey = subject.subjectKey, flat = flat)
                }
            }
        } catch (ex: Exception) {
            BatchSubjectResult.Error(
                subjectKey = subject.subjectKey,
                message = ex.message?.takeIf { it.isNotBlank() } ?: ex.javaClass.simpleName,
            )
        }
}
