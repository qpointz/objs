package org.poc.objs.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.isNull
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.poc.objs.api.domain.Entity
import org.poc.objs.core.domain.ResolvedGraph
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.core.domain.GraphException
import org.poc.objs.core.domain.GraphHeader
import org.poc.objs.core.domain.GraphListItem
import org.poc.objs.core.match.Matcher
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.core.validation.ValidationException
import org.poc.objs.core.validation.ValidationIssue
import org.poc.objs.core.validation.ValidationResult
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class ObjsGraphsControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var namedGraphs: NamedGraphStore
    private lateinit var graphStore: GraphStore

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    /**
     * [org.mockito.ArgumentMatchers.eq] always returns `null` at stub-registration time (the
     * matcher itself is recorded on Mockito's thread-local stack as a side effect); fall back to
     * the real [value] so Kotlin's non-null generic return type doesn't trip a platform-type
     * assertion when this is one of several arguments to a call.
     */
    private fun <T> eqObj(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value

    @BeforeEach
    fun setUp() {
        namedGraphs = mock(NamedGraphStore::class.java)
        graphStore = mock(GraphStore::class.java)
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsGraphsController(namedGraphs, graphStore))
            .setMessageConverters(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
            .build()
    }

    @Test
    fun shouldListGraphs() {
        val id = UUID.randomUUID()
        given(namedGraphs.list()).willReturn(listOf(GraphListItem(id, mapOf("p" to "1"), 2, 1)))

        mockMvc.perform(get("/api/v1/objs/graphs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(id.toString()))
            .andExpect(jsonPath("$[0].entityCount").value(2))
    }

    @Test
    fun shouldSearchGraphs_returningItemsEnvelope() {
        val id = UUID.randomUUID()
        given(namedGraphs.search(eqObj("prod"), isNull(), eqObj(15)))
            .willReturn(listOf(GraphHeader(id, mapOf("env" to "prod"))))

        mockMvc.perform(get("/api/v1/objs/graphs/search").param("q", "prod").param("limit", "15"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(id.toString()))
            .andExpect(jsonPath("$.items[0].annotations.env").value("prod"))
            .andExpect(jsonPath("$.items[0].entityCount").doesNotExist())
    }

    @Test
    fun shouldSearchGraphs_returnEmpty_whenNoQAndNoExpr() {
        given(namedGraphs.search(isNull(), isNull(), eqObj(15))).willReturn(emptyList())

        mockMvc.perform(get("/api/v1/objs/graphs/search"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items").isEmpty)
    }

    @Test
    fun shouldSearchGraphs_passExprAndQ() {
        given(namedGraphs.search(eqObj("acme"), eqObj("a.env == 'prod'"), eqObj(10)))
            .willReturn(emptyList())

        mockMvc.perform(
            get("/api/v1/objs/graphs/search")
                .param("q", "acme")
                .param("expr", "a.env == 'prod'")
                .param("limit", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)

        verify(namedGraphs).search(eqObj("acme"), eqObj("a.env == 'prod'"), eqObj(10))
    }

    @Test
    fun shouldCreateGraph_usingGraphFieldName() {
        val id = UUID.randomUUID()
        val entityId = UUID.randomUUID()
        given(namedGraphs.create(anyObj())).willReturn(
            ResolvedGraph(
                id = id,
                annotations = mapOf("env" to "prod"),
                contents = GraphContents(
                    entities = listOf(Entity(id = entityId, type = "Person", schemaVersion = "1")),
                    edges = emptyList(),
                ),
            ),
        )

        mockMvc.perform(
            post("/api/v1/objs/graphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"annotations":{"env":"prod"},"entityIds":["$entityId"]}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.graph.entities[0].id").value(entityId.toString()))
    }

    @Test
    fun shouldGetGraphById() {
        val id = UUID.randomUUID()
        given(namedGraphs.get(id)).willReturn(
            ResolvedGraph(id, emptyMap(), GraphContents(emptyList(), emptyList())),
        )
        mockMvc.perform(get("/api/v1/objs/graphs/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
    }

    @Test
    fun shouldReturn404_whenGraphMissing() {
        val id = UUID.randomUUID()
        given(namedGraphs.get(id)).willReturn(null)
        mockMvc.perform(get("/api/v1/objs/graphs/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldUpdateAnnotations_returningResolvedGraph() {
        val id = UUID.randomUUID()
        given(namedGraphs.updateAnnotations(eqObj(id), eqObj(mapOf("env" to "prod"))))
            .willReturn(
                ResolvedGraph(id, mapOf("env" to "prod"), GraphContents(emptyList(), emptyList())),
            )

        mockMvc.perform(
            put("/api/v1/objs/graphs/$id/annotations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"annotations":{"env":"prod"}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.annotations.env").value("prod"))
    }

    @Test
    fun shouldReturn404_whenUpdatingAnnotationsOnMissingGraph() {
        val id = UUID.randomUUID()
        willThrow(GraphException("GRAPH_NOT_FOUND", "Subgraph not found: $id"))
            .given(namedGraphs).updateAnnotations(eqObj(id), anyObj())

        mockMvc.perform(
            put("/api/v1/objs/graphs/$id/annotations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"annotations":{"env":"prod"}}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("GRAPH_NOT_FOUND"))
    }

    @Test
    fun shouldMutateGraph_withPatch_mergeMode() {
        val id = UUID.randomUUID()
        given(namedGraphs.mutate(anyObj(), anyObj())).willReturn(ValidationResult.ok())
        given(namedGraphs.get(id)).willReturn(
            ResolvedGraph(id, mapOf("env" to "prod"), GraphContents(emptyList(), emptyList())),
        )

        mockMvc.perform(
            patch("/api/v1/objs/graphs/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"entities":{"set":[],"unset":[]},"edges":{"set":[],"unset":[]}}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.annotations.env").value("prod"))
    }

    @Test
    fun shouldMutateGraph_withPut_replaceMode() {
        val id = UUID.randomUUID()
        given(namedGraphs.mutate(anyObj(), anyObj())).willReturn(ValidationResult.ok())
        given(namedGraphs.get(id)).willReturn(
            ResolvedGraph(id, mapOf("env" to "prod"), GraphContents(emptyList(), emptyList())),
        )

        mockMvc.perform(
            put("/api/v1/objs/graphs/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"entities":{"set":[],"unset":[]},"edges":{"set":[],"unset":[]}}""",
                ),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun shouldRejectMutate_whenInvalid() {
        val id = UUID.randomUUID()
        given(namedGraphs.mutate(anyObj(), anyObj())).willReturn(
            ValidationResult.of(ValidationIssue("EDGE_ENDPOINT_NOT_MEMBER", "bad")),
        )

        mockMvc.perform(
            patch("/api/v1/objs/graphs/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entities":{"set":[],"unset":[]},"edges":{"set":[],"unset":[]}}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("EDGE_ENDPOINT_NOT_MEMBER"))
    }

    @Test
    fun shouldReturn404_whenMutatingMissingGraph() {
        val id = UUID.randomUUID()
        willThrow(GraphException("GRAPH_NOT_FOUND", "Graph not found: $id"))
            .given(namedGraphs).mutate(anyObj(), anyObj())

        mockMvc.perform(
            patch("/api/v1/objs/graphs/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entities":{"set":[],"unset":[]},"edges":{"set":[],"unset":[]}}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("GRAPH_NOT_FOUND"))
    }

    @Test
    fun shouldValidateGraphMutation() {
        val id = UUID.randomUUID()
        given(namedGraphs.validateMutate(anyObj(), anyObj())).willReturn(
            ValidationResult.of(ValidationIssue("EDGE_ENDPOINT_NOT_MEMBER", "bad")),
        )

        mockMvc.perform(
            patch("/api/v1/objs/graphs/$id/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entities":{"set":[],"unset":[]},"edges":{"set":[],"unset":[]}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.issues[0].code").value("EDGE_ENDPOINT_NOT_MEMBER"))
    }

    @Test
    fun shouldDeleteGraph() {
        val id = UUID.randomUUID()
        mockMvc.perform(delete("/api/v1/objs/graphs/$id"))
            .andExpect(status().isNoContent)
        verify(namedGraphs).delete(id)
    }

    @Test
    fun shouldReturn404_whenDeletingMissingGraph() {
        val id = UUID.randomUUID()
        willThrow(GraphException("GRAPH_NOT_FOUND", "not found"))
            .given(namedGraphs).delete(id)

        mockMvc.perform(delete("/api/v1/objs/graphs/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldAttachMember() {
        val id = UUID.randomUUID()
        val entityId = UUID.randomUUID()
        given(namedGraphs.get(id)).willReturn(
            ResolvedGraph(id, emptyMap(), GraphContents(listOf(Entity(id = entityId, type = "Person", schemaVersion = "1")), emptyList())),
        )

        mockMvc.perform(post("/api/v1/objs/graphs/$id/members/$entityId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.graph.entities[0].id").value(entityId.toString()))
        verify(namedGraphs).attach(id, entityId)
    }

    @Test
    fun shouldReturn404_whenAttachingMissingEntity() {
        val id = UUID.randomUUID()
        val entityId = UUID.randomUUID()
        willThrow(GraphException("GRAPH_ENTITY_MISSING", "missing"))
            .given(namedGraphs).attach(id, entityId)

        mockMvc.perform(post("/api/v1/objs/graphs/$id/members/$entityId"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun shouldDetachMember() {
        val id = UUID.randomUUID()
        val entityId = UUID.randomUUID()
        mockMvc.perform(delete("/api/v1/objs/graphs/$id/members/$entityId"))
            .andExpect(status().isNoContent)
        verify(namedGraphs).detach(id, entityId)
    }

    @Test
    fun shouldQueryInGraph_withObjExpr() {
        val id = UUID.randomUUID()
        given(graphStore.selectInGraph(eqObj(id), anyObj<Matcher>())).willReturn(
            GraphContents(entities = emptyList(), edges = emptyList()),
        )

        mockMvc.perform(
            post("/api/v1/objs/graphs/$id/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"obj-expr":"type == 'Person'"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.entities").isArray)
    }

    @Test
    fun shouldQueryInGraph_withChainedObjExpr() {
        val id = UUID.randomUUID()
        given(graphStore.selectInGraph(eqObj(id), anyObj<Matcher>())).willReturn(
            GraphContents(entities = emptyList(), edges = emptyList()),
        )

        mockMvc.perform(
            post("/api/v1/objs/graphs/$id/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""[{"obj-expr":"type == 'Person'"},{"obj-expr":"a.env == 'prod'"}]"""),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun shouldReturn404_whenQueryingMissingGraph() {
        val id = UUID.randomUUID()
        given(graphStore.selectInGraph(eqObj(id), anyObj<Matcher>())).willThrow(
            ValidationException(
                "graph",
                ValidationResult.of(ValidationIssue("GRAPH_NOT_FOUND", "not found")),
            ),
        )

        mockMvc.perform(
            post("/api/v1/objs/graphs/$id/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"obj-expr":"type == 'Person'"}"""),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldRejectRetiredKey_onGraphQuery() {
        val id = UUID.randomUUID()
        mockMvc.perform(
            post("/api/v1/objs/graphs/$id/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"anno":{"env":"prod"}}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("MATCHER_DSL_RETIRED_KEY"))
    }

    @Test
    fun shouldQueryGraphs_withGraphExpr() {
        given(graphStore.select(anyObj<Matcher>())).willReturn(
            GraphContents(entities = emptyList(), edges = emptyList()),
        )

        mockMvc.perform(
            post("/api/v1/objs/graphs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"graph-expr":"a.env == 'prod'"}"""),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun shouldRejectQueryGraphs_whenBareObjExpr() {
        given(graphStore.select(anyObj<Matcher>())).willThrow(
            ValidationException(
                "matcher-dsl",
                ValidationResult.of(ValidationIssue("MATCHER_GRAPH_SCOPE_REQUIRED", "no scope")),
            ),
        )

        mockMvc.perform(
            post("/api/v1/objs/graphs/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"obj-expr":"type == 'Person'"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("MATCHER_GRAPH_SCOPE_REQUIRED"))
    }

    @Test
    fun shouldCloneGraph() {
        val sourceId = UUID.randomUUID()
        val cloneId = UUID.randomUUID()
        given(namedGraphs.clone(eqObj(sourceId), anyObj())).willReturn(
            ResolvedGraph(cloneId, mapOf("decisionId" to "D-9"), GraphContents(emptyList(), emptyList())),
        )

        mockMvc.perform(
            post("/api/v1/objs/graphs/$sourceId/clone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"annotations":{"decisionId":"D-9"}}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(cloneId.toString()))
    }
}
