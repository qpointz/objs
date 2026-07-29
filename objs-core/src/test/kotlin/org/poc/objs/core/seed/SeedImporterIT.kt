package org.poc.objs.core.seed

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.persistence.BoMEdgeRepository
import org.poc.objs.core.persistence.BoMEntityRepository
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class SeedImporterIT {
    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var importer: SeedImporter

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @Autowired
    lateinit var rules: BoMAllowedEdgeCatalog

    @Autowired
    lateinit var entities: BoMEntityRepository

    @Autowired
    lateinit var edges: BoMEdgeRepository

    @BeforeEach
    fun clear() {
        edges.deleteAll()
        entities.deleteAll()
        schemas.clear()
        rules.clear()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun shouldRollbackEntireResource_whenLaterDocumentFails() {
        val yaml = """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Person
            version: "1"
            contentSchema:
              type: OBJECT
              title: Person
              description: Person payload
              fields:
                - name: name
                  required: true
                  schema:
                    type: STRING
                    title: Name
                    description: Person name
            ---
            apiVersion: objs.poc.org/v1
            kind: Graph
            name: demo
            entities:
              - key: p
                type: Person
                schemaVersion: "1"
                annotations: {}
                payload: {}
            edges: []
        """.trimIndent()

        assertThatThrownBy { importer.importYaml(yaml) }
            .isInstanceOf(SeedImportException::class.java)

        assertThat(schemas.get("Person", "1")).isNull()
        assertThat(entities.count()).isZero()
    }

    @Test
    fun shouldMergeGraphByUuidV5WithoutDuplicates() {
        val yaml = """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Person
            version: "1"
            contentSchema:
              type: OBJECT
              title: Person
              description: Person payload
              fields:
                - name: name
                  required: true
                  schema:
                    type: STRING
                    title: Name
                    description: Person name
            ---
            apiVersion: objs.poc.org/v1
            kind: Graph
            name: demo
            entities:
              - key: p
                type: Person
                schemaVersion: "1"
                annotations:
                  app: demo
                payload:
                  name: Ada
            edges: []
        """.trimIndent()

        assertThat(importer.importYaml(yaml).isSuccess).isTrue()
        assertThat(importer.importYaml(yaml).isSuccess).isTrue()
        assertThat(entities.count()).isEqualTo(1)
        assertThat(entities.findById(UuidV5.entityId("demo", "p"))).isPresent
        assertThat(schemas.get("Person", "1")).isNotNull
    }

    @Test
    fun shouldLeaveOmittedRecordsUnchangedUnderMerge() {
        val first = """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Person
            version: "1"
            contentSchema:
              type: OBJECT
              title: Person
              description: Person payload
              fields:
                - name: name
                  required: true
                  schema:
                    type: STRING
                    title: Name
                    description: Person name
            ---
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Org
            version: "1"
            contentSchema:
              type: OBJECT
              title: Org
              description: Org payload
              fields:
                - name: name
                  required: true
                  schema:
                    type: STRING
                    title: Name
                    description: Org name
        """.trimIndent()
        importer.importYaml(first)
        assertThat(schemas.get("Person", "1")).isNotNull
        assertThat(schemas.get("Org", "1")).isNotNull

        val second = """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Person
            version: "1"
            contentSchema:
              type: OBJECT
              title: Person
              description: Person payload updated
              fields:
                - name: name
                  required: true
                  schema:
                    type: STRING
                    title: Name
                    description: Person name
        """.trimIndent()
        importer.importYaml(second)
        assertThat(schemas.get("Person", "1")!!.contentSchema.description).isEqualTo("Person payload updated")
        assertThat(schemas.get("Org", "1")).isNotNull
    }
}
