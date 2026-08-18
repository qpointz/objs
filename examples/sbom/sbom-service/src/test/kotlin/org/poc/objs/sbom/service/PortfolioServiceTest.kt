package org.poc.objs.sbom.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
import org.poc.objs.sbom.domain.CreateSubjectAreaRequest
import org.poc.objs.sbom.domain.PlaceApplicationRequest
import org.poc.objs.sbom.persistence.SbomPersistenceConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.web.server.ResponseStatusException

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
    PortfolioService::class,
    org.poc.objs.sbom.uniqueness.PortfolioUniquenessRules::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-portfolios;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class PortfolioServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var portfolios: PortfolioService

    @Autowired
    lateinit var inventory: ApplicationInventoryService

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
    fun shouldResolveRootAndSubtreeApplicationSets() {
        val payments = inventory.create(CreateApplicationRequest(name = "Payments"))
        val billing = inventory.create(CreateApplicationRequest(name = "Billing"))
        val portal = inventory.create(CreateApplicationRequest(name = "Portal"))

        val portfolio = portfolios.create(CreatePortfolioRequest(name = "Retail"))
        val platform = portfolios.addSubjectArea(portfolio.id, CreateSubjectAreaRequest(name = "Platform"))
        val nested =
            portfolios.addSubjectArea(
                portfolio.id,
                CreateSubjectAreaRequest(name = "Payments team", parentId = platform.id),
            )

        portfolios.placeApplication(
            portfolio.id,
            PlaceApplicationRequest(applicationId = payments.id, subjectAreaId = nested.id),
        )
        portfolios.placeApplication(
            portfolio.id,
            PlaceApplicationRequest(applicationId = billing.id, subjectAreaId = platform.id),
        )
        portfolios.placeApplication(
            portfolio.id,
            PlaceApplicationRequest(applicationId = portal.id, subjectAreaId = null),
        )

        val root = portfolios.applicationsForLevel(portfolio.id, "root")
        assertThat(root.applications.map { it.applicationName })
            .containsExactlyInAnyOrder("Billing", "Payments", "Portal")

        val platformSet = portfolios.applicationsForLevel(portfolio.id, platform.id.toString())
        assertThat(platformSet.applications.map { it.applicationName })
            .containsExactlyInAnyOrder("Billing", "Payments")

        val rootDirect = portfolios.applicationsForLevel(portfolio.id, "root", includeSubcategories = false)
        assertThat(rootDirect.applications.map { it.applicationName }).containsExactly("Portal")
    }

    @Test
    fun shouldRejectSameAppInSecondCategoryWhenUniqueApp() {
        val app = inventory.create(CreateApplicationRequest(name = "OnlyOnce"))
        val portfolio = portfolios.create(CreatePortfolioRequest(name = "Unique"))
        val area = portfolios.addSubjectArea(portfolio.id, CreateSubjectAreaRequest(name = "A"))
        portfolios.placeApplication(portfolio.id, PlaceApplicationRequest(applicationId = app.id))
        assertThatThrownBy {
            portfolios.placeApplication(
                portfolio.id,
                PlaceApplicationRequest(applicationId = app.id, subjectAreaId = area.id),
            )
        }.isInstanceOf(ResponseStatusException::class.java)
    }

    @Test
    fun shouldAllowSameAppInDifferentCategoriesWhenNotUnique() {
        val app = inventory.create(CreateApplicationRequest(name = "Shared"))
        val portfolio =
            portfolios.create(CreatePortfolioRequest(name = "Open", uniqueness = "NOT_UNIQUE"))
        val a = portfolios.addSubjectArea(portfolio.id, CreateSubjectAreaRequest(name = "A"))
        val b = portfolios.addSubjectArea(portfolio.id, CreateSubjectAreaRequest(name = "B"))
        portfolios.placeApplication(portfolio.id, PlaceApplicationRequest(applicationId = app.id, subjectAreaId = a.id))
        portfolios.placeApplication(portfolio.id, PlaceApplicationRequest(applicationId = app.id, subjectAreaId = b.id))
        val root = portfolios.applicationsForLevel(portfolio.id, "root")
        assertThat(root.applications).hasSize(2)
    }

    @Test
    fun shouldMovePlacementToAnotherCategory() {
        val app = inventory.create(CreateApplicationRequest(name = "Payments", description = "Checkout"))
        val portfolio = portfolios.create(CreatePortfolioRequest(name = "Retail"))
        val a = portfolios.addSubjectArea(portfolio.id, CreateSubjectAreaRequest(name = "A"))
        val b = portfolios.addSubjectArea(portfolio.id, CreateSubjectAreaRequest(name = "B"))
        portfolios.placeApplication(portfolio.id, PlaceApplicationRequest(applicationId = app.id, subjectAreaId = a.id))
        val placed = portfolios.applicationsForLevel(portfolio.id, a.id.toString(), includeSubcategories = false)
        val placementId = placed.applications.single().placementId!!
        assertThat(placed.applications.single().applicationDescription).isEqualTo("Checkout")
        portfolios.movePlacements(portfolio.id, listOf(placementId), b.id)
        assertThat(
            portfolios.applicationsForLevel(portfolio.id, b.id.toString(), includeSubcategories = false)
                .applications.map { it.applicationName },
        ).containsExactly("Payments")
        assertThat(
            portfolios.applicationsForLevel(portfolio.id, a.id.toString(), includeSubcategories = false)
                .applications,
        ).isEmpty()
    }
}
