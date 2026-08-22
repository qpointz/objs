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
import org.poc.objs.sbom.domain.AssetSearchRequest
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.CreatePoolAssetRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.DraftRelationWrite
import org.poc.objs.sbom.domain.CreateDraftVersionRequest
import org.poc.objs.sbom.domain.PromoteVersionRequest
import org.poc.objs.sbom.domain.SetAssetOwnerRequest
import org.poc.objs.sbom.domain.UpdatePoolAssetRequest
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
    BoMGraphStore::class,
    BoMNamedGraphStore::class,
    BoMPoolEntityReader::class,
    SbomService::class,
    AssetTypeCatalogService::class,
    ApplicationInventoryService::class,
    ApplicationVersionService::class,
    AssetInventoryService::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-assets;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class AssetInventoryServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var assets: AssetInventoryService

    @Autowired
    lateinit var inventory: ApplicationInventoryService

    @Autowired
    lateinit var versions: ApplicationVersionService

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
    fun shouldSearchByTypeAndSearchableFilter() {
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "jackson-core",
                        "version" to "2.17.0",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "guava",
                        "version" to "33.0",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )

        val hit =
            assets.search(
                AssetSearchRequest(type = "Component", filters = mapOf("name" to "jackson-core")),
            )
        assertThat(hit).hasSize(1)
        assertThat(hit[0].label).isEqualTo("jackson-core@2.17.0")
    }

    @Test
    fun shouldSearchByPrefixFilter() {
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "jackson-core",
                        "version" to "2.17.0",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "guava",
                        "version" to "33.0",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )

        val hit =
            assets.search(
                AssetSearchRequest(type = "Component", filters = mapOf("name" to "jackson*")),
            )
        assertThat(hit).hasSize(1)
        assertThat(hit[0].label).isEqualTo("jackson-core@2.17.0")
    }

    @Test
    fun shouldPageSearchResults() {
        repeat(3) { i ->
            assets.create(
                CreatePoolAssetRequest(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "page-comp-$i",
                            "version" to "1",
                            "ecosystem" to "maven",
                            "kind" to "library",
                        ),
                ),
            )
        }
        val first = assets.searchPage(AssetSearchRequest(type = "Component"), page = 1, size = 2)
        assertThat(first.size).isEqualTo(2)
        assertThat(first.items).hasSize(2)
        assertThat(first.total).isGreaterThanOrEqualTo(3)
        val second = assets.searchPage(AssetSearchRequest(type = "Component"), page = 2, size = 2)
        assertThat(second.page).isEqualTo(2)
        assertThat(first.items.map { it.id }.toSet()).doesNotContainAnyElementsOf(second.items.map { it.id })
    }

    @Test
    fun shouldSearchAllTypesAndObjExpr() {
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "across",
                        "version" to "1",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )
        val all = assets.search(AssetSearchRequest())
        assertThat(all.map { it.label }).contains("across@1")
        val expr =
            assets.search(AssetSearchRequest(objExpr = "p.name == 'across'"))
        assertThat(expr).hasSize(1)
        assertThat(expr[0].label).isEqualTo("across@1")
        val quoted =
            assets.search(AssetSearchRequest(objExpr = "p.ecosystem == \"maven\""))
        assertThat(quoted.map { it.label }).contains("across@1")
    }

    @Test
    fun shouldRejectNonSearchableFilters() {
        assertThatThrownBy {
            assets.search(
                AssetSearchRequest(type = "Component", filters = mapOf("description" to "secret")),
            )
        }.isInstanceOf(ResponseStatusException::class.java)
    }

    @Test
    fun shouldReportUsageAcrossDraftAndVersion() {
        val app = inventory.create(CreateApplicationRequest(name = "Payments"))
        val draft =
            inventory.addAsset(
                app.id,
                DraftAssetWrite(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "shared",
                            "version" to "1",
                            "ecosystem" to "maven",
                            "kind" to "library",
                        ),
                ),
            )
        val assetId = draft.assets.single().id
        val other =
            inventory.addAsset(
                app.id,
                DraftAssetWrite(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "dep",
                            "version" to "1",
                            "ecosystem" to "maven",
                            "kind" to "library",
                        ),
                ),
            ).assets.first { it.id != assetId }
        inventory.addRelation(
            app.id,
            DraftRelationWrite(fromAssetId = assetId, toAssetId = other.id, role = SbomRoles.DEPENDS_ON),
        )
        val released = versions.promote(app.id, versions.draft(app.id)!!.id, PromoteVersionRequest("1.0"))
        versions.createDraft(app.id, CreateDraftVersionRequest(fromVersionId = released.version.id))

        val detail = assets.get(assetId)
        assertThat(detail.usage.map { it.context }).contains("DRAFT", "RELEASED")
        assertThat(detail.usage.any { it.relations.any { rel -> rel.direction == "OUT" } }).isTrue()
    }

    @Test
    fun shouldFindDuplicateGroupsByIdentifier() {
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "dup",
                        "version" to "1.0",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "dup",
                        "version" to "1.0",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "unique",
                        "version" to "1.0",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )

        val groups = assets.findDuplicates("Component")
        assertThat(groups).hasSize(1)
        assertThat(groups[0].assets).hasSize(2)
        assertThat(groups[0].identity["name"]).isEqualTo("dup")
    }

    @Test
    fun shouldCountObjectsPerType() {
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "a",
                        "version" to "1",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )
        assets.create(
            CreatePoolAssetRequest(
                type = "Component",
                payload =
                    mapOf(
                        "name" to "b",
                        "version" to "1",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        )
        val stats = assets.statistics("Component")
        assertThat(stats.type).isEqualTo("Component")
        assertThat(stats.objectCount).isEqualTo(2)
    }

    @Test
    fun shouldSetAndClearOwner() {
        inventory.create(CreateApplicationRequest(name = "OwnerApp"))
        val asset =
            assets.create(
                CreatePoolAssetRequest(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "owned",
                            "version" to "1",
                            "ecosystem" to "maven",
                            "kind" to "library",
                        ),
                    owner = "OwnerApp",
                ),
            )
        assertThat(asset.owner).isEqualTo("OwnerApp")
        val cleared = assets.setOwner(asset.id, SetAssetOwnerRequest(owner = null))
        assertThat(cleared.owner).isNull()
    }

    @Test
    fun shouldUpdateNonIdentifierPayloadAndRejectIdentifierChange() {
        val asset =
            assets.create(
                CreatePoolAssetRequest(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "jackson-core",
                            "version" to "2.17.0",
                            "ecosystem" to "maven",
                            "kind" to "library",
                            "description" to "old",
                        ),
                ),
            )
        val updated =
            assets.update(
                asset.id,
                UpdatePoolAssetRequest(
                    payload =
                        mapOf(
                            "name" to "jackson-core",
                            "version" to "2.17.0",
                            "ecosystem" to "maven",
                            "kind" to "library",
                            "description" to "JSON processor",
                            "coordinates" to "com.fasterxml.jackson.core:jackson-core",
                        ),
                ),
            )
        assertThat(updated.payload["description"]).isEqualTo("JSON processor")
        assertThat(updated.payload["coordinates"]).isEqualTo("com.fasterxml.jackson.core:jackson-core")
        assertThatThrownBy {
            assets.update(
                asset.id,
                UpdatePoolAssetRequest(
                    payload =
                        mapOf(
                            "name" to "other",
                            "version" to "2.17.0",
                            "ecosystem" to "maven",
                            "kind" to "library",
                        ),
                ),
            )
        }.isInstanceOf(ResponseStatusException::class.java)
    }
}
