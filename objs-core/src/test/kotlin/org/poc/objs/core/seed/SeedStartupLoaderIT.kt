package org.poc.objs.core.seed

import org.poc.objs.api.seed.SeedResourceIdentity
import org.poc.objs.api.seed.SeedResourceResolver
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.persistence.ObjsPersistenceFixture
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

class SeedStartupLoaderIT : ObjsPersistenceFixture() {

    @BeforeEach
    fun clear() {
        schemas.clear()
        uow.write { ledgerRepo.deleteAll() }
    }

    @Test
    fun shouldSkipUnchangedAndReimportChangedBytes() {
        val file = Files.createTempFile("seed-", ".yaml")
        Files.writeString(file, validSchemaYaml("Person", "Person payload"))
        val location = file.toUri().toString()
        val props = SeedProperties(
            enabled = true,
            onFailure = SeedFailureMode.FAIL_FAST,
            resources = mutableListOf(location),
        )
        val loader = SeedStartupLoader(props, fileResolver(), importer, ledger)

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
    fun shouldContinueAfterFailure_whenConfigured() {
        val good = Files.createTempFile("seed-good-", ".yaml")
        val bad = Files.createTempFile("seed-bad-", ".yaml")
        Files.writeString(good, validSchemaYaml("Org", "Org payload"))
        val goodLocation = good.toUri().toString()
        val badLocation = bad.toUri().toString()
        Files.writeString(
            bad,
            """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Bad
            version: "1"
            contentSchema:
              type: STRING
              title: Bad
              description: not object
            """.trimIndent(),
        )

        val props = SeedProperties(
            enabled = true,
            onFailure = SeedFailureMode.CONTINUE,
            resources = mutableListOf(badLocation, goodLocation),
        )
        val loader = SeedStartupLoader(props, fileResolver(), importer, ledger)
        val result = loader.loadConfiguredResources()
        assertThat(result.resources.map { it.status }).containsExactly(
            SeedLedgerStatus.FAILED,
            SeedLedgerStatus.SUCCESS,
        )
        assertThat(schemas.get("Org", "1")).isNotNull
        val badKey = SeedResourceIdentity.ledgerKey(badLocation)
        assertThat(ledger.find(badKey)!!.lastAttemptStatus).isEqualTo(SeedLedgerStatus.FAILED.name)
        assertThat(ledger.find(badKey)!!.lastSuccessFingerprint).isNull()

        Files.deleteIfExists(good)
        Files.deleteIfExists(bad)
    }

    @Test
    fun shouldFailFastAndPreservePriorSuccessFingerprint() {
        val file = Files.createTempFile("seed-ff-", ".yaml")
        Files.writeString(file, validSchemaYaml("Thing", "ok"))
        val location = file.toUri().toString()
        val seedKey = SeedResourceIdentity.ledgerKey(location)
        val props = SeedProperties(
            enabled = true,
            onFailure = SeedFailureMode.FAIL_FAST,
            resources = mutableListOf(location),
        )
        val loader = SeedStartupLoader(props, fileResolver(), importer, ledger)
        loader.loadConfiguredResources()
        val successFp = ledger.find(seedKey)!!.lastSuccessFingerprint

        Files.writeString(
            file,
            """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Thing
            version: "1"
            contentSchema:
              type: STRING
              title: Thing
              description: bad root
            """.trimIndent(),
        )
        assertThatThrownBy { loader.loadConfiguredResources() }
            .isInstanceOf(SeedStartupException::class.java)

        val record = ledger.find(seedKey)!!
        assertThat(record.lastSuccessFingerprint).isEqualTo(successFp)
        assertThat(record.lastAttemptStatus).isEqualTo(SeedLedgerStatus.FAILED.name)

        Files.deleteIfExists(file)
    }

    private fun validSchemaYaml(type: String, description: String): String = """
        apiVersion: objs.poc.org/v1
        kind: ObjectSchema
        type: $type
        version: "1"
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

    private fun fileResolver(): SeedResourceResolver = SeedResourceResolver { location ->
        val path = Paths.get(URI.create(location))
        if (!Files.exists(path)) null else Files.newInputStream(path)
    }
}
