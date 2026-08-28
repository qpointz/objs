package org.poc.objs.core.seed

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.PropertiesPolicy
import org.poc.objs.core.domain.Schema
import org.poc.objs.core.domain.SchemaDsl
import org.poc.objs.core.domain.SchemaUsage
import org.poc.objs.core.domain.InMemoryAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemorySchemaCatalog

class SeedImporterTest {
    private lateinit var schemas: InMemorySchemaCatalog
    private lateinit var rules: InMemoryAllowedEdgeCatalog
    private lateinit var importer: SeedImporter
    private lateinit var serializer: CanonicalSeedSerializer
    private lateinit var objectHandler: ObjectSchemaSeedHandler
    private lateinit var ruleHandler: AllowedEdgeRuleSeedHandler

    @BeforeEach
    fun setUp() {
        schemas = InMemorySchemaCatalog()
        rules = InMemoryAllowedEdgeCatalog()
        objectHandler = ObjectSchemaSeedHandler(schemas)
        ruleHandler = AllowedEdgeRuleSeedHandler(rules)
        // Graph apply needs GraphStore; covered by SeedImporterIT. Unit tests cover schema/rule kinds.
        importer = SeedImporter(listOf(objectHandler, ruleHandler))
        serializer = CanonicalSeedSerializer(
            schemas,
            rules,
            objectHandler,
            ruleHandler,
            GraphSeedHandler(
                // unused in these tests
                org.mockito.Mockito.mock(org.poc.objs.core.persistence.NamedGraphStore::class.java),
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
            .isEqualTo(org.poc.objs.api.domain.EdgeCardinality.UNSPECIFIED)
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
            .isEqualTo(org.poc.objs.api.domain.EdgeCardinality.ONE_TO_MANY)
    }

    @Test
    fun shouldParseAllowedEdgeMetadata() {
        val yaml = """
            apiVersion: objs.poc.org/v1
            kind: AllowedEdgeRule
            sourceType: Product
            role: CONTAINS
            targetType: Component
            description: Product includes the component
            sourceVerb: contains
            targetVerb: contained in
            tags: [composition]
            attributes:
              ui.group: structure
        """.trimIndent()

        val result = importer.importYaml(yaml)
        assertThat(result.isSuccess).isTrue()
        val rule = rules.find("Product", "CONTAINS", "Component")!!
        assertThat(rule.description).isEqualTo("Product includes the component")
        assertThat(rule.sourceVerb).isEqualTo("contains")
        assertThat(rule.targetVerb).isEqualTo("contained in")
        assertThat(rule.tags).containsExactly("composition")
        assertThat(rule.attributes).containsEntry("ui.group", "structure")
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
            org.mockito.Mockito.mock(org.poc.objs.core.persistence.NamedGraphStore::class.java),
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
        assertThat(payload.graphId).isEqualTo(java.util.UUID.nameUUIDFromBytes("graph-seed:demo".toByteArray()))
        assertThat(payload.graph.edges.single().graphId).isEqualTo(payload.graphId)
    }

    @Test
    fun shouldRejectKind_whenNotAllowedForEndpoint() {
        assertThatThrownBy {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v1
                kind: Graph
                name: demo
                entities: []
                edges: []
                """.trimIndent(),
                CATALOG_SEED_KINDS,
            )
        }.isInstanceOf(SeedImportException::class.java)

        try {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v1
                kind: Graph
                name: demo
                entities: []
                edges: []
                """.trimIndent(),
                CATALOG_SEED_KINDS,
            )
        } catch (ex: SeedImportException) {
            assertThat(ex.result.allErrors().map { it.code }).contains("SEED_KIND_NOT_ALLOWED")
        }
    }

    @Test
    fun shouldRoundTripSchemaAndRuleKinds() {
        schemas.register(
            Schema(
                "Person",
                "1",
                SchemaDsl.obj(
                    "Person",
                    "Person payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Person name"))),
                ),
                usage = SchemaUsage.ENTITY,
            ),
        )
        rules.register(
            AllowedEdgeRule(
                "Person",
                "knows",
                "Person",
                PropertiesPolicy.NONE,
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
            .isEqualTo(org.poc.objs.api.domain.EdgeCardinality.UNSPECIFIED)
    }

    @Test
    fun shouldApplyCustomKind_usingRegisteredHandlerOrder() {
        val notes = mutableListOf<String>()
        val custom = object : SeedDocumentHandler {
            override val kind: String = "Note"
            override val applyOrder: Int = 5
            override fun parse(document: SeedRawDocument) = ParsedSeedDocument(
                document,
                requireText(document.raw, "name", document.index),
                document.raw["name"].toString(),
            )
            override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
                notes += parsed.identity!!
                return SeedDocumentResult(
                    index = parsed.document.index,
                    kind = kind,
                    apiVersion = parsed.document.apiVersion,
                    identity = parsed.identity,
                    applied = true,
                )
            }
        }
        val withCustom = SeedImporter(listOf(objectHandler, ruleHandler, custom))
        val result = withCustom.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Note
            name: hello
            ---
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Person
            version: "1"
            contentSchema:
              type: OBJECT
              title: Person
              description: Person payload
              fields: []
            """.trimIndent(),
        )
        assertThat(result.isSuccess).isTrue()
        assertThat(result.appliedByKind()["Note"]).isEqualTo(1)
        assertThat(schemas.get("Person", "1")).isNotNull
        assertThat(notes).containsExactly("hello")
    }

    @Test
    fun shouldRejectDuplicateHandlerKinds() {
        val dup = object : SeedDocumentHandler {
            override val kind: String = SEED_KIND_OBJECT_SCHEMA
            override fun parse(document: SeedRawDocument): ParsedSeedDocument = error("unused")
            override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult = error("unused")
        }
        assertThatThrownBy {
            SeedImporter(listOf(objectHandler, dup))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining(SEED_KIND_OBJECT_SCHEMA)
    }
}
