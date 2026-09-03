package org.poc.objs.autoconfigure

import jakarta.persistence.EntityManager
import org.poc.objs.api.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.CatalogSupport
import org.poc.objs.api.domain.FullCatalogJsonSchemaExporter
import org.poc.objs.api.domain.SchemaCatalog
import org.poc.objs.api.seed.SeedDocumentHandler
import org.poc.objs.api.seed.SeedResourceResolver
import org.poc.objs.api.versioning.ExplicitOnlyVersioningStrategy
import org.poc.objs.api.versioning.VersioningStrategy
import org.poc.objs.core.persistence.AllowedEdgeRuleDao
import org.poc.objs.core.persistence.DeepGraphVersionService
import org.poc.objs.core.persistence.EdgeDao
import org.poc.objs.core.persistence.EdgeVersionDao
import org.poc.objs.core.persistence.EntityDao
import org.poc.objs.core.persistence.EntityVersionDao
import org.poc.objs.core.persistence.GraphDao
import org.poc.objs.core.persistence.GraphMembershipDao
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.GraphVersionDao
import org.poc.objs.core.persistence.GraphVersionEdgeDao
import org.poc.objs.core.persistence.GraphVersionMemberDao
import org.poc.objs.core.persistence.JpaAllowedEdgeCatalog
import org.poc.objs.core.persistence.JpaSchemaCatalog
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.core.persistence.ObjsCatalogProperties
import org.poc.objs.core.persistence.PoolEntityReader
import org.poc.objs.core.persistence.SchemaCatalogDao
import org.poc.objs.core.persistence.SeedLedgerDao
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.core.seed.AllowedEdgeRuleSeedHandler
import org.poc.objs.core.seed.CanonicalSeedSerializer
import org.poc.objs.core.seed.GraphSeedHandler
import org.poc.objs.core.seed.ObjectSchemaSeedHandler
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.core.seed.SeedLedger
import org.poc.objs.core.seed.SeedProperties
import org.poc.objs.core.seed.SeedStartupLoader
import org.poc.objs.core.validation.Validator
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.ResourceLoader
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * Autoconfiguration for objs-core persistence and validation beans.
 *
 * Creates PostgreSQL-authoritative [JpaSchemaCatalog] / [JpaAllowedEdgeCatalog]
 * implementations with write-through + Caffeine TTL read snapshots. Tests or embedding
 * applications that need pure in-memory catalogs can provide their own [SchemaCatalog] /
 * [AllowedEdgeCatalog] beans.
 */
@AutoConfiguration
@AutoConfigurationPackage(basePackages = ["org.poc.objs.core.persistence"])
@Import(ObjsFlywayAutoConfiguration::class)
@EntityScan(basePackages = ["org.poc.objs.core.persistence"])
@EnableTransactionManagement
class ObjsCoreAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "objs.catalogs")
    @ConditionalOnMissingBean(ObjsCatalogProperties::class)
    fun objsCatalogProperties(): ObjsCatalogProperties = ObjsCatalogProperties()

    @Bean
    @ConfigurationProperties(prefix = "objs.seeds")
    @ConditionalOnMissingBean(SeedProperties::class)
    fun objsSeedProperties(): SeedProperties = SeedProperties()

    @Bean
    @ConditionalOnMissingBean(UnitOfWork::class)
    fun objsUnitOfWork(
        em: EntityManager,
        transactionManager: PlatformTransactionManager,
    ): UnitOfWork = TransactionTemplateUnitOfWork.create(em, transactionManager)

    @Bean
    @ConditionalOnMissingBean
    fun entityDao(uow: UnitOfWork) = EntityDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun edgeDao(uow: UnitOfWork) = EdgeDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun graphDao(uow: UnitOfWork) = GraphDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun graphMembershipDao(uow: UnitOfWork) = GraphMembershipDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun entityVersionDao(uow: UnitOfWork) = EntityVersionDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun graphVersionDao(uow: UnitOfWork) = GraphVersionDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun edgeVersionDao(uow: UnitOfWork) = EdgeVersionDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun graphVersionMemberDao(uow: UnitOfWork) = GraphVersionMemberDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun graphVersionEdgeDao(uow: UnitOfWork) = GraphVersionEdgeDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun schemaCatalogDao(uow: UnitOfWork) = SchemaCatalogDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun allowedEdgeRuleDao(uow: UnitOfWork) = AllowedEdgeRuleDao(uow)

    @Bean
    @ConditionalOnMissingBean
    fun seedLedgerDao(uow: UnitOfWork) = SeedLedgerDao(uow)

    @Bean
    @ConditionalOnMissingBean(SchemaCatalog::class)
    fun bomSchemaCatalog(
        dao: SchemaCatalogDao,
        uow: UnitOfWork,
        catalogProperties: ObjsCatalogProperties,
    ): JpaSchemaCatalog = JpaSchemaCatalog(dao, uow, catalogProperties)

    @Bean
    @ConditionalOnMissingBean(AllowedEdgeCatalog::class)
    fun bomAllowedEdgeCatalog(
        dao: AllowedEdgeRuleDao,
        uow: UnitOfWork,
        catalogProperties: ObjsCatalogProperties,
    ): JpaAllowedEdgeCatalog = JpaAllowedEdgeCatalog(dao, uow, catalogProperties)

    @Bean
    @ConditionalOnMissingBean(CatalogSupport::class)
    fun bomCatalogSupport(
        schemas: SchemaCatalog,
        edges: AllowedEdgeCatalog,
    ): CatalogSupport = CatalogSupport(schemas, edges)

    @Bean
    @ConditionalOnMissingBean(FullCatalogJsonSchemaExporter::class)
    fun bomFullCatalogJsonSchemaExporter(
        schemas: SchemaCatalog,
        edges: AllowedEdgeCatalog,
    ): FullCatalogJsonSchemaExporter = FullCatalogJsonSchemaExporter(schemas, edges)

    @Bean
    @ConditionalOnMissingBean(VersioningStrategy::class)
    fun bomVersioningStrategy(): VersioningStrategy = ExplicitOnlyVersioningStrategy()

    @Bean
    @ConditionalOnMissingBean
    fun bomValidator(
        schemas: SchemaCatalog,
        allowedEdges: AllowedEdgeCatalog,
    ): Validator = Validator(schemas, allowedEdges)

    @Bean
    @ConditionalOnMissingBean
    fun deepGraphVersionService(
        graphDao: GraphDao,
        membershipDao: GraphMembershipDao,
        entityDao: EntityDao,
        edgeDao: EdgeDao,
        entityVersionDao: EntityVersionDao,
        graphVersionDao: GraphVersionDao,
        edgeVersionDao: EdgeVersionDao,
        graphVersionMemberDao: GraphVersionMemberDao,
        graphVersionEdgeDao: GraphVersionEdgeDao,
        uow: UnitOfWork,
    ) = DeepGraphVersionService(
        graphDao = graphDao,
        membershipDao = membershipDao,
        entityDao = entityDao,
        edgeDao = edgeDao,
        entityVersions = entityVersionDao,
        graphVersions = graphVersionDao,
        edgeVersions = edgeVersionDao,
        versionMembers = graphVersionMemberDao,
        versionEdges = graphVersionEdgeDao,
        uow = uow,
    )

    @Bean
    @ConditionalOnMissingBean
    fun poolEntityReader(uow: UnitOfWork) = PoolEntityReader(uow)

    @Bean
    @ConditionalOnMissingBean
    fun namedGraphStore(
        graphDao: GraphDao,
        membershipDao: GraphMembershipDao,
        entityDao: EntityDao,
        edgeDao: EdgeDao,
        validator: Validator,
        deepVersions: DeepGraphVersionService,
        versionMemberDao: GraphVersionMemberDao,
        uow: UnitOfWork,
    ) = NamedGraphStore(
        graphDao = graphDao,
        membershipDao = membershipDao,
        entityDao = entityDao,
        edgeDao = edgeDao,
        validator = validator,
        deepVersions = deepVersions,
        versionMemberDao = versionMemberDao,
        uow = uow,
    )

    @Bean
    @ConditionalOnMissingBean
    fun graphStore(
        entityDao: EntityDao,
        edgeDao: EdgeDao,
        validator: Validator,
        namedGraphs: NamedGraphStore,
        poolReader: PoolEntityReader,
        schemas: SchemaCatalog,
        catalog: CatalogSupport,
        versioning: VersioningStrategy,
        uow: UnitOfWork,
    ): GraphStore {
        val store = GraphStore(
            entityDao = entityDao,
            edgeDao = edgeDao,
            validator = validator,
            namedGraphs = namedGraphs,
            poolReader = poolReader,
            schemas = schemas,
            catalog = catalog,
            versioning = versioning,
            uow = uow,
        )
        namedGraphs.attachGraphStore(store)
        return store
    }

    @Bean
    @ConditionalOnMissingBean
    fun objectSchemaSeedHandler(schemas: SchemaCatalog) = ObjectSchemaSeedHandler(schemas)

    @Bean
    @ConditionalOnMissingBean
    fun allowedEdgeRuleSeedHandler(edges: AllowedEdgeCatalog) = AllowedEdgeRuleSeedHandler(edges)

    @Bean
    @ConditionalOnMissingBean
    fun graphSeedHandler(namedGraphs: NamedGraphStore) = GraphSeedHandler(namedGraphs)

    @Bean
    @ConditionalOnMissingBean
    fun seedImporter(handlers: List<SeedDocumentHandler>, uow: UnitOfWork) =
        SeedImporter(handlers, uow)

    @Bean
    @ConditionalOnMissingBean
    fun seedLedger(dao: SeedLedgerDao, uow: UnitOfWork) = SeedLedger(dao, uow)

    @Bean
    @ConditionalOnMissingBean
    fun canonicalSeedSerializer(
        schemas: SchemaCatalog,
        edgeRules: AllowedEdgeCatalog,
        objectSchemaHandler: ObjectSchemaSeedHandler,
        allowedEdgeRuleHandler: AllowedEdgeRuleSeedHandler,
        graphHandler: GraphSeedHandler,
    ) = CanonicalSeedSerializer(
        schemas = schemas,
        edgeRules = edgeRules,
        objectSchemaHandler = objectSchemaHandler,
        allowedEdgeRuleHandler = allowedEdgeRuleHandler,
        graphHandler = graphHandler,
    )

    @Bean
    @ConditionalOnMissingBean(SeedResourceResolver::class)
    fun seedResourceResolver(resourceLoader: ResourceLoader): SeedResourceResolver =
        SeedResourceResolver { location ->
            val resource = resourceLoader.getResource(location)
            if (!resource.exists()) null else resource.inputStream
        }

    @Bean
    @ConditionalOnMissingBean
    fun seedStartupLoader(
        properties: SeedProperties,
        resourceResolver: SeedResourceResolver,
        importer: SeedImporter,
        ledger: SeedLedger,
    ) = SeedStartupLoader(properties, resourceResolver, importer, ledger)

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun bomCatalogHydration(
        schemaCatalog: JpaSchemaCatalog,
        edgeCatalog: JpaAllowedEdgeCatalog,
    ): ApplicationRunner = ApplicationRunner {
        schemaCatalog.hydrate()
        edgeCatalog.hydrate()
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    fun bomSeedStartup(loader: SeedStartupLoader): ApplicationRunner =
        ApplicationRunner { loader.loadConfiguredResources() }
}
