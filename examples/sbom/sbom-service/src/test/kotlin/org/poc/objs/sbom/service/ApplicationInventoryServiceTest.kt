package org.poc.objs.sbom.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.AllowedEdgeCatalog
import org.poc.objs.core.domain.SchemaCatalog
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.core.persistence.PoolEntityReader
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.DraftRelationWrite
import org.poc.objs.sbom.persistence.SbomPersistenceConfiguration
import org.poc.objs.sbom.registry.SbomRoles
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
    GraphStore::class,
    NamedGraphStore::class,
    PoolEntityReader::class,
    SbomService::class,
    ApplicationInventoryService::class,
    ApplicationVersionService::class,
    AssetTypeCatalogService::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-apps;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class ApplicationInventoryServiceTest {

    @SpringBootConfiguration
    class TestApp

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
    fun shouldCreateApplicationWithEmptyDraft() {
        val app = inventory.create(CreateApplicationRequest(name = "Payments", description = "core"))
        assertThat(app.name).isEqualTo("Payments")
        val draft = inventory.getDraft(app.id)
        assertThat(draft.applicationName).isEqualTo("Payments")
        assertThat(draft.assets).isEmpty()
        assertThat(draft.relations).isEmpty()
    }

    @Test
    fun shouldSearchByName() {
        inventory.create(CreateApplicationRequest(name = "Alpha"))
        inventory.create(CreateApplicationRequest(name = "Beta Portal"))
        assertThat(inventory.search("portal").map { it.name }).containsExactly("Beta Portal")
    }

    @Test
    fun shouldCreateAssetOnDraftAndInferSharedDeps() {
        val a = inventory.create(CreateApplicationRequest(name = "AppA"))
        val b = inventory.create(CreateApplicationRequest(name = "AppB"))

        val draftA =
            inventory.addAsset(
                a.id,
                DraftAssetWrite(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "jackson-core",
                            "version" to "2.17.0",
                            "ecosystem" to "maven",
                            "kind" to "library",
                        ),
                    setOwner = true,
                ),
            )
        assertThat(draftA.assets).hasSize(1)
        assertThat(draftA.assets[0].owner).isEqualTo("AppA")
        val sharedId = draftA.assets[0].id

        inventory.addAsset(b.id, DraftAssetWrite(assetId = sharedId))

        val deps = inventory.inferDependsOn(a.id)
        assertThat(deps).hasSize(1)
        assertThat(deps[0].applicationName).isEqualTo("AppB")
        assertThat(deps[0].sharedAssetIds).containsExactly(sharedId)
    }

    @Test
    fun shouldAddRelationBetweenDraftAssets() {
        val app = inventory.create(CreateApplicationRequest(name = "RelApp"))
        val first =
            inventory.addAsset(
                app.id,
                DraftAssetWrite(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "parent",
                            "version" to "1",
                            "ecosystem" to "maven",
                            "kind" to "library",
                        ),
                ),
            ).assets.single()
        val second =
            inventory.addAsset(
                app.id,
                DraftAssetWrite(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "child",
                            "version" to "1",
                            "ecosystem" to "maven",
                            "kind" to "library",
                        ),
                ),
            ).assets.first { it.id != first.id }

        val withRel =
            inventory.addRelation(
                app.id,
                DraftRelationWrite(
                    fromAssetId = first.id,
                    toAssetId = second.id,
                    role = SbomRoles.DEPENDS_ON,
                ),
            )
        assertThat(withRel.relations).hasSize(1)
        assertThat(withRel.relations[0].label).isEqualTo("Depends On")
    }

    @Test
    fun shouldRejectDuplicateApplicationName() {
        inventory.create(CreateApplicationRequest(name = "Dup"))
        org.assertj.core.api.Assertions.assertThatThrownBy {
            inventory.create(CreateApplicationRequest(name = "dup"))
        }.isInstanceOf(ResponseStatusException::class.java)
    }

    @Test
    fun shouldReturnPortalStatsWithoutLatestReleased() {
        val app = inventory.create(CreateApplicationRequest(name = "StatsApp", targetVersion = "1.0.0"))
        val stats = inventory.portalStats(app.id)
        assertThat(stats.applicationId).isEqualTo(app.id)
        assertThat(stats.versionCount).isEqualTo(1)
        assertThat(stats.bomCount).isEqualTo(1)
        assertThat(stats.latestVersion).isNull()
        assertThat(stats.latestMultiBom).isFalse()
    }
}
