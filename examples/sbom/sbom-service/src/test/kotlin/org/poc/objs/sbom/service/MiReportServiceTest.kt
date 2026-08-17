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
import org.poc.objs.sbom.domain.CreateSubjectAreaRequest
import org.poc.objs.sbom.domain.PromoteVersionRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.PlaceApplicationRequest
import org.poc.objs.sbom.domain.RunMiReportRequest
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
    MiReportService::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-mi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class MiReportServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var inventory: ApplicationInventoryService

    @Autowired
    lateinit var versions: ApplicationVersionService

    @Autowired
    lateinit var assets: AssetInventoryService

    @Autowired
    lateinit var portfolios: PortfolioService

    @Autowired
    lateinit var reports: MiReportService

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
    fun shouldRunMiReportsOverLatestVersionGraphs() {
        val payments = inventory.create(CreateApplicationRequest(name = "Payments"))
        val billing = inventory.create(CreateApplicationRequest(name = "Billing"))
        val noVersion = inventory.create(CreateApplicationRequest(name = "NoVersion"))

        val jacksonPayload =
            mapOf(
                "name" to "Jackson",
                "version" to "2.17.0",
                "ecosystem" to "Maven",
                "kind" to "library",
            )
        val shared =
            assets.create(
                org.poc.objs.sbom.domain.CreatePoolAssetRequest(
                    type = "Component",
                    payload = jacksonPayload,
                ),
            )
        // Same identity, distinct pool id — surfaces in MI-4; also keeps Component count at 2.
        val jacksonDup =
            assets.create(
                org.poc.objs.sbom.domain.CreatePoolAssetRequest(
                    type = "Component",
                    payload = jacksonPayload,
                ),
            )
        inventory.addAsset(payments.id, DraftAssetWrite(assetId = shared.id))
        inventory.addAsset(payments.id, DraftAssetWrite(assetId = jacksonDup.id))
        inventory.addAsset(billing.id, DraftAssetWrite(assetId = shared.id))
        versions.promote(payments.id, versions.draft(payments.id)!!.id, PromoteVersionRequest("1.0"))
        versions.promote(billing.id, versions.draft(billing.id)!!.id, PromoteVersionRequest("1.0"))

        val portfolio = portfolios.create(CreatePortfolioRequest(name = "Retail"))
        val platform = portfolios.addSubjectArea(portfolio.id, CreateSubjectAreaRequest(name = "Platform"))
        portfolios.placeApplication(
            portfolio.id,
            PlaceApplicationRequest(applicationId = payments.id, subjectAreaId = platform.id),
        )
        portfolios.placeApplication(
            portfolio.id,
            PlaceApplicationRequest(applicationId = billing.id, subjectAreaId = platform.id),
        )
        portfolios.placeApplication(
            portfolio.id,
            PlaceApplicationRequest(applicationId = noVersion.id, subjectAreaId = null),
        )

        val mi1 =
            reports.run(portfolio.id, RunMiReportRequest(level = "root", report = "MI-1"))
        assertThat(mi1.graphCount).isEqualTo(3)
        assertThat(mi1.composition!!.assetCountsByType["Component"]).isEqualTo(2L)

        val mi2 =
            reports.run(portfolio.id, RunMiReportRequest(level = platform.id.toString(), report = "MI-2"))
        assertThat(mi2.dependencyMap).isNotEmpty
        assertThat(mi2.dependencyMap!!.any { it.sharedAssetCount >= 1 }).isTrue()
        assertThat(mi2.applicationsInScope.map { it.applicationName })
            .containsExactlyInAnyOrder("Billing", "Payments")

        val nested =
            portfolios.addSubjectArea(
                portfolio.id,
                CreateSubjectAreaRequest(name = "Team", parentId = platform.id),
            )
        val nestedOnly = inventory.create(CreateApplicationRequest(name = "Nested"))
        inventory.addAsset(nestedOnly.id, DraftAssetWrite(assetId = shared.id))
        portfolios.placeApplication(
            portfolio.id,
            PlaceApplicationRequest(applicationId = nestedOnly.id, subjectAreaId = nested.id),
        )
        val nestedReport =
            reports.run(portfolio.id, RunMiReportRequest(level = nested.id.toString(), report = "MI-1"))
        assertThat(nestedReport.applicationsInScope.map { it.applicationName }).containsExactly("Nested")
        assertThat(nestedReport.composition!!.assetCountsByType["Component"]).isEqualTo(1L)

        val platformInclusive =
            reports.run(
                portfolio.id,
                RunMiReportRequest(level = platform.id.toString(), includeSubcategories = true, report = "MI-1"),
            )
        assertThat(platformInclusive.applicationsInScope.map { it.applicationName })
            .contains("Nested", "Billing", "Payments")

        val mi3 =
            reports.run(portfolio.id, RunMiReportRequest(level = "root", report = "MI-3"))
        assertThat(mi3.sharedAssets!!.map { it.assetId }).contains(shared.id)

        val mi4 =
            reports.run(portfolio.id, RunMiReportRequest(level = "root", report = "MI-4"))
        assertThat(mi4.duplicateSignals).isNotEmpty
        assertThat(mi4.riskSignals!!.any { it.kind == "duplicate-groups" }).isTrue()
    }
}
