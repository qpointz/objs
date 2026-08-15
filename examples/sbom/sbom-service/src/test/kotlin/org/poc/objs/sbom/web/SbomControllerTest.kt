package org.poc.objs.sbom.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.validation.BoMValidationResult
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.model.SbomApplicationCatalog
import org.poc.objs.sbom.model.SbomApplicationVersions
import org.poc.objs.sbom.service.SbomService
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper

class SbomControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var sbom: SbomService

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    @BeforeEach
    fun setUp() {
        sbom = mock(SbomService::class.java)
        mockMvc = MockMvcBuilders
            .standaloneSetup(SbomController(sbom))
            .setMessageConverters(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
            .build()
    }

    @Test
    fun shouldListApplications() {
        given(sbom.listApplications()).willReturn(
            SbomApplicationCatalog(
                applications = listOf(
                    SbomApplicationVersions("billing-api", listOf("1.0.0")),
                    SbomApplicationVersions("payments-api", listOf("2.3.1", "2.4.0")),
                ),
            ),
        )

        mockMvc.perform(get("/api/v1/example/sbom/apps"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applications[0].app").value("billing-api"))
            .andExpect(jsonPath("$.applications[1].versions[1]").value("2.4.0"))
    }

    @Test
    fun shouldGetByAppAndVersion() {
        given(sbom.getSbom(anyString(), anyString(), anyMap())).willReturn(
            BoMGraphContents(
                entities = listOf(
                    BoMEntity(
                        type = "Component",
                        schemaVersion = "1.0.0",
                        payload = mutableMapOf("name" to "Spring Boot"),
                        annotations = mutableMapOf("app" to "payments-api", "appVersion" to "2.3.1"),
                    ),
                ),
                edges = emptyList(),
            ),
        )

        mockMvc.perform(get("/api/v1/example/sbom/apps/payments-api/versions/2.3.1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.entities[0].payload.name").value("Spring Boot"))
    }

    @Test
    fun shouldGetByApp() {
        given(sbom.getSbom(anyString(), isNull(), anyMap())).willReturn(
            BoMGraphContents(entities = emptyList(), edges = emptyList()),
        )

        mockMvc.perform(get("/api/v1/example/sbom/apps/payments-api"))
            .andExpect(status().isOk)
    }

    @Test
    fun shouldPutWithOriginAnnotation() {
        given(sbom.save(anyObj<SbomContext>(), anyObj<BoMGraph>(), anyMap())).willReturn(BoMValidationResult.ok())

        val body =
            """{"entities":[{"type":"Component","schemaVersion":"1.0.0","payload":{"name":"X","version":"1","ecosystem":"Maven","kind":"library"},"annotations":{}}],"edges":[]}"""

        mockMvc.perform(
            put("/api/v1/example/sbom/apps/payments-api/versions/2.3.1")
                .param("origin", "batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)

        verify(sbom).save(anyObj(), anyObj(), anyMap())
    }
}
