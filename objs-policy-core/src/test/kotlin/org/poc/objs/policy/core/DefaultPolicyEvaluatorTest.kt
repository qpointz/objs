package org.poc.objs.policy.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.policy.api.ApplicabilityDecision
import org.poc.objs.policy.api.ApplicabilitySelector
import org.poc.objs.policy.api.ApplicabilityVerdict
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyContextWirer
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyEvaluationContext
import org.poc.objs.policy.api.PolicyEvaluationException
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.core.PolicyTestFixtures.storesWithCategory
import org.poc.objs.policy.core.PolicyTestFixtures.write as fixtureWrite
import java.util.UUID

class DefaultPolicyEvaluatorTest {

    private val stores = InMemoryPolicyStores()
    private val categoryId = stores.categories.save(
        org.poc.objs.policy.api.CategoryWrite(displayName = "General", slug = "general"),
    ).id

    private val entityA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val entityB = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val edgeAb = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")

    @Test
    fun shouldEvaluateHappyPath_withOverallAndVersionCitation() {
        val repo = stores.policies
        val pass = repo.save(write("ok", "PASS"))
        val fail = repo.save(write("bad", "FAIL"))
        val evaluator = DefaultPolicyEvaluator(repo)

        val result = evaluator.evaluate(
            okFragment(),
            listOf(PolicyRef.ByName("ok"), PolicyRef.ByName("bad")),
        )

        assertThat(result.outcomes).hasSize(2)
        assertThat(result.outcomes[0].status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(result.outcomes[0].policySerial).isEqualTo(pass.serial)
        assertThat(result.outcomes[1].status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(result.outcomes[1].policySerial).isEqualTo(fail.serial)
        assertThat(result.overall).isEqualTo(PolicyOutcomeStatus.FAIL)
    }

    @Test
    fun shouldRefuse_whenFragmentHasErrors() {
        val repo = stores.policies
        repo.save(write("ok", "PASS"))
        val evaluator = DefaultPolicyEvaluator(repo)

        assertThatThrownBy {
            evaluator.evaluate(conflictingFragment(), listOf(PolicyRef.ByName("ok")))
        }.isInstanceOf(PolicyEvaluationException::class.java)
            .hasMessageContaining("Refuse policy evaluate")
    }

    @Test
    fun shouldPreviewApplicability_withoutEngineSideEffects() {
        val repo = stores.policies
        repo.save(write("ok", "PASS"))
        var engineCalls = 0
        val engines = mapOf(
            PolicyEngineKinds.CUSTOM to org.poc.objs.policy.api.PolicyEngine { _, _ ->
                engineCalls++
                org.poc.objs.policy.api.PolicyEngineResult(PolicyOutcomeStatus.PASS)
            },
        )
        val evaluator = DefaultPolicyEvaluator(repository = repo, engines = engines)

        val preview = evaluator.applicability(okFragment(), listOf(PolicyRef.ByName("ok")))
        assertThat(preview).hasSize(1)
        assertThat(preview[0].verdict.decision).isEqualTo(ApplicabilityDecision.IN_SCOPE)
        assertThat(engineCalls).isZero()
    }

    @Test
    fun shouldContinueAfterFailAndError_andCiteLatestVersion() {
        val repo = stores.policies
        repo.save(write("a", "FAIL"))
        val aLatest = repo.save(write("a", "PASS"))
        repo.save(write("b", "ERROR"))
        repo.save(
            PolicyWrite(
                name = "c",
                engineKind = "DROOLS",
                body = "x",
                categoryId = categoryId,
                tags = listOf("test"),
            ),
        )
        repo.save(
            PolicyWrite(
                name = "d",
                engineKind = PolicyEngineKinds.CUSTOM,
                body = "PASS",
                applicabilityKind = "HAS_DATABASE",
                categoryId = categoryId,
                tags = listOf("test"),
            ),
        )
        val evaluator = DefaultPolicyEvaluator(repo)

        val result = evaluator.evaluate(
            okFragment(),
            listOf(
                PolicyRef.ByName("a"),
                PolicyRef.ByName("b"),
                PolicyRef.ByName("c"),
                PolicyRef.ByName("d"),
            ),
        )

        assertThat(result.outcomes.map { it.status }).containsExactly(
            PolicyOutcomeStatus.PASS,
            PolicyOutcomeStatus.ERROR,
            PolicyOutcomeStatus.ERROR,
            PolicyOutcomeStatus.ERROR,
        )
        assertThat(result.outcomes[0].policySerial).isEqualTo(aLatest.serial)
        assertThat(result.outcomes[2].message).contains("engineKind")
        assertThat(result.outcomes[3].message).contains("applicabilityKind")
        assertThat(result.overall).isEqualTo(PolicyOutcomeStatus.ERROR)
    }

    @Test
    fun shouldResolvePinnedVersion_andMissingRefAsError() {
        val repo = stores.policies
        val v1 = repo.save(write("gate", "FAIL"))
        val v2 = repo.save(write("gate", "PASS"))
        val evaluator = DefaultPolicyEvaluator(repo)

        val result = evaluator.evaluate(
            okFragment(),
            listOf(
                PolicyRef.ByName("gate", serial = v1.serial),
                PolicyRef.ByName("gate"),
                PolicyRef.ByName("missing"),
                PolicyRef.ById(v2.id),
            ),
        )

        assertThat(result.outcomes[0].status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(result.outcomes[0].policySerial).isEqualTo(v1.serial)
        assertThat(result.outcomes[1].status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(result.outcomes[1].policySerial).isEqualTo(v2.serial)
        assertThat(result.outcomes[2].status).isEqualTo(PolicyOutcomeStatus.ERROR)
        assertThat(result.outcomes[2].message).contains("not found")
        assertThat(result.outcomes[3].status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(result.outcomes[3].policySerial).isEqualTo(v2.serial)
    }

    @Test
    fun shouldRecordNotApplicable_whenSelectorSaysSo() {
        val repo = stores.policies
        repo.save(write("gate", "FAIL"))
        val selector = ApplicabilitySelector { _, _ ->
            ApplicabilityVerdict(ApplicabilityDecision.NOT_APPLICABLE, reason = "no database")
        }
        val evaluator = DefaultPolicyEvaluator(repository = repo, applicabilitySelector = selector)

        val result = evaluator.evaluate(okFragment(), listOf(PolicyRef.ByName("gate")))
        assertThat(result.outcomes).singleElement().satisfies({
            assertThat(it.status).isEqualTo(PolicyOutcomeStatus.NOT_APPLICABLE)
            assertThat(it.notApplicableReason).isEqualTo("no database")
            assertThat(it.findings).isEmpty()
        })
        assertThat(result.overall).isEqualTo(PolicyOutcomeStatus.NOT_APPLICABLE)
    }

    @Test
    fun shouldWireContextBeforeEvaluate_andSupportFindings() {
        val repo = stores.policies
        repo.save(
            write(
                "with-findings",
                """
                FAIL
                FINDING|too low|$entityA|
                FINDING|forbidden||$edgeAb
                """.trimIndent(),
            ),
        )
        val wirer = PolicyContextWirer { ctx -> ctx.facts["wired"] = true }
        var sawWired = false
        val engines = mapOf(
            PolicyEngineKinds.CUSTOM to org.poc.objs.policy.api.PolicyEngine { context, policy ->
                sawWired = context.facts["wired"] == true
                CustomPolicyEngine.evaluate(context, policy)
            },
        )
        val evaluator = DefaultPolicyEvaluator(
            repository = repo,
            wirers = listOf(wirer),
            engines = engines,
        )

        val result = evaluator.evaluate(okFragment(), listOf(PolicyRef.ByName("with-findings")))
        assertThat(sawWired).isTrue()
        val outcome = result.outcomes.single()
        assertThat(outcome.status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(outcome.findings).hasSize(2)
        assertThat(outcome.findings[0].entities).containsExactly(entityA)
        assertThat(outcome.findings[1].edges).containsExactly(edgeAb)
    }

    @Test
    fun shouldAllowFailWithZeroFindings() {
        val repo = stores.policies
        repo.save(write("bare-fail", "FAIL"))
        val evaluator = DefaultPolicyEvaluator(repo)

        val outcome = evaluator.evaluate(okFragment(), listOf(PolicyRef.ByName("bare-fail"))).outcomes.single()
        assertThat(outcome.status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(outcome.findings).isEmpty()
    }

    private fun write(name: String, body: String) = PolicyWrite(
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
