package org.poc.objs.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.mock
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMResolvedSubgraph
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.domain.BoMSubgraphException
import org.poc.objs.core.domain.BoMSubgraphListItem
import org.poc.objs.core.persistence.BoMSubgraphStore
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

class ObjsSubgraphControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var store: BoMSubgraphStore

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    @BeforeEach
    fun setUp() {
        store = mock(BoMSubgraphStore::class.java)
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsSubgraphController(store))
            .setMessageConverters(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
            .build()
    }

    @Test
    fun shouldListSubgraphs() {
        val id = UUID.randomUUID()
        given(store.list()).willReturn(
            listOf(BoMSubgraphListItem(id, mapOf("p" to "1"), 2, 1)),
        )
        mockMvc.perform(get("/api/v1/objs/graph/subgraphs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(id.toString()))
            .andExpect(jsonPath("$[0].entityCount").value(2))
    }

    @Test
    fun shouldCreateSubgraph() {
        val id = UUID.randomUUID()
        val entityId = UUID.randomUUID()
        given(store.create(anyObj())).willReturn(
            BoMResolvedSubgraph(
                id = id,
                annotations = mapOf("p" to "1"),
                subgraph = BoMSubgraph(
                    entities = listOf(
                        BoMEntity(id = entityId, type = "Person", schemaVersion = "1"),
                    ),
                    edges = emptyList(),
                ),
            ),
        )
        mockMvc.perform(
            post("/api/v1/objs/graph/subgraphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"annotations":{"p":"1"},"entityIds":["$entityId"],"edgeIds":[]}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.subgraph.entities[0].id").value(entityId.toString()))
    }

    @Test
    fun shouldGetById() {
        val id = UUID.randomUUID()
        given(store.get(id)).willReturn(
            BoMResolvedSubgraph(id, emptyMap(), BoMSubgraph(emptyList(), emptyList())),
        )
        mockMvc.perform(get("/api/v1/objs/graph/subgraphs/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
    }

    @Test
    fun shouldReturn404_whenMissing() {
        val id = UUID.randomUUID()
        given(store.get(id)).willReturn(null)
        mockMvc.perform(get("/api/v1/objs/graph/subgraphs/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldReplace() {
        val id = UUID.randomUUID()
        given(store.replace(anyObj(), anyObj())).willReturn(
            BoMResolvedSubgraph(id, mapOf("x" to "y"), BoMSubgraph(emptyList(), emptyList())),
        )
        mockMvc.perform(
            put("/api/v1/objs/graph/subgraphs/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"annotations":{"x":"y"},"entityIds":[],"edgeIds":[]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.annotations.x").value("y"))
    }

    @Test
    fun shouldDelete() {
        val id = UUID.randomUUID()
        willDoNothing().given(store).delete(id)
        mockMvc.perform(delete("/api/v1/objs/graph/subgraphs/$id"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun shouldReturn400_whenMembershipInvalid() {
        willThrow(
            BoMSubgraphException("SUBGRAPH_EDGE_ENDPOINTS", "bad edge"),
        ).given(store).create(anyObj())
        mockMvc.perform(
            post("/api/v1/objs/graph/subgraphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entityIds":[],"edgeIds":[]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("SUBGRAPH_EDGE_ENDPOINTS"))
    }
}
