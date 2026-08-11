package org.poc.objs.service.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMEdgeCardinality
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.domain.FullCatalogJsonSchemaExporter
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.seed.AllowedEdgeRuleSeedHandler
import org.poc.objs.core.seed.CanonicalSeedSerializer
import org.poc.objs.core.seed.GraphSeedHandler
import org.poc.objs.core.seed.ObjectSchemaSeedHandler
import org.poc.objs.core.seed.SeedImporter
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets

class ObjsRegistryControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var schemas: InMemoryBoMSchemaCatalog
    private lateinit var edgeRules: InMemoryBoMAllowedEdgeCatalog

    @BeforeEach
    fun setUp() {
        schemas = InMemoryBoMSchemaCatalog()
        edgeRules = InMemoryBoMAllowedEdgeCatalog()
        val objectHandler = ObjectSchemaSeedHandler(schemas)
        val ruleHandler = AllowedEdgeRuleSeedHandler(edgeRules)
        val importer = SeedImporter(listOf(objectHandler, ruleHandler))
        val serializer = CanonicalSeedSerializer(
            schemas,
            edgeRules,
            objectHandler,
            ruleHandler,
            GraphSeedHandler(mock(org.poc.objs.core.persistence.BoMNamedGraphStore::class.java)),
        )
        mockMvc = MockMvcBuilders
            .standaloneSetup(
                ObjsRegistryController(
                    schemas,
                    edgeRules,
                    importer,
                    serializer,
                    FullCatalogJsonSchemaExporter(schemas, edgeRules),
                ),
            )
            .setControllerAdvice(ObjsRegistryExceptionHandler())
            .setMessageConverters(
                JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()),
                StringHttpMessageConverter(StandardCharsets.UTF_8),
            )
            .build()
    }

    @Test
    fun shouldUpsertAndGetSchema() {
        mockMvc.perform(
            put("/api/v1/objs/registry/schemas/Person/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "contentSchema":{
                        "type":"OBJECT",
                        "title":"Person",
                        "description":"Person payload",
                        "fields":[{
                          "name":"name",
                          "schema":{"type":"STRING","title":"Name","description":"Person name"},
                          "required":true
                        }]
                      },
                      "usage":"ENTITY"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("Person"))
            .andExpect(jsonPath("$.version").value("1"))
            .andExpect(jsonPath("$.usage").value("ENTITY"))

        mockMvc.perform(get("/api/v1/objs/registry/schemas/Person/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contentSchema.type").value("OBJECT"))

        mockMvc.perform(get("/api/v1/objs/registry/schemas/Person/1/json-schema"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("object"))
            .andExpect(jsonPath("$.required[0]").value("name"))

        mockMvc.perform(get("/api/v1/objs/registry/types"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("Person"))
    }

    @Test
    fun shouldRejectInvalidSchemaDefinition() {
        mockMvc.perform(
            put("/api/v1/objs/registry/schemas/Person/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "contentSchema":{
                        "type":"OBJECT",
                        "title":"",
                        "description":"Person payload",
                        "fields":[]
                      }
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("SCHEMA_DEFINITION_INVALID"))
    }

    @Test
    fun shouldLintSchemaWithoutRegistering() {
        mockMvc.perform(
            post("/api/v1/objs/registry/schemas/Person/1/lint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "contentSchema":{
                        "type":"OBJECT",
                        "title":"Person",
                        "description":"Person payload",
                        "fields":[]
                      },
                      "usage":"ENTITY"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.schema.type").value("Person"))
            .andExpect(jsonPath("$.jsonSchema.type").value("object"))

        mockMvc.perform(get("/api/v1/objs/registry/schemas/Person/1"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldReturnLintIssue_whenSchemaInvalid() {
        mockMvc.perform(
            post("/api/v1/objs/registry/schemas/Person/1/lint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "contentSchema":{
                        "type":"OBJECT",
                        "title":"",
                        "description":"Person payload",
                        "fields":[]
                      }
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.issues[0].code").value("SCHEMA_DEFINITION_INVALID"))
    }

    @Test
    fun shouldCreateNextMajorVersion() {
        schemas.register(BoMSchema("Person", "1", BoMSchemaDsl.obj("Person", "Person payload")))
        schemas.register(BoMSchema("Person", "4", BoMSchemaDsl.obj("Person", "Person payload")))

        mockMvc.perform(
            post("/api/v1/objs/registry/schemas/Person/versions/next-major")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "contentSchema":{
                        "type":"OBJECT",
                        "title":"Person",
                        "description":"Person payload v5",
                        "fields":[]
                      },
                      "usage":"ENTITY"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.version").value("5"))
            .andExpect(jsonPath("$.contentSchema.description").value("Person payload v5"))
    }

    @Test
    fun shouldCreateNextMajorAsSemver_whenExistingAreDotted() {
        schemas.register(BoMSchema("Person", "1.0.0", BoMSchemaDsl.obj("Person", "Person payload")))
        schemas.register(BoMSchema("Person", "4.2.1", BoMSchemaDsl.obj("Person", "Person payload")))

        mockMvc.perform(
            post("/api/v1/objs/registry/schemas/Person/versions/next-major")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "contentSchema":{
                        "type":"OBJECT",
                        "title":"Person",
                        "description":"Person payload",
                        "fields":[]
                      }
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.version").value("5.0.0"))
    }

    @Test
    fun shouldFilterSchemasByUsage() {
        schemas.register(
            BoMSchema(
                "Person",
                "1",
                BoMSchemaDsl.obj("Person", "Person payload"),
                usage = BoMSchemaUsage.ENTITY,
            ),
        )
        schemas.register(
            BoMSchema(
                "LinkProps",
                "1",
                BoMSchemaDsl.obj("Link", "Link properties"),
                usage = BoMSchemaUsage.EDGE_PROPERTIES,
            ),
        )

        mockMvc.perform(get("/api/v1/objs/registry/schemas").param("usage", "ENTITY"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("Person"))

        mockMvc.perform(get("/api/v1/objs/registry/types").param("usage", "EDGE_PROPERTIES"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("LinkProps"))
    }

    @Test
    fun shouldListIncomingAndOutgoingEdgesIncludingWildcards() {
        edgeRules.register(BoMAllowedEdgeRule("Person", "knows", "Person", BoMPropertiesPolicy.NONE))
        edgeRules.register(BoMAllowedEdgeRule("*", "depends_on", "Component", BoMPropertiesPolicy.NONE))
        edgeRules.register(BoMAllowedEdgeRule("Product", "CONTAINS", "*", BoMPropertiesPolicy.NONE))

        mockMvc.perform(get("/api/v1/objs/registry/types/Person/edges"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.outgoing.length()").value(2))
            .andExpect(jsonPath("$.incoming.length()").value(2))
            .andExpect(jsonPath("$.outgoing[?(@.role=='knows')]").exists())
            .andExpect(jsonPath("$.outgoing[?(@.role=='depends_on')]").exists())

        mockMvc.perform(get("/api/v1/objs/registry/types/Component/edges"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.incoming.length()").value(2))
    }

    @Test
    fun shouldReplaceRelationsForEdgePropertySchema() {
        schemas.register(
            BoMSchema(
                "Person",
                "1",
                BoMSchemaDsl.obj("Person", "Person payload"),
                usage = BoMSchemaUsage.ENTITY,
            ),
        )
        schemas.register(
            BoMSchema(
                "Organization",
                "1",
                BoMSchemaDsl.obj("Organization", "Organization payload"),
                usage = BoMSchemaUsage.ENTITY,
            ),
        )
        schemas.register(
            BoMSchema(
                "CanonicalEdge",
                "1",
                BoMSchemaDsl.obj("Canonical edge", "Relationship properties"),
                usage = BoMSchemaUsage.EDGE_PROPERTIES,
            ),
        )

        mockMvc.perform(
            put("/api/v1/objs/registry/schemas/CanonicalEdge/1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    [{
                      "sourceType":"Person",
                      "role":"MEMBER_OF",
                      "targetType":"Organization",
                      "emptyPropertiesAllowed":false,
                      "cardinality":"1:1"
                    }]
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].propertiesPolicy").value("SCHEMA"))
            .andExpect(jsonPath("$[0].propertiesSchemaType").value("CanonicalEdge"))
            .andExpect(jsonPath("$[0].propertiesSchemaVersion").value("1"))
            .andExpect(jsonPath("$[0].cardinality").value("1:1"))

        mockMvc.perform(get("/api/v1/objs/registry/schemas/CanonicalEdge/1/edges"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].sourceType").value("Person"))
            .andExpect(jsonPath("$[0].role").value("MEMBER_OF"))
            .andExpect(jsonPath("$[0].targetType").value("Organization"))
            .andExpect(jsonPath("$[0].cardinality").value("1:1"))

        mockMvc.perform(
            put("/api/v1/objs/registry/schemas/CanonicalEdge/1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        mockMvc.perform(get("/api/v1/objs/registry/schemas/CanonicalEdge/1/edges"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun shouldRejectUnknownEntityTypeInEdgeRelation() {
        schemas.register(
            BoMSchema(
                "CanonicalEdge",
                "1",
                BoMSchemaDsl.obj("Canonical edge", "Relationship properties"),
                usage = BoMSchemaUsage.EDGE_PROPERTIES,
            ),
        )

        mockMvc.perform(
            put("/api/v1/objs/registry/schemas/CanonicalEdge/1/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """[{"sourceType":"Missing","role":"LINKS","targetType":"*"}]""",
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("EDGE_SOURCE_SCHEMA_NOT_FOUND"))
    }

    @Test
    fun shouldReturnConsistentValidationError_whenSchemaBodyCannotBind() {
        mockMvc.perform(
            put("/api/v1/objs/registry/schemas/Person/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"contentSchema":{"type":"NOT_A_TYPE","title":"X","description":"Y","fields":[]}}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("SCHEMA_REQUEST_INVALID"))
    }

    @Test
    fun shouldReturn404_whenSchemaMissing() {
        mockMvc.perform(get("/api/v1/objs/registry/schemas/Person/9"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldDeleteSchema() {
        schemas.register(BoMSchema("Person", "1", BoMSchemaDsl.obj("Person", "Person payload")))
        mockMvc.perform(delete("/api/v1/objs/registry/schemas/Person/1"))
            .andExpect(status().isNoContent)
        mockMvc.perform(delete("/api/v1/objs/registry/schemas/Person/1"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldDeleteSchemaType_andIncidentEdges() {
        schemas.register(BoMSchema("Person", "1", BoMSchemaDsl.obj("Person", "Person payload")))
        schemas.register(BoMSchema("Person", "2", BoMSchemaDsl.obj("Person", "Person payload v2")))
        schemas.register(BoMSchema("Org", "1", BoMSchemaDsl.obj("Org", "Org payload")))
        edgeRules.register(
            BoMAllowedEdgeRule(
                sourceType = "Person",
                role = "works_for",
                targetType = "Org",
            ),
        )
        edgeRules.register(
            BoMAllowedEdgeRule(
                sourceType = "Org",
                role = "employs",
                targetType = "Person",
            ),
        )
        edgeRules.register(
            BoMAllowedEdgeRule(
                sourceType = "Org",
                role = "owns",
                targetType = "Org",
            ),
        )

        mockMvc.perform(delete("/api/v1/objs/registry/schemas/Person"))
            .andExpect(status().isNoContent)

        assertThat(schemas.listByType("Person")).isEmpty()
        assertThat(schemas.listByType("Org")).hasSize(1)
        assertThat(edgeRules.all().map { it.role }).containsExactly("owns")

        mockMvc.perform(delete("/api/v1/objs/registry/schemas/Person"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldUpsertListAndDeleteEdge() {
        mockMvc.perform(
            put("/api/v1/objs/registry/edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"sourceType":"*","role":"depends_on","targetType":"*","propertiesPolicy":"NONE","cardinality":"1:*"}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("depends_on"))
            .andExpect(jsonPath("$.cardinality").value("1:*"))

        mockMvc.perform(get("/api/v1/objs/registry/edges"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].propertiesPolicy").value("NONE"))
            .andExpect(jsonPath("$[0].cardinality").value("1:*"))

        mockMvc.perform(
            delete("/api/v1/objs/registry/edges")
                .param("sourceType", "*")
                .param("role", "depends_on")
                .param("targetType", "*"),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            delete("/api/v1/objs/registry/edges")
                .param("sourceType", "*")
                .param("role", "depends_on")
                .param("targetType", "*"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldListSchemasByType() {
        schemas.register(BoMSchema("Person", "1", BoMSchemaDsl.obj("Person", "Person payload")))
        schemas.register(BoMSchema("Person", "2", BoMSchemaDsl.obj("Person", "Person payload")))
        mockMvc.perform(get("/api/v1/objs/registry/schemas/Person"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun shouldExportCatalogSeeds() {
        schemas.register(BoMSchema("Person", "1", BoMSchemaDsl.obj("Person", "Person payload")))
        mockMvc.perform(get("/api/v1/objs/registry/export").param("format", "seeds"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/yaml")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("ObjectSchema")))
    }

    @Test
    fun shouldRejectUnknownExportFormat() {
        mockMvc.perform(get("/api/v1/objs/registry/export").param("format", "protobuf"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("IO_FORMAT_UNSUPPORTED"))
    }

    @Test
    fun shouldImportCatalogSeeds_andRejectGraphKind() {
        val catalog = """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Person
            version: "1"
            contentSchema:
              type: OBJECT
              title: Person
              description: Person payload
              fields: []
        """.trimIndent()
        val file = MockMultipartFile(
            "file",
            "seed.yaml",
            "application/yaml",
            catalog.toByteArray(),
        )
        mockMvc.perform(multipart("/api/v1/objs/registry/import").param("format", "seeds").file(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.documents[0].applied").value(true))

        val graph = """
            apiVersion: objs.poc.org/v1
            kind: Graph
            name: demo
            entities: []
            edges: []
        """.trimIndent()
        val graphFile = MockMultipartFile("file", "g.yaml", "application/yaml", graph.toByteArray())
        mockMvc.perform(multipart("/api/v1/objs/registry/import").param("format", "seeds").file(graphFile))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.documents[0].errors[0].code").value("SEED_KIND_NOT_ALLOWED"))
    }

    @Test
    fun shouldExportFullCatalogJsonSchema() {
        schemas.register(BoMSchema("Person", "1", BoMSchemaDsl.obj("Person", "Person payload")))
        mockMvc.perform(get("/api/v1/objs/registry/export").param("format", "json-schema"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/schema+json")))
            .andExpect(jsonPath("$.['x-objs-export']").value("full-catalog"))
            .andExpect(jsonPath("$.['x-objs-json-schema-options'].includeEdges").value("outbound"))
            .andExpect(jsonPath("$.['\$defs'].Person.type").value("object"))
    }

    @Test
    fun shouldExportLinkedJsonSchema_whenIncludeEdgesLinked() {
        schemas.register(BoMSchema("Database", "1", BoMSchemaDsl.obj("Database", "Database payload")))
        schemas.register(BoMSchema("Dataset", "1", BoMSchemaDsl.obj("Dataset", "Dataset payload")))
        edgeRules.register(
            BoMAllowedEdgeRule(
                sourceType = "Database",
                role = "CONTAINS",
                targetType = "Dataset",
                cardinality = BoMEdgeCardinality.ONE_TO_MANY,
            ),
        )
        mockMvc.perform(
            get("/api/v1/objs/registry/export")
                .param("format", "json-schema")
                .param("includeEdges", "linked"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.['x-objs-json-schema-options'].includeEdges").value("linked"))
            .andExpect(jsonPath("$.['\$defs'].Database.properties.containsDataset.type").value("array"))
            .andExpect(
                jsonPath("$.['\$defs'].Dataset.properties.containsFromDatabase['\$ref']")
                    .value("#/\$defs/Database"),
            )
    }

    @Test
    fun shouldRejectUnknownJsonSchemaOptions() {
        mockMvc.perform(
            get("/api/v1/objs/registry/export")
                .param("format", "json-schema")
                .param("includeEdges", "both"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("JSON_SCHEMA_OPTIONS_INVALID"))
    }
}
