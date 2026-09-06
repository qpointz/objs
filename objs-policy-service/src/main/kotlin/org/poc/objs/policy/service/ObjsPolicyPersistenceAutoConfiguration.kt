package org.poc.objs.policy.service

import org.poc.objs.core.persistence.policy.JpaCategoryRepository
import org.poc.objs.core.persistence.policy.JpaPolicyRepository
import org.poc.objs.core.persistence.policy.JpaSuiteRepository
import org.poc.objs.core.persistence.policy.PolicyCategoryDao
import org.poc.objs.core.persistence.policy.PolicyDao
import org.poc.objs.core.persistence.policy.PolicySuiteDao
import org.poc.objs.core.persistence.policy.PolicySuiteFolderDao
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.SuiteRepository
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/**
 * Wires JPA-backed [CategoryRepository] / [PolicyRepository] / [SuiteRepository] (C-28 WI-002)
 * when a persistence [UnitOfWork] bean is present (i.e. `:objs-persistence` is wired up via
 * `:objs-autoconfigure`). Runs before [ObjsPolicyServiceAutoConfiguration] so its in-memory
 * `@ConditionalOnMissingBean` defaults back off in favour of these JPA repositories.
 */
@AutoConfiguration
@AutoConfigureBefore(ObjsPolicyServiceAutoConfiguration::class)
@ConditionalOnClass(UnitOfWork::class)
@ConditionalOnBean(UnitOfWork::class)
class ObjsPolicyPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun policyCategoryDao(uow: UnitOfWork): PolicyCategoryDao = PolicyCategoryDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun policyDao(uow: UnitOfWork): PolicyDao = PolicyDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun policySuiteDao(uow: UnitOfWork): PolicySuiteDao = PolicySuiteDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun policySuiteFolderDao(uow: UnitOfWork): PolicySuiteFolderDao = PolicySuiteFolderDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun categoryRepository(
        categoryDao: PolicyCategoryDao,
        policyDao: PolicyDao,
        uow: UnitOfWork,
    ): CategoryRepository = JpaCategoryRepository(categoryDao, policyDao, uow)

    @Bean
    @ConditionalOnMissingBean
    fun policyRepository(
        policyDao: PolicyDao,
        categoryDao: PolicyCategoryDao,
        uow: UnitOfWork,
    ): PolicyRepository = JpaPolicyRepository(policyDao, categoryDao, uow)

    @Bean
    @ConditionalOnMissingBean
    fun suiteRepository(
        suiteDao: PolicySuiteDao,
        folderDao: PolicySuiteFolderDao,
        uow: UnitOfWork,
    ): SuiteRepository = JpaSuiteRepository(suiteDao, folderDao, uow)
}
