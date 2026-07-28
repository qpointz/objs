package org.poc.objs.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper

class ObjsRegistryControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var schemas: BoMSchemaCatalog
    private lateinit var edgeRules: BoMAllowedEdgeCatalog

    @BeforeEach
    fun setUp() {
        schemas = BoMSchemaCatalog()
        edgeRules = BoMAllowedEdgeCatalog()
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsRegistryController(schemas, edgeRules))
            .setMessageConverters(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
            .build()
    }

    @Test
    fun shouldUpsertAndGetSchema() {
        mockMvc.perform(
            put("/api/v1/objs/registry/schemas/Person/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"object","properties":{"name":{"type":"string"}}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("Person"))
            .andExpect(jsonPath("$.version").value("1"))

        mockMvc.perform(get("/api/v1/objs/registry/schemas/Person/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.schema.type").value("object"))

        mockMvc.perform(get("/api/v1/objs/registry/types"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("Person"))
    }

    @Test
    fun shouldRejectEmptySchemaBody() {
        mockMvc.perform(
            put("/api/v1/objs/registry/schemas/Person/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("SCHEMA_BODY_EMPTY"))
    }

    @Test
    fun shouldReturn404_whenSchemaMissing() {
        mockMvc.perform(get("/api/v1/objs/registry/schemas/Person/9"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldDeleteSchema() {
        schemas.register(BoMSchema("Person", "1", mapOf("type" to "object")))
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
                    """{"sourceType":"*","role":"depends_on","targetType":"*","propertiesPolicy":"NONE"}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("depends_on"))

        mockMvc.perform(get("/api/v1/objs/registry/edges"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].propertiesPolicy").value("NONE"))

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
        schemas.register(BoMSchema("Person", "1", mapOf("type" to "object")))
        schemas.register(BoMSchema("Person", "2", mapOf("type" to "object")))
        mockMvc.perform(get("/api/v1/objs/registry/schemas/Person"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }
}
