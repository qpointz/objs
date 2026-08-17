package org.poc.objs.sbom

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.persistence.BoMAllowedEdgeRuleRepository
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMSchemaCatalogRepository
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.persistence.BoMPoolEntityReader
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.poc.objs.sbom.annotations.Provenance
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.builder.SbomGraphBuilder
import org.poc.objs.sbom.model.ComponentPayload
import org.poc.objs.sbom.registry.SbomRegistry
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
        "spring.datasource.url=jdbc:h2:mem:sbom;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
    ],
)
class SbomServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var sbom: SbomService

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @Autowired
    lateinit var edges: BoMAllowedEdgeCatalog

    @Autowired
    lateinit var schemaRepository: BoMSchemaCatalogRepository

    @Autowired
    lateinit var edgeRuleRepository: BoMAllowedEdgeRuleRepository

    @BeforeEach
    fun resetCatalogs() {
        schemas.clear()
        edges.clear()
        // allow re-register after clear
        val field = SbomService::class.java.getDeclaredField("packRegistered")
        field.isAccessible = true
        field.setBoolean(sbom, false)
        sbom.ensureRegistry()
    }

    @Test
    fun shouldPersistCanonicalRegistry() {
        val pack = SbomRegistry.pack()
        assertThat(schemaRepository.count()).isEqualTo(pack.schemas.size.toLong())
        assertThat(edgeRuleRepository.count()).isEqualTo(pack.edgeRules.size.toLong())
    }

    @Test
    fun shouldRejectManualWithoutCapturedBy() {
        assertThatThrownBy {
            Provenance.manual("  ")
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun shouldPersistAndFetchByAppVersion() {
        val payments231 = SbomContext("payments-api", "2.3.1")
        val builder = SbomGraphBuilder(payments231)
        val boot = builder.addComponent(
            ComponentPayload("Spring Boot", "3.3.0", "Maven", "framework"),
            Provenance.detected(),
            key = "boot",
        )
        val jackson = builder.addComponent(
            ComponentPayload("Jackson", "2.17.0", "Maven", "library"),
            Provenance.manual("alice"),
            key = "jackson",
        )
        builder.dependsOn(boot, jackson)
        val result = sbom.save(builder.build())
        assertThat(result.isValid)
            .withFailMessage { result.issues.joinToString { "${it.code}: ${it.message}" } }
            .isTrue()

        val billing = SbomContext("billing-api", "1.0.0")
        val billingBuilder = SbomGraphBuilder(billing)
        billingBuilder.addComponent(
            ComponentPayload("Guava", "33.0", "Maven", "library"),
            Provenance.enriched("catalog1"),
        )
        assertThat(sbom.save(billingBuilder.build()).isValid).isTrue()

        val payments240 = SbomGraphBuilder(SbomContext("payments-api", "2.4.0"))
        payments240.addComponent(
            ComponentPayload("Spring Boot", "3.4.0", "Maven", "framework"),
            Provenance.detected(),
        )
        assertThat(sbom.save(payments240.build()).isValid).isTrue()

        val bom = sbom.getSbom("payments-api", "2.3.1")
        assertThat(bom.entities).hasSize(2)
        assertThat(bom.edges).hasSize(1)
        assertThat(bom.entities).allMatch {
            it.annotations["app"] == "payments-api" && it.annotations["appVersion"] == "2.3.1"
        }

        val allPayments = sbom.getSbom("payments-api")
        assertThat(allPayments.entities).hasSize(3)

        val manualOnly = sbom.getSbom("payments-api", "2.3.1", mapOf("source" to "manual"))
        assertThat(manualOnly.entities).hasSize(1)
        assertThat(manualOnly.entities[0].payload["name"]).isEqualTo("Jackson")
    }

    @Test
    fun shouldListApplicationsAndVersions() {
        assertThat(sbom.listApplications().applications).isEmpty()

        assertThat(
            sbom.save(
                SbomGraphBuilder(SbomContext("payments-api", "2.3.1"))
                    .also {
                        it.addComponent(
                            ComponentPayload("Spring Boot", "3.3.0", "Maven", "framework"),
                            Provenance.detected(),
                        )
                    }
                    .build(),
            ).isValid,
        ).isTrue()
        assertThat(
            sbom.save(
                SbomGraphBuilder(SbomContext("payments-api", "2.4.0"))
                    .also {
                        it.addComponent(
                            ComponentPayload("Spring Boot", "3.4.0", "Maven", "framework"),
                            Provenance.detected(),
                        )
                    }
                    .build(),
            ).isValid,
        ).isTrue()
        assertThat(
            sbom.save(
                SbomGraphBuilder(SbomContext("billing-api", "1.0.0"))
                    .also {
                        it.addComponent(
                            ComponentPayload("Guava", "33.0", "Maven", "library"),
                            Provenance.detected(),
                        )
                    }
                    .build(),
            ).isValid,
        ).isTrue()

        val catalog = sbom.listApplications()
        assertThat(catalog.applications.map { it.app }).containsExactly("billing-api", "payments-api")
        assertThat(catalog.applications[0].versions).containsExactly("1.0.0")
        assertThat(catalog.applications[1].versions).containsExactly("2.3.1", "2.4.0")
    }
}
