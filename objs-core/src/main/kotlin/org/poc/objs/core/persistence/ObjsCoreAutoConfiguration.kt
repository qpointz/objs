package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.validation.BoMValidator
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Autoconfiguration for objs-core persistence and validation beans.
 *
 * Creates PostgreSQL-authoritative [JpaBoMSchemaCatalog] / [JpaBoMAllowedEdgeCatalog]
 * implementations with in-memory read caches. Tests or embedding applications that need
 * pure in-memory catalogs can provide their own [BoMSchemaCatalog] / [BoMAllowedEdgeCatalog]
 * beans.
 */
@AutoConfiguration
@ComponentScan(basePackages = ["org.poc.objs.core"])
@EntityScan(basePackages = ["org.poc.objs.core.persistence"])
@EnableJpaRepositories(basePackages = ["org.poc.objs.core.persistence"])
class ObjsCoreAutoConfiguration {

    // ── JPA-backed catalogs ──

    @Bean
    @ConditionalOnMissingBean(BoMSchemaCatalog::class)
    fun bomSchemaCatalog(repo: BoMSchemaCatalogRepository): JpaBoMSchemaCatalog =
        JpaBoMSchemaCatalog(repo)

    @Bean
    @ConditionalOnMissingBean(BoMAllowedEdgeCatalog::class)
    fun bomAllowedEdgeCatalog(repo: BoMAllowedEdgeRuleRepository): JpaBoMAllowedEdgeCatalog =
        JpaBoMAllowedEdgeCatalog(repo)

    // ── Startup hydration (runs before other ApplicationRunners like SBOM registration) ──

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun bomCatalogHydration(
        schemaCatalog: JpaBoMSchemaCatalog,
        edgeCatalog: JpaBoMAllowedEdgeCatalog,
    ): ApplicationRunner = ApplicationRunner {
        schemaCatalog.hydrate()
        edgeCatalog.hydrate()
    }

    // ── Validator ──

    @Bean
    fun bomValidator(
        schemas: BoMSchemaCatalog,
        allowedEdges: BoMAllowedEdgeCatalog,
    ): BoMValidator = BoMValidator(schemas, allowedEdges)
}
