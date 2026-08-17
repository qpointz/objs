package org.poc.objs.sbom.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.persistence.BoMPoolEntityReader
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.CreatePortfolioRequest
import org.poc.objs.sbom.domain.CreatePoolAssetRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.PlaceApplicationRequest
import org.poc.objs.sbom.persistence.SbomPersistenceConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(
    SbomPersistenceConfiguration::class,
    BoMGraphStore::class,
    BoMNamedGraphStore::class,
    BoMPoolEntityReader::class,
    SbomService::class,
    ApplicationInventoryService::class,
    ApplicationVersionService::class,
    AssetTypeCatalogService::class,
    AssetInventoryService::class,
    PortfolioService::class,
    org.poc.objs.sbom.uniqueness.PortfolioUniquenessRules::class,
    org.poc.objs.sbom.resolution.LatestVersionResolver::class,
    org.poc.objs.sbom.resolution.PortfolioGraphSelector::class,
    CategoryAssetsService::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-cat-assets;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class CategoryAssetsServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var inventory: ApplicationInventoryService

    @Autowired
    lateinit var assets: AssetInventoryService

    @Autowired
    lateinit var portfolios: PortfolioService

    @Autowired
    lateinit var categoryAssets: CategoryAssetsService

    @Autowired
    lateinit var sbom: SbomService

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @Autowired
    lateinit var edges: BoMAllowedEdgeCatalog

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
    fun shouldListDraftComponentsWithoutACapturedVersion() {
        val app = inventory.create(CreateApplicationRequest(name = "Payments"))
        val component =
            assets.create(
                CreatePoolAssetRequest(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "Jackson",
                            "version" to "2.17.0",
                            "ecosystem" to "Maven",
                            "kind" to "library",
                        ),
                ),
            )
        inventory.addAsset(app.id, DraftAssetWrite(assetId = component.id))
        val portfolio = portfolios.create(CreatePortfolioRequest(name = "Retail"))
        portfolios.placeApplication(portfolio.id, PlaceApplicationRequest(applicationId = app.id))

        val page = categoryAssets.list(portfolio.id, "root", true, 1, 20)
        assertThat(page.items.map { it.label }).anyMatch { it.contains("Jackson") }
        assertThat(page.items.single().usedInApplicationNames).containsExactly("Payments")
    }
}
