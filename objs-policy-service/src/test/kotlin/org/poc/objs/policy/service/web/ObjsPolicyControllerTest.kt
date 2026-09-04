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
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.core.DefaultPolicyEvaluator
import org.poc.objs.policy.core.InMemoryPolicyRepository
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
    private lateinit var repo: InMemoryPolicyRepository

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
        repo = InMemoryPolicyRepository()
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
            evaluator = evaluator,
            knowledgeBaseCache = cache,
        )
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsPolicyController(play))
            .setMessageConverters(
                JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()),
            )
            .build()
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
                      "name": "demo",
                      "engineKind": "DROOLS",
                      "body": "package x\n"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("demo"))
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
                      "name": "demo",
                      "engineKind": "DROOLS",
                      "body": "package updated\n"
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
        val saved = repo.save(
            PolicyWrite(
                name = "pass",
                engineKind = PolicyEngineKinds.DROOLS,
                body = validDrl,
                applicabilityKind = ApplicabilityKinds.ALWAYS_APPLY,
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
}
