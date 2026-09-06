package org.poc.objs.policy.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.policy.api.PolicyEvaluationKinds
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteEvaluateScope
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderRollUpModes
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import org.poc.objs.policy.core.PolicyTestFixtures.write
import java.util.UUID

class DefaultSuiteEvaluatorTest {

    private val stores = InMemoryPolicyStores()
    private val categoryId = stores.categories.save(
        org.poc.objs.policy.api.CategoryWrite(name = "General", key = "general"),
    ).id
    private val policyEvaluator = DefaultPolicyEvaluator(stores.policies)
    private val suiteEvaluator = DefaultSuiteEvaluator(policyEvaluator, stores.policies, stores.categories)

    private val entityA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val entityB = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val edgeAb = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")

    @Test
    fun shouldEvaluateSuite_allPassAndFailRollUp() {
        val pass = stores.policies.save(write("ok", "PASS", categoryId))
        val fail = stores.policies.save(write("bad", "FAIL", categoryId))
        val rootId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            key = "baseline",
            name = "Baseline",
            rollUpStrategyKind = SuiteRollUpStrategyKinds.BUILTIN,
            folders = listOf(
                SuiteFolder(
                    id = rootId,
                    key = "root",
                    name = "Root",
                    matchers = listOf(
                        StaticListMatcher(
                            entries = listOf(
                                StaticPolicyEntry(pass.id),
                                StaticPolicyEntry(fail.id),
                            ),
                        ),
                    ),
                    tags = listOf("t"),
                ),
            ),
            tags = listOf("suite"),
        )

        val result = suiteEvaluator.evaluateSuite(okFragment(), suite)
        assertThat(result.meta.kind).isEqualTo(PolicyEvaluationKinds.SUITE)
        assertThat(result.meta.evaluationId).isNotNull()
        assertThat(result.meta.overallStatus).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(result.outcomes).hasSize(2)
        assertThat(result.tree?.status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(result.tree?.leaves).hasSize(2)
        assertThat(result.tree?.leaves?.map { it.outcomeIndex }).containsExactlyInAnyOrder(0, 1)
    }

    @Test
    fun shouldOmitDisabledSubtree_andHonorIgnoredNonVoting() {
        val pass = stores.policies.save(write("ok", "PASS", categoryId))
        val fail = stores.policies.save(write("bad", "FAIL", categoryId))
        val rootId = UUID.randomUUID()
        val ignoredId = UUID.randomUUID()
        val disabledId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            key = "s",
            name = "S",
            rollUpStrategyKind = SuiteRollUpStrategyKinds.BUILTIN,
            folders = listOf(
                SuiteFolder(id = rootId, key = "root", name = "Root"),
                SuiteFolder(
                    id = ignoredId,
                    key = "ign",
                    name = "Ignored",
                    parentId = rootId,
                    participation = SuiteFolderParticipation.IGNORED,
                    matchers = listOf(StaticListMatcher(entries = listOf(StaticPolicyEntry(fail.id)))),
                ),
                SuiteFolder(
                    id = disabledId,
                    key = "off",
                    name = "Off",
                    parentId = rootId,
                    participation = SuiteFolderParticipation.DISABLED,
                    matchers = listOf(StaticListMatcher(entries = listOf(StaticPolicyEntry(pass.id)))),
                ),
            ),
        )
        // Root has no matchers; ignored contributes fail locally but does not vote → root N/A or PASS empty
        val result = suiteEvaluator.evaluateSuite(okFragment(), suite)
        assertThat(result.tree?.children?.map { it.key }).containsExactly("ign")
        assertThat(result.tree?.children?.single()?.votes).isFalse()
        assertThat(result.tree?.status).isEqualTo(PolicyOutcomeStatus.NOT_APPLICABLE)
        assertThat(result.outcomes.map { it.policyName }).containsExactly("bad")
    }

    @Test
    fun shouldEvaluateSubfolderScope() {
        val pass = stores.policies.save(write("ok", "PASS", categoryId))
        val fail = stores.policies.save(write("bad", "FAIL", categoryId))
        val rootId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            key = "s",
            name = "S",
            folders = listOf(
                SuiteFolder(
                    id = rootId,
                    key = "root",
                    name = "Root",
                    matchers = listOf(StaticListMatcher(entries = listOf(StaticPolicyEntry(fail.id)))),
                ),
                SuiteFolder(
                    id = childId,
                    key = "child",
                    name = "Child",
                    parentId = rootId,
                    matchers = listOf(StaticListMatcher(entries = listOf(StaticPolicyEntry(pass.id)))),
                ),
            ),
        )
        val result = suiteEvaluator.evaluateSuite(
            okFragment(),
            suite,
            SuiteEvaluateScope.Subfolder(childId),
        )
        assertThat(result.tree?.folderId).isEqualTo(childId)
        assertThat(result.tree?.status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(result.outcomes).hasSize(1)
        assertThat(result.outcomes.single().policyName).isEqualTo("ok")
    }

    @Test
    fun shouldUseAnyPassFolderMode() {
        val pass = stores.policies.save(write("ok", "PASS", categoryId))
        val fail = stores.policies.save(write("bad", "FAIL", categoryId))
        val rootId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            key = "s",
            name = "S",
            rollUpStrategyKind = SuiteRollUpStrategyKinds.BUILTIN,
            folders = listOf(
                SuiteFolder(
                    id = rootId,
                    key = "root",
                    name = "Root",
                    rollUpMode = SuiteFolderRollUpModes.ANY_PASS,
                    matchers = listOf(
                        StaticListMatcher(
                            entries = listOf(
                                StaticPolicyEntry(pass.id),
                                StaticPolicyEntry(fail.id),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val result = suiteEvaluator.evaluateSuite(okFragment(), suite)
        assertThat(result.meta.overallStatus).isEqualTo(PolicyOutcomeStatus.PASS)
    }

    private fun okFragment(): GraphContents {
        val contents = GraphContents(
            entities = listOf(
                Entity(entityA, "Component", "1"),
                Entity(entityB, "Component", "1"),
            ),
            edges = listOf(
                Edge(edgeAb, source = entityA, target = entityB, role = "depends_on"),
            ),
        )
        return DefaultGraphFragmentPolicy.resolve(contents).asGraphContents()
    }
}
