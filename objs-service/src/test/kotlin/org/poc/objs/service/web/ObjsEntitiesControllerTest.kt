package org.poc.objs.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.poc.objs.api.domain.Entity
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.validation.ValidationIssue
import org.poc.objs.core.validation.ValidationResult
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
import java.util.UUID

class ObjsEntitiesControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var store: GraphStore

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    @BeforeEach
    fun setUp() {
        store = mock(GraphStore::class.java)
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsEntitiesController(store))
            .setMessageConverters(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
            .build()
    }

    @Test
    fun shouldListPoolEntities() {
        val id = UUID.randomUUID()
        given(store.listEntities()).willReturn(
            listOf(Entity(id = id, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A"))),
        )
        mockMvc.perform(get("/api/v1/objs/entities"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(id.toString()))
    }

    @Test
    fun shouldQueryPool_withObjExpr() {
        val id = UUID.randomUUID()
        given(store.selectFromPool(anyObj())).willReturn(
            org.poc.objs.api.domain.GraphContents(
                entities = listOf(Entity(id = id, type = "Person", schemaVersion = "1")),
                edges = emptyList(),
            ),
        )

        mockMvc.perform(
            post("/api/v1/objs/entities/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"obj-expr":"type == 'Person'"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.entities[0].id").value(id.toString()))
            .andExpect(jsonPath("$.edges").isEmpty)

        verify(store).selectFromPool(anyObj())
    }

    @Test
    fun shouldCreateEntity_inPoolOnly() {
        given(store.write(anyObj())).willReturn(ValidationResult.ok())

        mockMvc.perform(
            post("/api/v1/objs/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"Person","schemaVersion":"1","payload":{"name":"Ada"}}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("Person"))

        verify(store).write(anyObj())
    }

    @Test
    fun shouldRejectCreate_whenInvalid() {
        given(store.write(anyObj())).willReturn(
            ValidationResult.of(ValidationIssue("SCHEMA_VIOLATION", "bad")),
        )

        mockMvc.perform(
            post("/api/v1/objs/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"Person","schemaVersion":"1","payload":{}}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("SCHEMA_VIOLATION"))
    }

    @Test
    fun shouldGetById() {
        val id = UUID.randomUUID()
        given(store.getEntity(id)).willReturn(
            Entity(id = id, type = "Person", schemaVersion = "1"),
        )
        mockMvc.perform(get("/api/v1/objs/entities/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
    }

    @Test
    fun shouldReturn404_whenMissing() {
        val id = UUID.randomUUID()
        given(store.getEntity(id)).willReturn(null)
        mockMvc.perform(get("/api/v1/objs/entities/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldUpdateEntity() {
        val id = UUID.randomUUID()
        given(store.getEntity(id)).willReturn(Entity(id = id, type = "Person", schemaVersion = "1"))
        given(store.write(anyObj())).willReturn(ValidationResult.ok())

        mockMvc.perform(
            put("/api/v1/objs/entities/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"Person","schemaVersion":"1","payload":{"name":"Updated"}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.payload.name").value("Updated"))
    }

    @Test
    fun shouldReturn404_whenUpdatingMissing() {
        val id = UUID.randomUUID()
        given(store.getEntity(id)).willReturn(null)

        mockMvc.perform(
            put("/api/v1/objs/entities/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"Person","schemaVersion":"1","payload":{}}"""),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldDeleteEntity() {
        val id = UUID.randomUUID()
        given(store.deleteEntity(id)).willReturn(ValidationResult.ok())

        mockMvc.perform(delete("/api/v1/objs/entities/$id"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun shouldReturn404_whenDeletingMissing() {
        val id = UUID.randomUUID()
        given(store.deleteEntity(id)).willReturn(
            ValidationResult.of(ValidationIssue("ENTITY_NOT_FOUND", "missing")),
        )

        mockMvc.perform(delete("/api/v1/objs/entities/$id"))
            .andExpect(status().isNotFound)
    }
}
