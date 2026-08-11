package org.poc.objs.service.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMResolvedGraph
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.seed.AllowedEdgeRuleSeedHandler
import org.poc.objs.core.seed.CanonicalSeedSerializer
import org.poc.objs.core.seed.GRAPH_SEED_KINDS
import org.poc.objs.core.seed.GraphSeedHandler
import org.poc.objs.core.seed.ObjectSchemaSeedHandler
import org.poc.objs.core.seed.SEED_KIND_GRAPH
import org.poc.objs.core.seed.SeedDocumentResult
import org.poc.objs.core.seed.SeedImportException
import org.poc.objs.core.seed.SeedImportResult
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.core.validation.BoMValidationIssue
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.util.UUID

class ObjsGraphIoControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var importer: SeedImporter
    private lateinit var graphStore: BoMGraphStore
    private lateinit var namedGraphs: BoMNamedGraphStore
    private lateinit var serializer: CanonicalSeedSerializer

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObj(): T = org.mockito.ArgumentMatchers.any() as T

    @BeforeEach
    fun setUp() {
        importer = mock(SeedImporter::class.java)
        graphStore = mock(BoMGraphStore::class.java)
        namedGraphs = mock(BoMNamedGraphStore::class.java)
        val schemas = InMemoryBoMSchemaCatalog()
        val rules = InMemoryBoMAllowedEdgeCatalog()
        serializer = CanonicalSeedSerializer(
            schemas,
            rules,
            ObjectSchemaSeedHandler(schemas),
            AllowedEdgeRuleSeedHandler(rules),
            GraphSeedHandler(namedGraphs),
        )
        mockMvc = MockMvcBuilders
            .standaloneSetup(ObjsGraphController(graphStore, importer, serializer, namedGraphs))
            .setMessageConverters(
                JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()),
                StringHttpMessageConverter(StandardCharsets.UTF_8),
            )
            .build()
    }

    @Test
    fun shouldImportMultipartYaml() {
        given(importer.importYaml(anyObj<String>(), org.mockito.ArgumentMatchers.eq(GRAPH_SEED_KINDS)))
            .willReturn(
                SeedImportResult(
                    documents = listOf(
                        SeedDocumentResult(
                            index = 0,
                            kind = SEED_KIND_GRAPH,
                            apiVersion = "objs.poc.org/v1",
                            applied = true,
                        ),
                    ),
                ),
            )
        val file = MockMultipartFile(
            "file",
            "seed.yaml",
            "application/yaml",
            "apiVersion: objs.poc.org/v1\nkind: Graph\n".toByteArray(),
        )
        mockMvc.perform(multipart("/api/v1/objs/graph/import").param("format", "seeds").file(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.documents[0].applied").value(true))
        verify(importer).importYaml(anyObj<String>(), org.mockito.ArgumentMatchers.eq(GRAPH_SEED_KINDS))
    }

    @Test
    fun shouldReturnBadRequest_whenImportFails() {
        given(importer.importYaml(anyObj<String>(), org.mockito.ArgumentMatchers.eq(GRAPH_SEED_KINDS)))
            .willThrow(
                SeedImportException(
                    "failed",
                    SeedImportResult(
                        documents = listOf(
                            SeedDocumentResult(
                                index = 0,
                                kind = "ObjectSchema",
                                apiVersion = "objs.poc.org/v1",
                                errors = listOf(
                                    BoMValidationIssue("SEED_KIND_NOT_ALLOWED", "not allowed"),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        val file = MockMultipartFile("file", "seed.yaml", "application/yaml", "kind: X".toByteArray())
        mockMvc.perform(multipart("/api/v1/objs/graph/import").param("format", "seeds").file(file))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.documents[0].errors[0].code").value("SEED_KIND_NOT_ALLOWED"))
    }

    @Test
    fun shouldRejectExport_whenGraphIdMissing() {
        mockMvc.perform(get("/api/v1/objs/graph/export").param("format", "seeds"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.issues[0].code").value("GRAPH_ID_REQUIRED"))
    }

    @Test
    fun shouldReturn404_whenExportGraphMissing() {
        val graphId = UUID.randomUUID()
        given(namedGraphs.get(graphId)).willReturn(null)
        mockMvc.perform(
            get("/api/v1/objs/graph/export")
                .param("format", "seeds")
                .param("graphId", graphId.toString()),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldExportBoundedGraph() {
        val graphId = UUID.randomUUID()
        val id = UUID.randomUUID()
        given(namedGraphs.get(graphId)).willReturn(
            BoMResolvedGraph(
                id = graphId,
                annotations = mapOf("app" to "demo"),
                contents = BoMGraphContents(
                    entities = listOf(
                        BoMEntity(
                            id = id,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "Ada"),
                            annotations = mutableMapOf("app" to "demo"),
                        ),
                    ),
                    edges = emptyList(),
                ),
            ),
        )
        val result = mockMvc.perform(
            get("/api/v1/objs/graph/export")
                .param("format", "seeds")
                .param("graphId", graphId.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/yaml")))
            .andReturn()
        val body = result.response.contentAsString
        assert(body.contains("Graph")) { body }
        assert(body.contains(id.toString())) { body }
    }
}
