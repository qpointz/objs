package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.seed.BoMSeedProperties
import org.poc.objs.core.seed.BoMSeedStartupLoader
import org.poc.objs.core.validation.BoMValidator
import org.poc.objs.core.versioning.BomVersioningStrategy
import org.poc.objs.core.versioning.ExplicitOnlyVersioningStrategy
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Autoconfiguration for objs-core persistence and validation beans.
 *
 * Creates PostgreSQL-authoritative [JpaBoMSchemaCatalog] / [JpaBoMAllowedEdgeCatalog]
 * implementations with write-through + Caffeine TTL read snapshots. Tests or embedding
 * applications that need pure in-memory catalogs can provide their own [BoMSchemaCatalog] /
 * [BoMAllowedEdgeCatalog] beans.
 */
@AutoConfiguration
@Import(ObjsFlywayAutoConfiguration::class)
@ComponentScan(basePackages = ["org.poc.objs.core"])
@EntityScan(basePackages = ["org.poc.objs.core.persistence"])
@EnableJpaRepositories(basePackages = ["org.poc.objs.core.persistence"])
@EnableConfigurationProperties(BoMSeedProperties::class, ObjsCatalogProperties::class)
class ObjsCoreAutoConfiguration {

    // ── JPA-backed catalogs ──

    @Bean
    @ConditionalOnMissingBean(BoMSchemaCatalog::class)
    fun bomSchemaCatalog(
        repo: BoMSchemaCatalogRepository,
        catalogProperties: ObjsCatalogProperties,
    ): JpaBoMSchemaCatalog = JpaBoMSchemaCatalog(repo, catalogProperties)

    @Bean
    @ConditionalOnMissingBean(BoMAllowedEdgeCatalog::class)
    fun bomAllowedEdgeCatalog(
        repo: BoMAllowedEdgeRuleRepository,
        catalogProperties: ObjsCatalogProperties,
    ): JpaBoMAllowedEdgeCatalog = JpaBoMAllowedEdgeCatalog(repo, catalogProperties)

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

    /** Ordered classpath/file seed import after catalogs are hydrated. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    fun bomSeedStartup(loader: BoMSeedStartupLoader): ApplicationRunner =
        ApplicationRunner { loader.loadConfiguredResources() }

    // ── Validator ──

    @Bean
    @ConditionalOnMissingBean(BomVersioningStrategy::class)
    fun bomVersioningStrategy(): BomVersioningStrategy = ExplicitOnlyVersioningStrategy()

    @Bean
    fun bomValidator(
        schemas: BoMSchemaCatalog,
        allowedEdges: BoMAllowedEdgeCatalog,
    ): BoMValidator = BoMValidator(schemas, allowedEdges)
}
