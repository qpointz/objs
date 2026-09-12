package org.poc.objs.policy.service

import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.PolicyBatchEvaluator
import org.poc.objs.policy.api.PolicyBatchExecutor
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyEvaluator
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.SuiteEvaluator
import org.poc.objs.policy.api.SuiteRepository
import org.poc.objs.policy.core.DefaultPolicyBatchEvaluator
import org.poc.objs.policy.core.DefaultPolicyEvaluator
import org.poc.objs.policy.core.DefaultSuiteEvaluator
import org.poc.objs.policy.core.InMemoryPolicyStores
import org.poc.objs.policy.core.SequentialPolicyBatchExecutor
import org.poc.objs.policy.drools.DroolsPolicyEngine
import org.poc.objs.policy.drools.PolicyKnowledgeBaseCache
import org.poc.objs.policy.service.seed.CategorySeedHandler
import org.poc.objs.policy.service.seed.DropCategorySeedHandler
import org.poc.objs.policy.service.seed.DropPolicySeedHandler
import org.poc.objs.policy.service.seed.DropPolicySuiteSeedHandler
import org.poc.objs.policy.service.seed.PolicyCatalogSeedExporter
import org.poc.objs.policy.service.seed.PolicySeedHandler
import org.poc.objs.policy.service.seed.PolicySuiteSeedHandler
import org.poc.objs.policy.service.seed.SeedBodyLoader
import org.poc.objs.policy.service.seed.SpringSeedBodyLoader
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@AutoConfiguration
@ComponentScan(basePackages = ["org.poc.objs.policy.service"])
class ObjsPolicyServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun inMemoryPolicyStores(): InMemoryPolicyStores = InMemoryPolicyStores()

    @Bean
    @ConditionalOnMissingBean
    fun categoryRepository(stores: InMemoryPolicyStores): CategoryRepository = stores.categories

    @Bean
    @ConditionalOnMissingBean
    fun policyRepository(stores: InMemoryPolicyStores): PolicyRepository = stores.policies

    @Bean
    @ConditionalOnMissingBean
    fun suiteRepository(stores: InMemoryPolicyStores): SuiteRepository = stores.suites

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

    @Bean
    @ConditionalOnMissingBean
    fun suiteEvaluator(
        policyEvaluator: PolicyEvaluator,
        repository: PolicyRepository,
        categories: CategoryRepository,
    ): SuiteEvaluator =
        DefaultSuiteEvaluator(
            policyEvaluator = policyEvaluator,
            policies = repository,
            categories = categories,
        )

    @Bean
    @ConditionalOnMissingBean
    fun policyBatchExecutor(
        policyEvaluator: PolicyEvaluator,
        suiteEvaluator: SuiteEvaluator,
    ): PolicyBatchExecutor =
        SequentialPolicyBatchExecutor(
            policyEvaluator = policyEvaluator,
            suiteEvaluator = suiteEvaluator,
        )

    @Bean
    @ConditionalOnMissingBean
    fun policyBatchEvaluator(
        executor: PolicyBatchExecutor,
    ): PolicyBatchEvaluator = DefaultPolicyBatchEvaluator(executor)

    /**
     * Policy catalog [org.poc.objs.api.seed.SeedDocumentHandler] beans (C-28 WI-003,
     * G-P38seed) — no dedicated importer; Spring collects these into the shared
     * `seedImporter(handlers: List<SeedDocumentHandler>, ...)` bean (`:objs-autoconfigure`)
     * alongside the built-in graph/object seed kinds.
     */
    @Bean
    @ConditionalOnMissingBean
    fun seedBodyLoader(): SeedBodyLoader = SpringSeedBodyLoader()

    @Bean
    @ConditionalOnMissingBean
    fun categorySeedHandler(
        categories: CategoryRepository,
        policies: PolicyRepository,
    ): CategorySeedHandler = CategorySeedHandler(categories, policies)

    @Bean
    @ConditionalOnMissingBean
    fun policySeedHandler(
        policies: PolicyRepository,
        categories: CategoryRepository,
        bodyLoader: SeedBodyLoader,
    ): PolicySeedHandler = PolicySeedHandler(policies, categories, bodyLoader)

    @Bean
    @ConditionalOnMissingBean
    fun policySuiteSeedHandler(
        suites: SuiteRepository,
        policies: PolicyRepository,
    ): PolicySuiteSeedHandler = PolicySuiteSeedHandler(suites, policies)

    @Bean
    @ConditionalOnMissingBean
    fun dropCategorySeedHandler(
        categories: CategoryRepository,
        policies: PolicyRepository,
    ): DropCategorySeedHandler = DropCategorySeedHandler(categories, policies)

    @Bean
    @ConditionalOnMissingBean
    fun dropPolicySeedHandler(policies: PolicyRepository): DropPolicySeedHandler =
        DropPolicySeedHandler(policies)

    @Bean
    @ConditionalOnMissingBean
    fun dropPolicySuiteSeedHandler(suites: SuiteRepository): DropPolicySuiteSeedHandler =
        DropPolicySuiteSeedHandler(suites)

    @Bean
    @ConditionalOnMissingBean
    fun policyCatalogSeedExporter(
        categories: CategoryRepository,
        policies: PolicyRepository,
        suites: SuiteRepository,
    ): PolicyCatalogSeedExporter = PolicyCatalogSeedExporter(categories, policies, suites)
}
