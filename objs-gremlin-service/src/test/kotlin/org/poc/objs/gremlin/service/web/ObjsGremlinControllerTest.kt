package org.poc.objs.gremlin.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.gremlin.core.BoMGremlinEngine
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class ObjsGremlinControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var store: BoMGraphStore

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    private val a = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val b = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val e = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")

    private fun sampleSubgraph(): BoMSubgraph =
        BoMSubgraph(
            entities = listOf(
                BoMEntity(
                    id = a,
                    type = "Component",
                    schemaVersion = "1.0.0",
                    payload = mutableMapOf("name" to "lib"),
                    annotations = mutableMapOf("env" to "test"),
                ),
                BoMEntity(
                    id = b,
                    type = "Component",
                    schemaVersion = "1.0.0",
                    payload = mutableMapOf("name" to "app"),
                    annotations = mutableMapOf("env" to "test"),
                ),
            ),
            edges = listOf(
                BoMEdge(id = e, source = a, target = b, role = "DEPENDS_ON"),
            ),
        )

    @BeforeEach
    fun setUp() {
        store = mock(BoMGraphStore::class.java)
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsGremlinController(store, BoMGremlinEngine()))
            .setMessageConverters(
                JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()),
            )
            .build()
    }

    @Test
    fun shouldTraverse_whenAnnoMatcherAndScript() {
        given(store.selectSubgraph(anyObj<BoMMatcher>())).willReturn(sampleSubgraph())

        mockMvc.perform(
            post("/api/v1/objs/graph/traverse/gremlin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "anno": { "env": "test" } },
                      "script": "g.V().hasLabel('Component')"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.primary").value("graph"))
            .andExpect(jsonPath("$.subgraph.entities").isArray)
            .andExpect(jsonPath("$.meta.language").value("gremlin-lang"))

        verify(store).selectSubgraph(anyObj<BoMMatcher>())
    }

    @Test
    fun shouldReturn400_whenBlankScript() {
        mockMvc.perform(
            post("/api/v1/objs/graph/traverse/gremlin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "anno": { "env": "test" } },
                      "script": "   "
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun shouldReturn400_whenBadMatcher() {
        mockMvc.perform(
            post("/api/v1/objs/graph/traverse/gremlin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "unknown-key": {} },
                      "script": "g.V()"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues").isArray)
    }

    @Test
    fun shouldReturnScalar_whenCount() {
        given(store.selectSubgraph(anyObj<BoMMatcher>())).willReturn(sampleSubgraph())

        mockMvc.perform(
            post("/api/v1/objs/graph/traverse/gremlin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "anno": { "env": "test" } },
                      "script": "g.V().count()"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.primary").value("scalar"))
            .andExpect(jsonPath("$.views.scalar").value(2))
    }

    @Test
    fun shouldAcceptChainedMatcher() {
        given(store.selectSubgraph(anyObj<BoMMatcher>())).willReturn(sampleSubgraph())

        mockMvc.perform(
            post("/api/v1/objs/graph/traverse/gremlin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": [
                        { "anno": { "env": "test" } },
                        { "anno-expr": "annotations['env'] == 'test'" }
                      ],
                      "script": "g.E().count()"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.primary").value("scalar"))
            .andExpect(jsonPath("$.views.scalar").value(1))
    }
}
