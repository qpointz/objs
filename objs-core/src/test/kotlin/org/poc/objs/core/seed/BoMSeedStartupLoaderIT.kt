package org.poc.objs.core.seed

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMSeedLedgerRepository
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-seed-startup;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "objs.seeds.enabled=false",
    ],
)
class BoMSeedStartupLoaderIT {
    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var importer: SeedImporter

    @Autowired
    lateinit var ledger: BoMSeedLedger

    @Autowired
    lateinit var ledgerRepo: BoMSeedLedgerRepository

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @BeforeEach
    fun clear() {
        schemas.clear()
        ledgerRepo.deleteAll()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun shouldSkipUnchangedAndReimportChangedBytes() {
        val file = Files.createTempFile("seed-", ".yaml")
        Files.writeString(file, validSchemaYaml("Person", "Person payload"))
        val props = BoMSeedProperties(
            enabled = true,
            onFailure = SeedFailureMode.FAIL_FAST,
            resources = mutableListOf(
                SeedResourceProperties(location = file.toUri().toString(), name = "person"),
            ),
        )
        val loader = BoMSeedStartupLoader(props, DefaultResourceLoader(), importer, ledger)

        val first = loader.loadConfiguredResources()
        assertThat(first.resources.single().status).isEqualTo(SeedLedgerStatus.SUCCESS)
        assertThat(schemas.get("Person", "1")).isNotNull

        val second = loader.loadConfiguredResources()
        assertThat(second.resources.single().status).isEqualTo(SeedLedgerStatus.SKIPPED)

        Files.writeString(file, validSchemaYaml("Person", "Person payload changed"))
        val third = loader.loadConfiguredResources()
        assertThat(third.resources.single().status).isEqualTo(SeedLedgerStatus.SUCCESS)
        assertThat(schemas.get("Person", "1")!!.contentSchema.description)
            .isEqualTo("Person payload changed")

        Files.deleteIfExists(file)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun shouldContinueAfterFailure_whenConfigured() {
        val good = Files.createTempFile("seed-good-", ".yaml")
        val bad = Files.createTempFile("seed-bad-", ".yaml")
        Files.writeString(good, validSchemaYaml("Org", "Org payload"))
        Files.writeString(
            bad,
            """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            metadata:
              type: Bad
              version: "1"
            spec:
              contentSchema:
                type: STRING
                title: Bad
                description: not object
            """.trimIndent(),
        )

        val props = BoMSeedProperties(
            enabled = true,
            onFailure = SeedFailureMode.CONTINUE,
            resources = mutableListOf(
                SeedResourceProperties(location = bad.toUri().toString(), name = "bad"),
                SeedResourceProperties(location = good.toUri().toString(), name = "good"),
            ),
        )
        val loader = BoMSeedStartupLoader(props, DefaultResourceLoader(), importer, ledger)
        val result = loader.loadConfiguredResources()
        assertThat(result.resources.map { it.status }).containsExactly(
            SeedLedgerStatus.FAILED,
            SeedLedgerStatus.SUCCESS,
        )
        assertThat(schemas.get("Org", "1")).isNotNull
        assertThat(ledger.find("bad")!!.lastAttemptStatus).isEqualTo(SeedLedgerStatus.FAILED.name)
        assertThat(ledger.find("bad")!!.lastSuccessFingerprint).isNull()

        Files.deleteIfExists(good)
        Files.deleteIfExists(bad)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun shouldFailFastAndPreservePriorSuccessFingerprint() {
        val file = Files.createTempFile("seed-ff-", ".yaml")
        Files.writeString(file, validSchemaYaml("Thing", "ok"))
        val props = BoMSeedProperties(
            enabled = true,
            onFailure = SeedFailureMode.FAIL_FAST,
            resources = mutableListOf(
                SeedResourceProperties(location = file.toUri().toString(), name = "thing"),
            ),
        )
        val loader = BoMSeedStartupLoader(props, DefaultResourceLoader(), importer, ledger)
        loader.loadConfiguredResources()
        val successFp = ledger.find("thing")!!.lastSuccessFingerprint

        Files.writeString(
            file,
            """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            metadata:
              type: Thing
              version: "1"
            spec:
              contentSchema:
                type: STRING
                title: Thing
                description: bad root
            """.trimIndent(),
        )
        assertThatThrownBy { loader.loadConfiguredResources() }
            .isInstanceOf(SeedStartupException::class.java)

        val record = ledger.find("thing")!!
        assertThat(record.lastSuccessFingerprint).isEqualTo(successFp)
        assertThat(record.lastAttemptStatus).isEqualTo(SeedLedgerStatus.FAILED.name)

        Files.deleteIfExists(file)
    }

    private fun validSchemaYaml(type: String, description: String): String = """
        apiVersion: objs.poc.org/v1
        kind: ObjectSchema
        metadata:
          type: $type
          version: "1"
        spec:
          contentSchema:
            type: OBJECT
            title: $type
            description: $description
            fields:
              - name: name
                required: true
                schema:
                  type: STRING
                  title: Name
                  description: Name
    """.trimIndent()
}
