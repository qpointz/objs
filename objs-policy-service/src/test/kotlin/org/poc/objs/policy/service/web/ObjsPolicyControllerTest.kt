package org.poc.objs.policy.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.match.Matcher
import org.poc.objs.api.store.GraphStore
import org.poc.objs.policy.api.ApplicabilityKinds
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.core.DefaultPolicyEvaluator
import org.poc.objs.policy.core.InMemoryPolicyStores
import org.poc.objs.policy.drools.DroolsPolicyEngine
import org.poc.objs.policy.drools.PolicyKnowledgeBaseCache
import org.poc.objs.policy.service.PolicyPlayService
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

class ObjsPolicyControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var store: GraphStore
    private lateinit var stores: InMemoryPolicyStores
    private lateinit var categoryId: UUID

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    private val validDrl =
        """
        package org.poc.objs.policy.fixture
        import org.poc.objs.policy.drools.DroolsEvaluationScratch;
        global DroolsEvaluationScratch scratch;
        rule "always-pass"
        when
        then
            // keep PASS
        end
        """.trimIndent()

    @BeforeEach
    fun setUp() {
        store = mock(GraphStore::class.java)
        stores = InMemoryPolicyStores()
        categoryId = stores.categories.save(CategoryWrite("General", "general")).id
        mockMvc = buildMockMvc()
    }

    private fun buildMockMvc(
        evaluationArchive: org.poc.objs.policy.api.EvaluationArchive? = null,
    ): MockMvc {
        val repo = stores.policies
        val cache = PolicyKnowledgeBaseCache()
        val drools = DroolsPolicyEngine(cache)
        val evaluator = DefaultPolicyEvaluator(
            repository = repo,
            fragmentPolicy = DefaultGraphFragmentPolicy,
            engines = mapOf(PolicyEngineKinds.DROOLS to drools),
        )
        val play = PolicyPlayService(
            store = store,
            fragmentPolicy = DefaultGraphFragmentPolicy,
            repository = repo,
            categories = stores.categories,
            suites = stores.suites,
            evaluator = evaluator,
            suiteEvaluator = org.poc.objs.policy.core.DefaultSuiteEvaluator(
                evaluator,
                repo,
                stores.categories,
            ),
            knowledgeBaseCache = cache,
            catalogExporter = org.poc.objs.policy.service.seed.PolicyCatalogSeedExporter(
                stores.categories,
                repo,
                stores.suites,
            ),
            evaluationArchive = java.util.Optional.ofNullable(evaluationArchive),
        )
        return MockMvcBuilders
            .standaloneSetup(ObjsPolicyController(play))
            .setMessageConverters(
                JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()),
            )
            .build()
    }

    @Test
    fun shouldCrudCategories() {
        mockMvc.perform(
            post("/api/v1/objs/policy/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Licensing","key":"licensing"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.key").value("licensing"))

        mockMvc.perform(get("/api/v1/objs/policy/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.key=='licensing')]").isNotEmpty)
    }

    @Test
    fun shouldExposeCapabilities() {
        mockMvc.perform(get("/api/v1/objs/policy/capabilities"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.engines[0]").value("DROOLS"))
            .andExpect(jsonPath("$.operations[0]").value("list"))
    }

    @Test
    fun shouldCrudPolicies() {
        val created = mockMvc.perform(
            post("/api/v1/objs/policy/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "key": "demo",
                      "name": "demo",
                      "engineKind": "DROOLS",
                      "body": "package x\n",
                      "categoryId": "$categoryId",
                      "tags": ["demo"],
                      "version": "1.0"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("demo"))
            .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
            .andReturn()

        val id = JsonMapper.builder().findAndAddModules().build()
            .readTree(created.response.contentAsString)
            .get("id").asText()

        mockMvc.perform(get("/api/v1/objs/policy/policies"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(id))

        mockMvc.perform(
            put("/api/v1/objs/policy/policies/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "key": "demo",
                      "name": "demo",
                      "engineKind": "DROOLS",
                      "body": "package updated\n",
                      "categoryId": "$categoryId",
                      "tags": ["demo"],
                      "version": "1.0"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.body").value("package updated\n"))

        mockMvc.perform(delete("/api/v1/objs/policy/policies/$id"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/v1/objs/policy/policies/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldCheckInvalidDrls() {
        mockMvc.perform(
            post("/api/v1/objs/policy/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"this is not drools"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value(false))
            .andExpect(jsonPath("$.messages").isArray)
            .andExpect(jsonPath("$.issues").isArray)
            .andExpect(jsonPath("$.issues[0].message").isString)
    }

    @Test
    fun shouldEvaluateAgainstGraph() {
        val graphId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val entityId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        given(store.selectInGraph(anyObj<UUID>(), anyObj<Matcher>())).willReturn(
            GraphContents(
                entities = listOf(Entity(entityId, "Component", "1")),
                edges = emptyList(),
            ),
        )
        val saved = stores.policies.save(
            PolicyWrite(
                key = "pass",
                name = "pass",
                engineKind = PolicyEngineKinds.DROOLS,
                body = validDrl,
                applicabilityKind = ApplicabilityKinds.ALWAYS_APPLY,
                categoryId = categoryId,
                tags = listOf("pass"),
            ),
        )

        mockMvc.perform(
            post("/api/v1/objs/policy/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "matcher": { "all": true },
                      "graphId": "$graphId",
                      "policyId": "${saved.id}"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.outcomes[0].status").value("PASS"))
    }

    @Test
    fun shouldReturnServiceUnavailable_whenPersistSuiteWithoutArchive() {
        mockMvc.perform(
            post("/api/v1/objs/policy/evaluations/suite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "result": {
                        "meta": {
                          "evaluationId": "11111111-1111-1111-1111-111111111111",
                          "kind": "SUITE",
                          "evaluatedAtEpochMs": 1
                        },
                        "tree": null,
                        "outcomes": []
                      },
                      "axes": { "results": true, "executionContext": false, "input": false },
                      "tags": ["ci"],
                      "annotations": { "k": "v" }
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isServiceUnavailable)
    }

    @Test
    fun shouldNotAdvertiseArchive_whenArchiveBeanAbsent() {
        mockMvc.perform(get("/api/v1/objs/policy/capabilities"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.operations").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("archive"))))
    }

    @Test
    fun shouldListGetDeleteEvaluations_whenArchivePresent() {
        val id = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val axes = org.poc.objs.policy.api.PersistContentAxes(results = true, executionContext = true, input = true)
        val summary =
            org.poc.objs.policy.api.EvaluationArchiveSummary(
                evaluationId = id,
                kind = org.poc.objs.policy.api.PolicyEvaluationKinds.SUITE,
                name = "nightly",
                evaluatedAtEpochMs = 9_000L,
                tags = listOf("ci"),
                axes = axes,
            )
        val entityId = UUID.randomUUID()
        val fullDoc =
            org.poc.objs.policy.api.EvaluationArchiveDocument(
                evaluationId = id,
                kind = org.poc.objs.policy.api.PolicyEvaluationKinds.SUITE,
                name = "nightly",
                meta =
                    org.poc.objs.policy.api.PolicyEvaluationMeta(
                        evaluationId = id,
                        kind = org.poc.objs.policy.api.PolicyEvaluationKinds.SUITE,
                        evaluatedAtEpochMs = 9_000L,
                        tags = listOf("ci"),
                    ),
                outcomes = emptyList(),
                axes = axes,
                filters = org.poc.objs.policy.api.PersistResultFilters.ALL,
                executionContext = mapOf("source" to "workbench"),
                input =
                    GraphContents(
                        entities = listOf(Entity(id = entityId, type = "Component", schemaVersion = "1")),
                        edges = emptyList(),
                    ),
            )
        val standardDoc = fullDoc.copy(input = null)
        val archive = mock(org.poc.objs.policy.api.EvaluationArchive::class.java)
        given(archive.list(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull()))
            .willReturn(listOf(summary))
        given(archive.load(id)).willReturn(fullDoc)
        given(archive.loadAsStandard(id)).willReturn(standardDoc)
        org.mockito.Mockito.doNothing().`when`(archive).delete(id)

        val mvc = buildMockMvc(archive)

        mvc.perform(get("/api/v1/objs/policy/evaluations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].evaluationId").value(id.toString()))
            .andExpect(jsonPath("$.items[0].name").value("nightly"))

        mvc.perform(get("/api/v1/objs/policy/evaluations/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.input.entities").isArray)
            .andExpect(jsonPath("$.executionContext.source").value("workbench"))

        mvc.perform(get("/api/v1/objs/policy/evaluations/$id").param("view", "standard"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.input").doesNotExist())

        mvc.perform(get("/api/v1/objs/policy/evaluations/$id").param("view", "weird"))
            .andExpect(status().isBadRequest)

        mvc.perform(delete("/api/v1/objs/policy/evaluations/$id"))
            .andExpect(status().isNoContent)

        given(archive.load(id)).willReturn(null)
        mvc.perform(get("/api/v1/objs/policy/evaluations/$id"))
            .andExpect(status().isNotFound)
        mvc.perform(delete("/api/v1/objs/policy/evaluations/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldReturnServiceUnavailable_whenListWithoutArchive() {
        mockMvc.perform(get("/api/v1/objs/policy/evaluations"))
            .andExpect(status().isServiceUnavailable)
    }
}
