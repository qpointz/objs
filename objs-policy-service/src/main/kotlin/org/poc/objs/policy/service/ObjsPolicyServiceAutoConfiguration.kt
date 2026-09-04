package org.poc.objs.policy.service

import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyEvaluator
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.core.DefaultPolicyEvaluator
import org.poc.objs.policy.core.InMemoryPolicyRepository
import org.poc.objs.policy.drools.DroolsPolicyEngine
import org.poc.objs.policy.drools.PolicyKnowledgeBaseCache
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@AutoConfiguration
@ComponentScan(basePackages = ["org.poc.objs.policy.service"])
class ObjsPolicyServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun policyRepository(): PolicyRepository = InMemoryPolicyRepository()

    @Bean
    @ConditionalOnMissingBean
    fun policyKnowledgeBaseCache(): PolicyKnowledgeBaseCache = PolicyKnowledgeBaseCache()

    @Bean
    @ConditionalOnMissingBean
    fun droolsPolicyEngine(cache: PolicyKnowledgeBaseCache): DroolsPolicyEngine =
        DroolsPolicyEngine(cache)

    @Bean
    @ConditionalOnMissingBean
    fun graphFragmentPolicy(): GraphFragmentPolicy = DefaultGraphFragmentPolicy

    /**
     * Build the engine map explicitly.
     *
     * Do **not** inject `Map<String, PolicyEngine>` — Spring treats that as “all
     * [org.poc.objs.policy.api.PolicyEngine] beans keyed by bean name”
     * (e.g. `droolsPolicyEngine`), not by [PolicyEngineKinds].
     */
    @Bean
    @ConditionalOnMissingBean
    fun policyEvaluator(
        repository: PolicyRepository,
        fragmentPolicy: GraphFragmentPolicy,
        drools: DroolsPolicyEngine,
    ): PolicyEvaluator =
        DefaultPolicyEvaluator(
            repository = repository,
            fragmentPolicy = fragmentPolicy,
            engines = mapOf(PolicyEngineKinds.DROOLS to drools),
        )
}
