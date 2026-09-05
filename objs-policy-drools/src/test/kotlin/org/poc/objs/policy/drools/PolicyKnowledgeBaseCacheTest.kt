package org.poc.objs.policy.drools

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.core.InMemoryPolicyStores

class PolicyKnowledgeBaseCacheTest {

    @Test
    fun shouldReportLineFromMessageApi_whenCompileFails() {
        val cache = PolicyKnowledgeBaseCache()
        val stores = InMemoryPolicyStores()
        val categoryId = stores.categories.save(CategoryWrite("General", "general")).id
        val policy =
            stores.policies.save(
                PolicyWrite(
                    name = "bad",
                    engineKind = PolicyEngineKinds.DROOLS,
                    categoryId = categoryId,
                    tags = listOf("test"),
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
