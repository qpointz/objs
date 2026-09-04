package org.poc.objs.policy.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AggregateOverallTest {

    @Test
    fun shouldReturnNotApplicable_whenEmpty() {
        assertThat(aggregateOverall(emptyList())).isEqualTo(PolicyOutcomeStatus.NOT_APPLICABLE)
    }

    @Test
    fun shouldPreferError_overFailAndPass() {
        val outcomes = listOf(
            outcome(PolicyOutcomeStatus.PASS),
            outcome(PolicyOutcomeStatus.FAIL),
            outcome(PolicyOutcomeStatus.ERROR),
        )
        assertThat(aggregateOverall(outcomes)).isEqualTo(PolicyOutcomeStatus.ERROR)
    }

    @Test
    fun shouldPreferFail_overPassAndNotApplicable() {
        val outcomes = listOf(
            outcome(PolicyOutcomeStatus.PASS),
            outcome(PolicyOutcomeStatus.NOT_APPLICABLE),
            outcome(PolicyOutcomeStatus.FAIL),
        )
        assertThat(aggregateOverall(outcomes)).isEqualTo(PolicyOutcomeStatus.FAIL)
    }

    @Test
    fun shouldPreferPass_overNotApplicable() {
        val outcomes = listOf(
            outcome(PolicyOutcomeStatus.NOT_APPLICABLE),
            outcome(PolicyOutcomeStatus.PASS),
        )
        assertThat(aggregateOverall(outcomes)).isEqualTo(PolicyOutcomeStatus.PASS)
    }

    @Test
    fun shouldReturnNotApplicable_whenOnlyNotApplicable() {
        val outcomes = listOf(outcome(PolicyOutcomeStatus.NOT_APPLICABLE))
        assertThat(aggregateOverall(outcomes)).isEqualTo(PolicyOutcomeStatus.NOT_APPLICABLE)
    }

    private fun outcome(status: PolicyOutcomeStatus) = PolicyOutcome(
        policyName = "p",
        policyVersion = 1L,
        engineKind = PolicyEngineKinds.CUSTOM,
        status = status,
    )
}
