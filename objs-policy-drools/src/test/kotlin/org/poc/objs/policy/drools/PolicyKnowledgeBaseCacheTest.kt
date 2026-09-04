package org.poc.objs.policy.drools

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.core.InMemoryPolicyRepository

class PolicyKnowledgeBaseCacheTest {

    @Test
    fun shouldReportLineFromMessageApi_whenCompileFails() {
        val cache = PolicyKnowledgeBaseCache()
        val policy =
            InMemoryPolicyRepository().save(
                PolicyWrite(
                    name = "bad",
                    engineKind = PolicyEngineKinds.DROOLS,
                    body =
                        """
                        package org.poc.objs.policy.drools.bad;
                        import org.poc.objs.policy.drools.DroolsEvaluationScratch;
                        global DroolsEvaluationScratch scratch;
                        rule "broken"
                        when
                            thisIsNotValidSyntax !!!
                        then
                            scratch.fail("x");
                        end
                        """.trimIndent(),
                ),
            )

        val issues = cache.tryCompile(policy)

        assertThat(issues).isNotEmpty
        assertThat(issues[0].message).isNotBlank
        assertThat(issues[0].message).doesNotContain("Java source of")
        // Line comes from Message.getLine() when Drools reports it (often > 0 for DRL syntax errors).
        assertThat(issues.any { it.line != null && it.line!! > 0 }).isTrue()
    }
}
