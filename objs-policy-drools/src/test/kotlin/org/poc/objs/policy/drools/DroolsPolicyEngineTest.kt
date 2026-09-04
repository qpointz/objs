package org.poc.objs.policy.drools

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyEvaluationContext
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.core.DefaultPolicyEvaluator
import org.poc.objs.policy.core.InMemoryPolicyRepository
import java.util.UUID

class DroolsPolicyEngineTest {

    private val entityA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val entityB = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val edgeAb = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")

    private val criticalComponentRule = """
        package org.poc.objs.policy.drools.fixtures;

        import org.poc.objs.policy.drools.EntityFact;
        import org.poc.objs.policy.drools.DroolsEvaluationScratch;

        global DroolsEvaluationScratch scratch;

        rule "fail when component annotation severity critical"
        when
            EntityFact( type == "Component", annotations["severity"] == "CRITICAL" )
        then
            scratch.fail("component has CRITICAL severity");
        end
    """.trimIndent()

    private val dependsOnRule = """
        package org.poc.objs.policy.drools.fixtures;

        import org.poc.objs.policy.drools.EdgeFact;
        import org.poc.objs.policy.drools.DroolsEvaluationScratch;

        global DroolsEvaluationScratch scratch;

        rule "fail when depends_on edge present"
        when
            EdgeFact( role == "depends_on" )
        then
            scratch.fail("depends_on edge found");
        end
    """.trimIndent()

    private val wiredObjectRule = """
        package org.poc.objs.policy.drools.fixtures;

        import org.poc.objs.policy.drools.ObjectFact;
        import org.poc.objs.policy.drools.DroolsEvaluationScratch;

        global DroolsEvaluationScratch scratch;

        rule "fail when wired asset severity critical"
        when
            ObjectFact( name == "asset", values["severity"] == "CRITICAL" )
        then
            scratch.fail("asset has CRITICAL severity");
        end
    """.trimIndent()

    private val passWithRuleNote = """
        package org.poc.objs.policy.drools.fixtures;

        import org.poc.objs.policy.drools.EntityFact;
        import org.poc.objs.policy.drools.DroolsEvaluationScratch;

        global DroolsEvaluationScratch scratch;

        rule "component-present"
        when
            EntityFact( type == "Component" )
        then
            scratch.pass("component ok");
        end
    """.trimIndent()

    @Test
    fun shouldPass_whenEntityAnnotationNotCritical() {
        val engine = DroolsPolicyEngine()
        val policy = savedPolicy(criticalComponentRule)
        val context = PolicyEvaluationContext(fragment = fragmentWithSeverity("LOW"))

        val result = engine.evaluate(context, policy)

        assertThat(result.status)
            .withFailMessage("status=${result.status} message=${result.message}")
            .isEqualTo(PolicyOutcomeStatus.PASS)
    }

    @Test
    fun shouldFail_whenEntityAnnotationCritical() {
        val engine = DroolsPolicyEngine()
        val policy = savedPolicy(criticalComponentRule)
        val context = PolicyEvaluationContext(fragment = fragmentWithSeverity("CRITICAL"))

        val result = engine.evaluate(context, policy)

        assertThat(result.status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(result.findings).singleElement().satisfies({ f ->
            assertThat(f.message).isEqualTo(
                "[fail when component annotation severity critical] component has CRITICAL severity",
            )
            assertThat(f.extras[DroolsEvaluationScratch.EXTRA_RULE])
                .isEqualTo("fail when component annotation severity critical")
        })
    }

    @Test
    fun shouldFail_whenEdgeRoleMatches() {
        val engine = DroolsPolicyEngine()
        val policy = savedPolicy(dependsOnRule)
        val context = PolicyEvaluationContext(fragment = fragmentWithEdge())

        val result = engine.evaluate(context, policy)

        assertThat(result.status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(result.findings).singleElement().extracting { it.message }
            .isEqualTo("[fail when depends_on edge present] depends_on edge found")
    }

    @Test
    fun shouldFail_whenWiredObjectFactMatches() {
        val engine = DroolsPolicyEngine()
        val policy = savedPolicy(wiredObjectRule)
        val context = PolicyEvaluationContext(
            fragment = okFragment(),
            facts = mutableMapOf("asset" to mapOf("severity" to "CRITICAL")),
        )

        val result = engine.evaluate(context, policy)

        assertThat(result.status).isEqualTo(PolicyOutcomeStatus.FAIL)
        assertThat(result.findings).singleElement().extracting { it.message }
            .isEqualTo("[fail when wired asset severity critical] asset has CRITICAL severity")
    }

    @Test
    fun shouldSurfaceRuleName_onPassFinding() {
        val engine = DroolsPolicyEngine()
        val policy = savedPolicy(passWithRuleNote)
        val context = PolicyEvaluationContext(fragment = okFragment())

        val result = engine.evaluate(context, policy)

        assertThat(result.status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(result.findings).singleElement().satisfies({ f ->
            assertThat(f.severity).isEqualTo("OK")
            assertThat(f.message).isEqualTo("[component-present] component ok")
            assertThat(f.extras[DroolsEvaluationScratch.EXTRA_RULE]).isEqualTo("component-present")
        })
    }

    @Test
    fun shouldError_whenBodyDoesNotCompile() {
        val engine = DroolsPolicyEngine()
        val policy = savedPolicy("this is not valid drl {{{")

        val result = engine.evaluate(PolicyEvaluationContext(okFragment()), policy)

        assertThat(result.status).isEqualTo(PolicyOutcomeStatus.ERROR)
        assertThat(result.message).containsIgnoringCase("compile")
    }

    @Test
    fun shouldReuseCompiledKb_acrossCallsForSamePolicyRevision() {
        val cache = PolicyKnowledgeBaseCache()
        val engine = DroolsPolicyEngine(cache)
        val policy = savedPolicy(criticalComponentRule)
        val ctx = PolicyEvaluationContext(fragment = fragmentWithSeverity("LOW"))

        assertThat(engine.evaluate(ctx, policy).status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(cache.size()).isEqualTo(1)

        assertThat(engine.evaluate(ctx, policy).status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(cache.size()).isEqualTo(1)

        cache.invalidate(policy.id)
        assertThat(cache.size()).isZero()
        assertThat(engine.evaluate(ctx, policy).status).isEqualTo(PolicyOutcomeStatus.PASS)
        assertThat(cache.size()).isEqualTo(1)
    }

    @Test
    fun shouldIntegrateWithDefaultPolicyEvaluator_failViaFragment() {
        val repo = InMemoryPolicyRepository()
        val saved = repo.save(
            PolicyWrite(
                name = "critical-component",
                engineKind = PolicyEngineKinds.DROOLS,
                body = criticalComponentRule,
            ),
        )
        val evaluator = DefaultPolicyEvaluator(
            repository = repo,
            engines = mapOf(PolicyEngineKinds.DROOLS to DroolsPolicyEngine()),
        )

        val result = evaluator.evaluate(
            fragmentWithSeverity("CRITICAL").asGraphContents(),
            listOf(PolicyRef.ByName("critical-component")),
        )

        assertThat(result.outcomes).singleElement().satisfies({ o ->
            assertThat(o.status).isEqualTo(PolicyOutcomeStatus.FAIL)
            assertThat(o.policyVersion).isEqualTo(saved.version)
            assertThat(o.engineKind).isEqualTo(PolicyEngineKinds.DROOLS)
            assertThat(o.findings).isNotEmpty()
        })
    }

    @Test
    fun shouldProjectEntityFactMetadata() {
        val entity = Entity(
            entityA,
            "Component",
            "1",
            mutableMapOf("name" to "svc"),
            mutableMapOf("severity" to "HIGH"),
        )
        val fact = EntityFact.from(entity)

        assertThat(fact.type).isEqualTo("Component")
        assertThat(fact.schema).isEqualTo("Component")
        assertThat(fact.schemaVersion).isEqualTo("1")
        assertThat(fact.annotations["severity"]).isEqualTo("HIGH")
        assertThat(fact["name"]).isEqualTo("svc")
    }

    private fun savedPolicy(body: String) =
        InMemoryPolicyRepository().save(
            PolicyWrite(
                name = "fixture",
                engineKind = PolicyEngineKinds.DROOLS,
                body = body,
            ),
        )

    private fun okFragment() =
        DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(Entity(entityA, "Component", "1")),
                edges = emptyList(),
            ),
        )

    private fun fragmentWithSeverity(severity: String) =
        DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(
                    Entity(
                        entityA,
                        "Component",
                        "1",
                        mutableMapOf(),
                        mutableMapOf("severity" to severity),
                    ),
                ),
                edges = emptyList(),
            ),
        )

    private fun fragmentWithEdge() =
        DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = listOf(
                    Entity(entityA, "Component", "1"),
                    Entity(entityB, "Component", "1"),
                ),
                edges = listOf(
                    Edge(edgeAb, source = entityA, target = entityB, role = "depends_on"),
                ),
            ),
        )
}
