package org.poc.objs.core.seed

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog

class SeedImporterTest {
    private lateinit var schemas: InMemoryBoMSchemaCatalog
    private lateinit var rules: InMemoryBoMAllowedEdgeCatalog
    private lateinit var importer: SeedImporter
    private lateinit var serializer: CanonicalSeedSerializer
    private lateinit var objectHandler: ObjectSchemaSeedHandler
    private lateinit var ruleHandler: AllowedEdgeRuleSeedHandler

    @BeforeEach
    fun setUp() {
        schemas = InMemoryBoMSchemaCatalog()
        rules = InMemoryBoMAllowedEdgeCatalog()
        objectHandler = ObjectSchemaSeedHandler(schemas)
        ruleHandler = AllowedEdgeRuleSeedHandler(rules)
        // Graph apply needs BoMGraphStore; covered by SeedImporterIT. Unit tests cover schema/rule kinds.
        importer = SeedImporter(listOf(objectHandler, ruleHandler))
        serializer = CanonicalSeedSerializer(
            schemas,
            rules,
            objectHandler,
            ruleHandler,
            GraphSeedHandler(
                // unused in these tests
                org.mockito.Mockito.mock(org.poc.objs.core.persistence.BoMGraphStore::class.java),
            ),
        )
    }

    @Test
    fun shouldApplyDocumentsIndependentOfDeclarationOrder() {
        val yaml = """
            apiVersion: objs.poc.org/v1
            kind: AllowedEdgeRule
            sourceType: Person
            role: knows
            targetType: Person
            propertiesPolicy: NONE
            ---
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
        """.trimIndent()

        val result = importer.importYaml(yaml)
        assertThat(result.isSuccess).isTrue()
        assertThat(schemas.get("Person", "1")).isNotNull
        assertThat(rules.find("Person", "knows", "Person")).isNotNull
        assertThat(result.appliedByKind()[SEED_KIND_OBJECT_SCHEMA]).isEqualTo(1)
        assertThat(result.appliedByKind()[SEED_KIND_ALLOWED_EDGE_RULE]).isEqualTo(1)
        assertThat(rules.find("Person", "knows", "Person")!!.cardinality)
            .isEqualTo(org.poc.objs.core.domain.BoMEdgeCardinality.UNSPECIFIED)
    }

    @Test
    fun shouldParseCardinalityOnAllowedEdgeRule() {
        val yaml = """
            apiVersion: objs.poc.org/v1
            kind: AllowedEdgeRule
            sourceType: Product
            role: CONTAINS
            targetType: Component
            propertiesPolicy: NONE
            cardinality: "1:*"
        """.trimIndent()

        val result = importer.importYaml(yaml)
        assertThat(result.isSuccess).isTrue()
        assertThat(rules.find("Product", "CONTAINS", "Component")!!.cardinality)
            .isEqualTo(org.poc.objs.core.domain.BoMEdgeCardinality.ONE_TO_MANY)
    }

    @Test
    fun shouldRejectUnknownCardinality() {
        assertThatThrownBy {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v1
                kind: AllowedEdgeRule
                sourceType: A
                role: r
                targetType: B
                cardinality: "1:0..1"
                """.trimIndent(),
            )
        }.isInstanceOf(SeedImportException::class.java)
    }

    @Test
    fun shouldRejectUnsupportedApiVersionAndKind() {
        assertThatThrownBy {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v0
                kind: ObjectSchema
                type: X
                version: "1"
                contentSchema:
                  type: OBJECT
                  title: X
                  description: X
                  fields: []
                """.trimIndent(),
            )
        }.isInstanceOf(SeedImportException::class.java)

        assertThatThrownBy {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v1
                kind: UnknownKind
                """.trimIndent(),
            )
        }.isInstanceOf(SeedImportException::class.java)

        try {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v1
                kind: UnknownKind
                """.trimIndent(),
            )
        } catch (ex: SeedImportException) {
            assertThat(ex.result.allErrors().map { it.code }).contains("SEED_KIND_UNSUPPORTED")
        }
    }

    @Test
    fun shouldRejectInvalidObjectSchemaDsl() {
        assertThatThrownBy {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v1
                kind: ObjectSchema
                type: Bad
                version: "1"
                contentSchema:
                  type: STRING
                  title: Bad
                  description: not an object root
                """.trimIndent(),
            )
        }.isInstanceOf(SeedImportException::class.java)
    }

    @Test
    fun shouldParseGraphWithDeterministicUuidV5Ids() {
        val handler = GraphSeedHandler(
            org.mockito.Mockito.mock(org.poc.objs.core.persistence.BoMGraphStore::class.java),
        )
        val docs = SeedYaml.parseDocuments(
            """
            apiVersion: objs.poc.org/v1
            kind: Graph
            name: demo
            entities:
              - key: p1
                type: Person
                schemaVersion: "1"
                annotations:
                  app: demo
                payload:
                  name: Ada
            edges:
              - key: e1
                source: p1
                target: p1
                role: knows
            """.trimIndent(),
        )
        val parsed = handler.parse(docs.single())
        val payload = parsed.payload as SeedGraphPayload
        assertThat(payload.graph.entities.single().id).isEqualTo(UuidV5.entityId("demo", "p1"))
        assertThat(payload.graph.edges.single().id).isEqualTo(UuidV5.edgeId("demo", "e1"))
        assertThat(payload.graph.edges.single().source).isEqualTo(UuidV5.entityId("demo", "p1"))
    }

    @Test
    fun shouldRoundTripSchemaAndRuleKinds() {
        schemas.register(
            BoMSchema(
                "Person",
                "1",
                BoMSchemaDsl.obj(
                    "Person",
                    "Person payload",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Person name"))),
                ),
                usages = setOf(BoMSchemaUsage.ENTITY),
            ),
        )
        rules.register(
            BoMAllowedEdgeRule(
                "Person",
                "knows",
                "Person",
                BoMPropertiesPolicy.NONE,
            ),
        )
        val yaml = serializer.serializeCatalogs()
        assertThat(yaml).doesNotContain("metadata:")
        assertThat(yaml).doesNotContain("spec:")
        assertThat(yaml).contains("type: \"Person\"")
        assertThat(yaml).contains("sourceType: \"Person\"")
        assertThat(yaml).contains("cardinality: \"UNSPECIFIED\"")
        schemas.clear()
        rules.clear()
        val result = importer.importYaml(yaml)
        assertThat(result.isSuccess).isTrue()
        assertThat(schemas.get("Person", "1")).isNotNull
        assertThat(rules.find("Person", "knows", "Person")).isNotNull
        assertThat(rules.find("Person", "knows", "Person")!!.cardinality)
            .isEqualTo(org.poc.objs.core.domain.BoMEdgeCardinality.UNSPECIFIED)
    }
}
