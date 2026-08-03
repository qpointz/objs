package org.poc.objs.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper

class ObjsRegistryControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var schemas: InMemoryBoMSchemaCatalog
    private lateinit var edgeRules: InMemoryBoMAllowedEdgeCatalog

    @BeforeEach
    fun setUp() {
        schemas = InMemoryBoMSchemaCatalog()
        edgeRules = InMemoryBoMAllowedEdgeCatalog()
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsRegistryController(schemas, edgeRules))
            .setControllerAdvice(ObjsRegistryExceptionHandler())
            .setMessageConverters(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
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
                      "usages":["ENTITY"]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("Person"))
            .andExpect(jsonPath("$.version").value("1"))
            .andExpect(jsonPath("$.usages[0]").value("ENTITY"))

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
                      "usages":["ENTITY"]
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
                      "usages":["ENTITY"]
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
                usages = setOf(BoMSchemaUsage.ENTITY),
            ),
        )
        schemas.register(
            BoMSchema(
                "LinkProps",
                "1",
                BoMSchemaDsl.obj("Link", "Link properties"),
                usages = setOf(BoMSchemaUsage.EDGE_PROPERTIES),
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
                usages = setOf(BoMSchemaUsage.ENTITY),
            ),
        )
        schemas.register(
            BoMSchema(
                "Organization",
                "1",
                BoMSchemaDsl.obj("Organization", "Organization payload"),
                usages = setOf(BoMSchemaUsage.ENTITY),
            ),
        )
        schemas.register(
            BoMSchema(
                "CanonicalEdge",
                "1",
                BoMSchemaDsl.obj("Canonical edge", "Relationship properties"),
                usages = setOf(BoMSchemaUsage.EDGE_PROPERTIES),
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
                usages = setOf(BoMSchemaUsage.EDGE_PROPERTIES),
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
}
