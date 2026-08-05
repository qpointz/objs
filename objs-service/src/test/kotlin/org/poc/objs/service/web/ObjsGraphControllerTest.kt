package org.poc.objs.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
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

class ObjsGraphControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var store: BoMGraphStore

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    @BeforeEach
    fun setUp() {
        store = mock(BoMGraphStore::class.java)
        mockMvc = MockMvcBuilders
            .standaloneSetup(
                ObjsGraphController(
                    store,
                    mock(org.poc.objs.core.seed.SeedImporter::class.java),
                    mock(org.poc.objs.core.seed.CanonicalSeedSerializer::class.java),
                ),
                ObjsStatusController(),
            )
            .setMessageConverters(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
            .build()
    }

    @Test
    fun shouldReturnStatus() {
        mockMvc.perform(get("/api/v1/objs/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.state").value("ok"))
    }

    @Test
    fun shouldPutGraph_whenValid() {
        given(store.mutate(anyObj())).willAnswer { inv ->
            val m = inv.getArgument<BoMGraphMutation>(0)
            if (m.upsert.entities.isNotEmpty() && m.upsert.entities[0].id == null) {
                m.upsert.entities[0].id = UUID.randomUUID()
            }
            BoMValidationResult.ok()
        }

        mockMvc.perform(
            put("/api/v1/objs/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"upsert":{"entities":[{"type":"Person","schemaVersion":"1","payload":{"name":"A"}}],"edges":[]},"delete":{"entities":[],"edges":[]}}""",
                ),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun shouldPutGraphMutation_withDeletes() {
        val deleteId = UUID.randomUUID()
        given(store.mutate(anyObj())).willReturn(BoMValidationResult.ok())

        mockMvc.perform(
            put("/api/v1/objs/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "upsert":{"entities":[],"edges":[]},
                      "delete":{"entities":["$deleteId"],"edges":[]}
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        verify(store).mutate(anyObj())
    }

    @Test
    fun shouldRejectPutGraph_whenInvalid() {
        given(store.mutate(anyObj())).willReturn(
            BoMValidationResult.of(BoMValidationIssue("SCHEMA_VIOLATION", "bad")),
        )

        mockMvc.perform(
            put("/api/v1/objs/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"upsert":{"entities":[{"type":"Person","schemaVersion":"1","payload":{}}],"edges":[]},"delete":{"entities":[],"edges":[]}}""",
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("SCHEMA_VIOLATION"))
    }

    @Test
    fun shouldValidateGraph_returning200() {
        given(store.validateMutation(anyObj())).willReturn(
            BoMValidationResult.of(BoMValidationIssue("SCHEMA_VIOLATION", "bad")),
        )

        mockMvc.perform(
            post("/api/v1/objs/graph/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"upsert":{"entities":[],"edges":[]},"delete":{"entities":[],"edges":[]}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.issues[0].code").value("SCHEMA_VIOLATION"))
    }

    @Test
    fun shouldQuerySubgraph_withAnnoJson() {
        given(store.selectSubgraph(anyObj<BoMMatcher>())).willReturn(
            BoMSubgraph(entities = emptyList(), edges = emptyList()),
        )

        mockMvc.perform(
            post("/api/v1/objs/graph/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"anno":{"env":"prod"}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.entities").isArray)

        verify(store).selectSubgraph(anyObj<BoMMatcher>())
    }

    @Test
    fun shouldQuerySubgraph_withYamlBody() {
        given(store.selectSubgraph(anyObj<BoMMatcher>())).willReturn(
            BoMSubgraph(entities = emptyList(), edges = emptyList()),
        )

        mockMvc.perform(
            post("/api/v1/objs/graph/query")
                .contentType(MediaType.parseMediaType("application/yaml"))
                .content(
                    """
                    anno:
                      env: prod
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun shouldRejectQuery_whenMatcherEmpty() {
        mockMvc.perform(
            post("/api/v1/objs/graph/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("MATCHER_DSL_EMPTY"))
    }

    @Test
    fun shouldDeleteGraph_returning204() {
        val id = UUID.randomUUID()
        given(store.delete(anyList(), anyList())).willReturn(BoMValidationResult.ok())

        mockMvc.perform(
            delete("/api/v1/objs/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entityIds":["$id"],"edgeIds":[]}"""),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun shouldDeleteGraph_returning404_whenMissing() {
        val id = UUID.randomUUID()
        given(store.delete(anyList(), anyList())).willReturn(
            BoMValidationResult.of(BoMValidationIssue("ENTITY_NOT_FOUND", "missing")),
        )

        mockMvc.perform(
            delete("/api/v1/objs/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entityIds":["$id"]}"""),
        )
            .andExpect(status().isNotFound)
    }
}
