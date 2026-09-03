package org.poc.objs.core.seed

import org.poc.objs.api.seed.SeedImportException
import org.poc.objs.api.seed.UuidV5
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.persistence.ObjsPersistenceFixture

class SeedImporterIT : ObjsPersistenceFixture() {

    @BeforeEach
    fun clear() {
        uow.write {
            edges.deleteAll()
            entities.deleteAll()
        }
        schemas.clear()
        rules.clear()
    }

    @Test
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
        assertThat(uow.read { entities.count() }).isZero()
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
        assertThat(uow.read { entities.count() }).isEqualTo(1)
        assertThat(uow.read { entities.findById(UuidV5.entityId("demo", "p")) }).isNotNull
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
