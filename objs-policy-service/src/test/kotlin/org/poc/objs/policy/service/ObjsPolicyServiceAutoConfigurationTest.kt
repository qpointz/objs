package org.poc.objs.policy.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.store.GraphStore
import org.poc.objs.policy.api.ApplicabilityKinds
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyEvaluator
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.UUID

class ObjsPolicyServiceAutoConfigurationTest {

    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObjsPolicyServiceAutoConfiguration::class.java))
            .withBean(GraphStore::class.java, { mock(GraphStore::class.java) })

    @Test
    fun shouldEvaluateDrools_whenAutoconfigWiresEngineByKind() {
        contextRunner.run { context ->
            val evaluator = context.getBean(PolicyEvaluator::class.java)
            val entityId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
            val fragment =
                org.poc.objs.api.domain.DefaultGraphFragmentPolicy.resolve(
                    GraphContents(
                        entities =
                            listOf(
                                Entity(
                                    id = entityId,
                                    type = "Component",
                                    schemaVersion = "1.0.0",
                                    payload = mutableMapOf("version" to "1.0.0-SNAPSHOT"),
                                ),
                            ),
                        edges = emptyList(),
                    ),
                )
            val policy =
                Policy(
                    id = UUID.randomUUID(),
                    name = "probe",
                    version = 1L,
                    engineKind = PolicyEngineKinds.DROOLS,
                    body =
                        """
                        package org.poc.objs.policy.service.probe;
                        import org.poc.objs.policy.drools.EntityFact;
                        import org.poc.objs.policy.drools.DroolsEvaluationScratch;
                        global DroolsEvaluationScratch scratch;
                        rule "hit"
                        when
                            EntityFact( type == "Component" )
                        then
                            scratch.fail("component seen");
                        end
                        """.trimIndent(),
                    applicabilityKind = ApplicabilityKinds.ALWAYS_APPLY,
                )

            val result = evaluator.evaluatePolicies(fragment, listOf(policy))

            assertThat(result.outcomes).hasSize(1)
            assertThat(result.outcomes[0].status)
                .withFailMessage("outcome=%s", result.outcomes[0])
                .isEqualTo(PolicyOutcomeStatus.FAIL)
            assertThat(result.outcomes[0].engineKind).isEqualTo(PolicyEngineKinds.DROOLS)
            assertThat(result.outcomes[0].findings).isNotEmpty()
        }
    }
}
