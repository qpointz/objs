package org.poc.objs.sbom.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.persistence.BoMPoolEntityReader
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
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
    AssetTypeCatalogService::class,
    SchemaBrowseService::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-schema-browse;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class SchemaBrowseServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @Autowired
    lateinit var sbom: SbomService

    @Autowired
    lateinit var browse: SchemaBrowseService

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
    fun shouldExposeEntityCatalogWithLatestVersion() {
        val catalog = browse.catalog()
        assertThat(catalog.map { it.type }).contains("Component", "CanonicalEdge")
        val component = catalog.single { it.type == "Component" }
        assertThat(component.latestVersion).isNotBlank()
        assertThat(component.versions).isNotEmpty()
        assertThat(component.description).isNotBlank()
        assertThat(component.usedIn).isEmpty()
        assertThat(browse.usedInForType(component.type)).isEmpty()
        val loaded = browse.get(component.type, component.latestVersion)
        assertThat(loaded.type).isEqualTo("Component")
        assertThat(loaded.contentSchema.fields).isNotEmpty()
    }

    @Test
    fun shouldGroupRegisteredVersions() {
        schemas.register(
            BoMSchema(
                type = "ExtraThing",
                version = "1.0.0",
                usage = BoMSchemaUsage.ENTITY,
                contentSchema = BoMSchemaDsl.obj("ExtraThing", "v1", emptyList()),
            ),
        )
        schemas.register(
            BoMSchema(
                type = "ExtraThing",
                version = "2.0.0",
                usage = BoMSchemaUsage.ENTITY,
                contentSchema = BoMSchemaDsl.obj("ExtraThing", "v2", emptyList()),
            ),
        )
        val entry = browse.catalog().single { it.type == "ExtraThing" }
        assertThat(entry.versions).containsExactly("1.0.0", "2.0.0")
        assertThat(entry.latestVersion).isEqualTo("2.0.0")
        assertThat(entry.description).isEqualTo("v2")
    }

    @Test
    fun shouldListIncomingAndOutgoingAllowedEdgesIncludingWildcardsAndMetadata() {
        val product = browse.allowedEdgesForType("Product")
        val contains = product.outgoing.single { it.role == "CONTAINS" && it.targetType == "Component" }
        assertThat(contains.description).isEqualTo("Product includes the software component in its bill")
        assertThat(contains.sourceVerb).isEqualTo("contains")
        assertThat(contains.targetVerb).isEqualTo("contained in")
        assertThat(contains.tags).containsExactly("composition")

        val component = browse.allowedEdgesForType("Component")
        assertThat(component.incoming).anyMatch { it.sourceType == "Product" && it.role == "CONTAINS" }

        edges.register(
            BoMAllowedEdgeRule(
                sourceType = BoMAllowedEdgeRule.ANY,
                role = "ANNOTATES",
                targetType = BoMAllowedEdgeRule.ANY,
            ),
        )
        val withWild = browse.allowedEdgesForType("Component")
        assertThat(withWild.incoming).anyMatch { it.role == "ANNOTATES" && it.sourceType == "*" }
        assertThat(withWild.outgoing).anyMatch { it.role == "ANNOTATES" && it.targetType == "*" }
    }
}
