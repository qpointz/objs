package org.poc.objs.jgrapht.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.match.Matcher
import org.poc.objs.api.store.GraphStore
import org.poc.objs.jgrapht.core.GraphAlgorithmIds
import org.poc.objs.jgrapht.core.analysis.DirectedCycleRegionAnalyzer
import org.poc.objs.jgrapht.service.GraphAlgorithmService
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class ObjsGraphAlgorithmsControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var store: GraphStore

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    private val a = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val b = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val ab = UUID.fromString("00000000-0000-0000-0000-000000000101")
    private val ba = UUID.fromString("00000000-0000-0000-0000-000000000102")

    @BeforeEach
    fun setUp() {
        store = mock(GraphStore::class.java)
        val service = GraphAlgorithmService(
            store = store,
            policy = DefaultGraphFragmentPolicy,
            cycleAnalyzer = DirectedCycleRegionAnalyzer(),
        )
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsGraphAlgorithmsController(service))
            .setMessageConverters(
                JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()),
            )
            .build()
    }

    @Test
    fun shouldExposeCapabilities() {
        mockMvc.perform(get("/api/v1/objs/graph/algorithms/capabilities"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.algorithms[0].id").value(GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS))
            .andExpect(jsonPath("$.algorithms[0].materializationModes[0]").value("GENERIC"))
    }

    @Test
    fun shouldAnalyzeCyclesForMatcherSelectedGraph() {
        given(store.select(anyObj<Matcher>())).willReturn(twoNodeCycleContents())

        mockMvc.perform(
            post("/api/v1/objs/graph/algorithms/cycles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "all": true }
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.algorithm").value(GraphAlgorithmIds.DIRECTED_CYCLE_REGIONS))
            .andExpect(jsonPath("$.components.length()").value(1))
            .andExpect(jsonPath("$.components[0].id").value(a.toString()))
            .andExpect(jsonPath("$.stats.entityCount").value(2))
            .andExpect(jsonPath("$.stats.cyclicComponentCount").value(1))

        verify(store).select(anyObj<Matcher>())
    }

    @Test
    fun shouldAnalyzeInGraphScope() {
        val graphId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        given(store.selectInGraph(anyObj<UUID>(), anyObj<Matcher>())).willReturn(twoNodeCycleContents())

        mockMvc.perform(
            post("/api/v1/objs/graph/algorithms/cycles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "obj-expr": "true" },
                      "graphId": "$graphId"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components.length()").value(1))

        verify(store).selectInGraph(anyObj<UUID>(), anyObj<Matcher>())
    }

    @Test
    fun shouldAnalyzePinnedGraphVersionScope() {
        val graphId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        given(store.selectInGraphVersion(anyObj<UUID>(), anyLong(), anyObj<Matcher>()))
            .willReturn(acyclicContents())

        mockMvc.perform(
            post("/api/v1/objs/graph/algorithms/cycles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "obj-expr": "true" },
                      "graphId": "$graphId",
                      "graphVersion": 3
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components.length()").value(0))
            .andExpect(jsonPath("$.stats.cyclicComponentCount").value(0))

        verify(store).selectInGraphVersion(anyObj<UUID>(), eq(3L), anyObj<Matcher>())
    }

    @Test
    fun shouldReturnNoCyclesForAcyclicGraph() {
        given(store.select(anyObj<Matcher>())).willReturn(acyclicContents())

        mockMvc.perform(
            post("/api/v1/objs/graph/algorithms/cycles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "matcher": { "all": true } }"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components.length()").value(0))
    }

    @Test
    fun shouldDetectSelfLoopCycle() {
        val loop = UUID.fromString("00000000-0000-0000-0000-000000000101")
        given(store.select(anyObj<Matcher>())).willReturn(
            GraphContents(
                entities = listOf(Entity(a, "Component", "1")),
                edges = listOf(Edge(loop, source = a, target = a, role = "depends_on")),
            ),
        )

        mockMvc.perform(
            post("/api/v1/objs/graph/algorithms/cycles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "matcher": { "all": true } }"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components[0].entityIds[0]").value(a.toString()))
            .andExpect(jsonPath("$.components[0].edgeIds[0]").value(loop.toString()))
    }

    @Test
    fun shouldReturn400ForUnsupportedMaterializationMode() {
        given(store.select(anyObj<Matcher>())).willReturn(acyclicContents())

        mockMvc.perform(
            post("/api/v1/objs/graph/algorithms/cycles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "all": true },
                      "materialization": "TYPED"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Materialization mode TYPED is not available"))
    }

    @Test
    fun shouldReturn400ForBadMatcher() {
        mockMvc.perform(
            post("/api/v1/objs/graph/algorithms/cycles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "unknown-key": {} }
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues").isArray)
    }

    @Test
    fun shouldReturn400ForConflictingFragment() {
        given(store.select(anyObj<Matcher>())).willReturn(
            GraphContents(
                entities = listOf(
                    Entity(a, "Component", "1", mutableMapOf("name" to "a")),
                    Entity(a, "Component", "1", mutableMapOf("name" to "b")),
                ),
                edges = emptyList(),
            ),
        )

        mockMvc.perform(
            post("/api/v1/objs/graph/algorithms/cycles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "matcher": { "all": true } }"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
    }

    private fun twoNodeCycleContents(): GraphContents =
        GraphContents(
            entities = listOf(Entity(a, "Component", "1"), Entity(b, "Component", "1")),
            edges = listOf(
                Edge(ab, source = a, target = b, role = "depends_on"),
                Edge(ba, source = b, target = a, role = "depends_on"),
            ),
        )

    private fun acyclicContents(): GraphContents =
        GraphContents(
            entities = listOf(Entity(a, "Component", "1"), Entity(b, "Component", "1")),
            edges = listOf(Edge(ab, source = a, target = b, role = "depends_on")),
        )
}
