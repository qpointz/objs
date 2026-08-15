package org.poc.objs.sbom

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.persistence.BoMPoolEntityReader
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.poc.objs.sbom.annotations.Provenance
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.builder.SbomGraphBuilder
import org.poc.objs.sbom.model.ComponentPayload
import org.poc.objs.sbom.model.LicensePayload
import org.poc.objs.sbom.model.LicenseType
import org.poc.objs.sbom.model.OrganizationPayload
import org.poc.objs.sbom.model.OrganizationType
import org.poc.objs.sbom.model.ProductPayload
import org.poc.objs.sbom.model.ProductType
import org.poc.objs.sbom.registry.SbomRoles
import org.poc.objs.sbom.service.SbomService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class, BoMNamedGraphStore::class, BoMPoolEntityReader::class, SbomService::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-wavea;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class WaveATypesTest {

    @SpringBootConfiguration
    class TestApp

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
    fun shouldPersistWaveAGraph() {
        val ctx = SbomContext("payments-api", "2.3.1")
        val b = SbomGraphBuilder(ctx)
        val product = b.add(ProductType.entity(ProductPayload("Payments API", "2.3.1")), Provenance.detected(), "product")
        val org = b.add(OrganizationType.entity(OrganizationPayload("Acme")), Provenance.enriched("catalog1"), "org")
        val lic = b.add(LicenseType.entity(LicensePayload("Apache 2.0", "Apache-2.0")), Provenance.detected(), "lic")
        val comp = b.addComponent(
            ComponentPayload("Spring Boot", "3.3.0", "Maven", "framework"),
            Provenance.detected(),
            "boot",
        )
        b.link(product, SbomRoles.CONTAINS, comp)
        b.link(product, SbomRoles.OWNED_BY, org)
        b.link(comp, SbomRoles.PROVIDED_BY, org)
        b.link(comp, SbomRoles.LICENSED_UNDER, lic)

        val result = sbom.save(b.build())
        assertThat(result.isValid)
            .withFailMessage { result.issues.joinToString { "${it.code}: ${it.message}" } }
            .isTrue()

        val bom = sbom.getSbom(ctx)
        assertThat(bom.entities).hasSize(4)
        assertThat(bom.edges).hasSize(4)
    }
}
