package org.poc.objs.policy.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.FindingSeverity
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.RollUpChild
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderRollUpModes
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import java.util.UUID

class SuiteStrategiesTest {

    private val folder = SuiteFolder(id = UUID.randomUUID(), key = "f", name = "F")

    @Test
    fun shouldRollUpAllPassMatrix_viaBuiltin() {
        val f = folder.copy(rollUpMode = SuiteFolderRollUpModes.ALL_PASS)
        val r = BuiltinSuiteRollUpStrategy.rollUp(
            f,
            listOf(
                RollUpChild(PolicyOutcomeStatus.PASS, FindingSeverity.LOW),
                RollUpChild(PolicyOutcomeStatus.FAIL, FindingSeverity.MEDIUM),
            ),
        )
        assertThat(r.status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(r.severity).isEqualTo(FindingSeverity.MEDIUM)
    }

    @Test
    fun shouldRollUpAnyPassWithWarningSeverity_viaFolderMode() {
        val f = folder.copy(rollUpMode = SuiteFolderRollUpModes.ANY_PASS)
        val r = BuiltinSuiteRollUpStrategy.rollUp(
            f,
            listOf(
                RollUpChild(PolicyOutcomeStatus.PASS, FindingSeverity.HIGH),
                RollUpChild(PolicyOutcomeStatus.FAIL, FindingSeverity.LOW),
            ),
        )
        assertThat(r.status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(r.severity).isEqualTo(FindingSeverity.HIGH)
    }

    @Test
    fun shouldHonorSeverityConfigOverride() {
        val f = folder.copy(
            rollUpMode = SuiteFolderRollUpModes.ALL_PASS,
            severityConfig = "CRITICAL",
        )
        val r = BuiltinSuiteRollUpStrategy.rollUp(
            f,
            listOf(RollUpChild(PolicyOutcomeStatus.FAIL, FindingSeverity.LOW)),
        )
        assertThat(r.severity).isEqualTo(FindingSeverity.CRITICAL)
    }

    @Test
    fun shouldResolveBuiltinStrategyKind() {
        assertThat(SuiteStrategies.rollUp(SuiteRollUpStrategyKinds.BUILTIN).kind)
            .isEqualTo(SuiteRollUpStrategyKinds.BUILTIN)
        assertThatThrownBy { SuiteStrategies.rollUp("ALL_PASS") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun shouldDedupeByPolicyId() {
        val (stores, categoryId) = PolicyTestFixtures.storesWithCategory()
        val p = stores.policies.save(PolicyTestFixtures.write("a", "PASS", categoryId))
        val selected = DedupeExecutionStrategy.selectForEvaluate(listOf(p, p))
        assertThat(selected).hasSize(1)
        assertThat(NoDedupeExecutionStrategy.selectForEvaluate(listOf(p, p))).hasSize(2)
    }
}
