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
import org.poc.objs.sbom.domain.PromoteVersionRequest
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
    CycloneDxExportService::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-cdx;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class CycloneDxExportServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var inventory: ApplicationInventoryService

    @Autowired
    lateinit var versions: ApplicationVersionService

    @Autowired
    lateinit var export: CycloneDxExportService

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
    fun shouldExportDraftAsCycloneDxShapedJson() {
        val app = inventory.create(CreateApplicationRequest(name = "Payments"))
        val parent =
            inventory.addAsset(
                app.id,
                DraftAssetWrite(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "Spring Boot",
                            "version" to "3.3.0",
                            "ecosystem" to "Maven",
                            "kind" to "framework",
                        ),
                ),
            ).assets.single()
        val child =
            inventory.addAsset(
                app.id,
                DraftAssetWrite(
                    type = "Component",
                    payload =
                        mapOf(
                            "name" to "Jackson",
                            "version" to "2.17.0",
                            "ecosystem" to "Maven",
                            "kind" to "library",
                            "coordinates" to "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.17.0",
                        ),
                ),
            ).assets.first { it.id != parent.id }
        inventory.addRelation(
            app.id,
            DraftRelationWrite(fromAssetId = parent.id, toAssetId = child.id, role = SbomRoles.DEPENDS_ON),
        )

        val bom = export.exportDraft(app.id)
        assertThat(bom["bomFormat"]).isEqualTo("CycloneDX")
        assertThat(bom["specVersion"]).isEqualTo("1.6")
        @Suppress("UNCHECKED_CAST")
        val metadata = bom["metadata"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val metaComponent = metadata["component"] as Map<String, Any?>
        assertThat(metaComponent["type"]).isEqualTo("application")
        assertThat(metaComponent["name"]).isEqualTo("Payments")
        assertThat(metaComponent["version"]).isEqualTo("draft")

        @Suppress("UNCHECKED_CAST")
        val components = bom["components"] as List<Map<String, Any?>>
        assertThat(components).hasSize(2)
        assertThat(components.map { it["name"] }).containsExactlyInAnyOrder("Spring Boot", "Jackson")
        assertThat(components.any { it["purl"] != null }).isTrue()

        @Suppress("UNCHECKED_CAST")
        val dependencies = bom["dependencies"] as List<Map<String, Any?>>
        val parentDep = dependencies.first { it["ref"] == parent.id.toString() }
        @Suppress("UNCHECKED_CAST")
        assertThat(parentDep["dependsOn"] as List<String>).containsExactly(child.id.toString())

        val version = versions.promote(app.id, versions.draft(app.id)!!.id, PromoteVersionRequest("1.0.0"))
        val versionBom = export.exportVersion(app.id, version.version.id)
        @Suppress("UNCHECKED_CAST")
        val vMeta = (versionBom["metadata"] as Map<String, Any?>)["component"] as Map<String, Any?>
        assertThat(vMeta["version"]).isEqualTo("1.0.0")
    }
}
