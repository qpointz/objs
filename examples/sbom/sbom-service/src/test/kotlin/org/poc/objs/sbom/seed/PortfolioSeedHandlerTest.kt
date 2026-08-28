package org.poc.objs.sbom.seed

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.AllowedEdgeCatalog
import org.poc.objs.core.domain.SchemaCatalog
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.core.persistence.PoolEntityReader
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.poc.objs.core.seed.SeedDocumentParseException
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.core.seed.SeedRawDocument
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.persistence.SbomPersistenceConfiguration
import org.poc.objs.sbom.service.ApplicationInventoryService
import org.poc.objs.sbom.service.ApplicationVersionService
import org.poc.objs.sbom.service.AssetTypeCatalogService
import org.poc.objs.sbom.service.PortfolioService
import org.poc.objs.sbom.service.SbomService
import org.poc.objs.sbom.uniqueness.PortfolioUniquenessRules
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(
    SbomPersistenceConfiguration::class,
    GraphStore::class,
    NamedGraphStore::class,
    PoolEntityReader::class,
    SbomService::class,
    ApplicationInventoryService::class,
    ApplicationVersionService::class,
    AssetTypeCatalogService::class,
    PortfolioService::class,
    PortfolioUniquenessRules::class,
    PortfolioSeedHandler::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-portfolio-seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class PortfolioSeedHandlerTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var handler: PortfolioSeedHandler

    @Autowired
    lateinit var importer: SeedImporter

    @Autowired
    lateinit var portfolios: PortfolioService

    @Autowired
    lateinit var inventory: ApplicationInventoryService

    @Autowired
    lateinit var sbom: SbomService

    @Autowired
    lateinit var schemas: SchemaCatalog

    @Autowired
    lateinit var edges: AllowedEdgeCatalog

    @BeforeEach
    fun reset() {
        schemas.clear()
        edges.clear()
        val field = SbomService::class.java.getDeclaredField("packRegistered")
        field.isAccessible = true
        field.setBoolean(sbom, false)
        sbom.ensureRegistry()
    }

    @Test
    fun shouldRejectMissingPortfolioId() {
        assertThatThrownBy {
            handler.parse(
                SeedRawDocument(
                    index = 0,
                    apiVersion = "objs.poc.org/v1",
                    kind = "Portfolio",
                    raw = mapOf("kind" to "Portfolio", "name" to "X"),
                ),
            )
        }.isInstanceOf(SeedDocumentParseException::class.java)
    }

    @Test
    fun shouldUpsertByIdNotByName() {
        val portfolioId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val catId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val yaml = """
            apiVersion: objs.poc.org/v1
            kind: Portfolio
            id: $portfolioId
            name: Retail
            categories:
              - id: $catId
                name: Platform
                description: Shared
        """.trimIndent()
        importer.importYaml(yaml)
        importer.importYaml(yaml.replace("name: Retail", "name: Retail renamed"))
        val tree = portfolios.getTree(portfolioId)
        assertThat(tree.portfolio.name).isEqualTo("Retail renamed")
        assertThat(tree.subjectAreas).hasSize(1)
        assertThat(tree.subjectAreas[0].id).isEqualTo(catId)
        assertThat(tree.subjectAreas[0].description).isEqualTo("Shared")
    }

    @Test
    fun shouldPlaceByApplicationUuid() {
        val portfolioId = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111")
        val catId = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222")
        val app = inventory.create(CreateApplicationRequest(name = "Payments"))
        val yaml = """
            apiVersion: objs.poc.org/v1
            kind: Portfolio
            id: $portfolioId
            name: WithApp
            categories:
              - id: $catId
                name: Platform
            placements:
              - applicationId: ${app.id}
                categoryId: $catId
        """.trimIndent()
        importer.importYaml(yaml)
        val tree = portfolios.getTree(portfolioId)
        assertThat(tree.subjectAreas[0].applications.map { it.applicationId }).containsExactly(app.id)
    }
}
