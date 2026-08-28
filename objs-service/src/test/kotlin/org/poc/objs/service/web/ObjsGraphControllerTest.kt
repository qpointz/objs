package org.poc.objs.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.core.validation.ValidationIssue
import org.poc.objs.core.validation.ValidationResult
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper

class ObjsGraphControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var store: GraphStore

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    @BeforeEach
    fun setUp() {
        store = mock(GraphStore::class.java)
        mockMvc = MockMvcBuilders
            .standaloneSetup(
                ObjsGraphController(
                    store,
                    mock(org.poc.objs.core.seed.SeedImporter::class.java),
                    mock(org.poc.objs.core.seed.CanonicalSeedSerializer::class.java),
                    mock(NamedGraphStore::class.java),
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
    fun shouldValidateGraph_returning200() {
        given(store.validateMutation(anyObj())).willReturn(
            ValidationResult.of(ValidationIssue("SCHEMA_VIOLATION", "bad")),
        )

        mockMvc.perform(
            post("/api/v1/objs/graph/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entities":{"set":[],"unset":[]},"edges":{"set":[],"unset":[]}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.issues[0].code").value("SCHEMA_VIOLATION"))
    }
}
