package org.poc.objs.policy.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.policy.api.BatchSubject
import org.poc.objs.policy.api.BatchSubjectResult
import org.poc.objs.policy.api.BatchTarget
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import java.util.UUID

class SequentialPolicyBatchExecutorTest {

    private val stores = InMemoryPolicyStores()
    private val categoryId = stores.categories.save(
        org.poc.objs.policy.api.CategoryWrite(name = "General", key = "general"),
    ).id
    private val policyEvaluator = DefaultPolicyEvaluator(stores.policies)
    private val suiteEvaluator = DefaultSuiteEvaluator(policyEvaluator, stores.policies, stores.categories)
    private val batch = DefaultPolicyBatchEvaluator(
        SequentialPolicyBatchExecutor(policyEvaluator, suiteEvaluator),
    )

    private val entityA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val entityB = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val edgeAb = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")

    @Test
    fun shouldPreserveOrder_andContinueWhenOneSubjectFails_flatRefs() {
        stores.policies.save(write("ok", "PASS"))
        val ok = okFragment()
        val bad = conflictingFragment()

        val pack = batch.evaluateBatch(
            subjects = listOf(
                BatchSubject("A", ok),
                BatchSubject("B", bad),
                BatchSubject("C", ok),
            ),
            target = BatchTarget.PolicyRefs(listOf(PolicyRef.ByKey("ok"))),
        )

        assertThat(pack.cells.map { it.subjectKey }).containsExactly("A", "B", "C")
        assertThat(pack.cells[0]).isInstanceOf(BatchSubjectResult.Ok::class.java)
        assertThat((pack.cells[0] as BatchSubjectResult.Ok).flat!!.overall)
            .isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(pack.cells[1]).isInstanceOf(BatchSubjectResult.Error::class.java)
        assertThat((pack.cells[1] as BatchSubjectResult.Error).message).isNotBlank()
        assertThat(pack.cells[2]).isInstanceOf(BatchSubjectResult.Ok::class.java)
    }

    @Test
    fun shouldEvaluateSuiteTarget_forEachSubject() {
        val pass = stores.policies.save(write("suite-ok", "PASS"))
        val rootId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            key = "batch-suite",
            name = "Batch Suite",
            rollUpStrategyKind = SuiteRollUpStrategyKinds.BUILTIN,
            folders = listOf(
                SuiteFolder(
                    id = rootId,
                    key = "root",
                    name = "Root",
                    matchers = listOf(
                        StaticListMatcher(
                            entries = listOf(StaticPolicyEntry(pass.id)),
                        ),
                    ),
                ),
            ),
        )

        val pack = batch.evaluateBatch(
            subjects = listOf(BatchSubject("app-1", okFragment())),
            target = BatchTarget.Suite(suite),
        )

        val cell = pack.cells.single() as BatchSubjectResult.Ok
        assertThat(cell.suite).isNotNull
        assertThat(cell.suite!!.outcomes).isNotEmpty
        assertThat(cell.suite!!.meta.overallStatus).isEqualTo(PolicyOutcomeStatus.PASS)
    }

    @Test
    fun shouldEmitDiagnostic_whenPolicyRefsEmpty() {
        val pack = batch.evaluateBatch(
            subjects = listOf(BatchSubject("A", okFragment())),
            target = BatchTarget.PolicyRefs(emptyList()),
        )
        assertThat(pack.diagnostics).containsExactly("Batch target has zero policy refs")
        assertThat(pack.cells.single()).isInstanceOf(BatchSubjectResult.Ok::class.java)
    }

    private fun write(name: String, body: String) = PolicyWrite(
        key = name,
        name = name,
        engineKind = PolicyEngineKinds.CUSTOM,
        body = body,
        categoryId = categoryId,
        tags = listOf("test"),
    )

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

    private fun conflictingFragment(): GraphContents =
        GraphContents(
            entities = listOf(
                Entity(entityA, "Component", "1", mutableMapOf("name" to "a")),
                Entity(entityA, "Component", "1", mutableMapOf("name" to "b")),
            ),
            edges = emptyList(),
        )
}
