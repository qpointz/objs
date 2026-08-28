package org.poc.objs.core.persistence

import org.poc.objs.core.domain.AllowedEdgeCatalog
import org.poc.objs.core.domain.SchemaCatalog
import org.poc.objs.core.seed.SeedProperties
import org.poc.objs.core.seed.SeedStartupLoader
import org.poc.objs.core.validation.Validator
import org.poc.objs.core.versioning.VersioningStrategy
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
 * Creates PostgreSQL-authoritative [JpaSchemaCatalog] / [JpaAllowedEdgeCatalog]
 * implementations with write-through + Caffeine TTL read snapshots. Tests or embedding
 * applications that need pure in-memory catalogs can provide their own [SchemaCatalog] /
 * [AllowedEdgeCatalog] beans.
 */
@AutoConfiguration
@Import(ObjsFlywayAutoConfiguration::class)
@ComponentScan(basePackages = ["org.poc.objs.core"])
@EntityScan(basePackages = ["org.poc.objs.core.persistence"])
@EnableJpaRepositories(basePackages = ["org.poc.objs.core.persistence"])
@EnableConfigurationProperties(SeedProperties::class, ObjsCatalogProperties::class)
class ObjsCoreAutoConfiguration {

    // ── JPA-backed catalogs ──

    @Bean
    @ConditionalOnMissingBean(SchemaCatalog::class)
    fun bomSchemaCatalog(
        repo: SchemaCatalogRepository,
        catalogProperties: ObjsCatalogProperties,
    ): JpaSchemaCatalog = JpaSchemaCatalog(repo, catalogProperties)

    @Bean
    @ConditionalOnMissingBean(AllowedEdgeCatalog::class)
    fun bomAllowedEdgeCatalog(
        repo: AllowedEdgeRuleRepository,
        catalogProperties: ObjsCatalogProperties,
    ): JpaAllowedEdgeCatalog = JpaAllowedEdgeCatalog(repo, catalogProperties)

    // ── Startup hydration (runs before other ApplicationRunners like SBOM registration) ──

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun bomCatalogHydration(
        schemaCatalog: JpaSchemaCatalog,
        edgeCatalog: JpaAllowedEdgeCatalog,
    ): ApplicationRunner = ApplicationRunner {
        schemaCatalog.hydrate()
        edgeCatalog.hydrate()
    }

    /** Ordered classpath/file seed import after catalogs are hydrated. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    fun bomSeedStartup(loader: SeedStartupLoader): ApplicationRunner =
        ApplicationRunner { loader.loadConfiguredResources() }

    // ── Validator ──

    @Bean
    @ConditionalOnMissingBean(VersioningStrategy::class)
    fun bomVersioningStrategy(): VersioningStrategy = ExplicitOnlyVersioningStrategy()

    @Bean
    fun bomValidator(
        schemas: SchemaCatalog,
        allowedEdges: AllowedEdgeCatalog,
    ): Validator = Validator(schemas, allowedEdges)
}
