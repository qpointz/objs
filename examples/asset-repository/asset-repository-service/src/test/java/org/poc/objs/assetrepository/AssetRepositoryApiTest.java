package org.poc.objs.assetrepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.poc.objs.api.domain.Schema;
import org.poc.objs.api.domain.SchemaCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ar_wi004;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "objs.seeds.enabled=true",
        "objs.seeds.resources[0]=classpath:seeds/asset-repository-ontology.yaml",
        "objs.seeds.on-failure=FAIL_FAST"
})
class AssetRepositoryApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SchemaCatalog schemas;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCreateCollectionWriteAndSearchObject() throws Exception {
        String createBody = mapper.writeValueAsString(Map.of(
                "name", "prompts-test",
                "owner", "ai-platform",
                "objectWriteMode", "UUID_OR_IDENTIFIER",
                "types", java.util.List.of(Map.of("objectType", "Prompt"))
        ));

        MvcResult created = mockMvc.perform(post("/api/v1/asset-repository/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        assertThat(created.getResponse().getStatus())
                .as(created.getResponse().getContentAsString())
                .isEqualTo(201);

        String collectionId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/asset-repository/collections/" + collectionId + "/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionId").value(collectionId))
                .andExpect(jsonPath("$.objectCount").value(0));

        String writeBody = mapper.writeValueAsString(Map.of(
                "type", "Prompt",
                "schemaVersion", "1.0.0",
                "payload", Map.of(
                        "promptId", "prompt-greet-001",
                        "name", "greet",
                        "promptType", "Task",
                        "owner", "ai-platform",
                        "status", "Approved",
                        "body", "Hello {{name}}")
        ));

        MvcResult written = mockMvc.perform(post("/api/v1/asset-repository/collections/" + collectionId + "/objects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody))
                .andReturn();
        assertThat(written.getResponse().getStatus())
                .as(written.getResponse().getContentAsString())
                .isEqualTo(201);

        // identifier upsert
        mockMvc.perform(post("/api/v1/asset-repository/collections/" + collectionId + "/objects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "type", "Prompt",
                                "schemaVersion", "1.0.0",
                                "payload", Map.of(
                                        "promptId", "prompt-greet-001",
                                        "name", "greet",
                                        "promptType", "Task",
                                        "owner", "ai-platform",
                                        "status", "Approved",
                                        "body", "Hello {{name}}",
                                        "description", "Greeting prompt")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload.description").value("Greeting prompt"));

        MvcResult listed = mockMvc.perform(get("/api/v1/asset-repository/collections/" + collectionId + "/objects"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = mapper.readTree(listed.getResponse().getContentAsString());
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isEqualTo(1);
        String objectId = list.get(0).get("id").asText();

        mockMvc.perform(get("/api/v1/asset-repository/collections/" + collectionId + "/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectCount").value(1));

        mockMvc.perform(post("/api/v1/asset-repository/collections/" + collectionId + "/objects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "id", objectId,
                                "type", "Prompt",
                                "schemaVersion", "1.0.0",
                                "payload", Map.of("description", "partial-only")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(objectId))
                .andExpect(jsonPath("$.payload.name").value("greet"))
                .andExpect(jsonPath("$.payload.body").value("Hello {{name}}"))
                .andExpect(jsonPath("$.payload.description").value("partial-only"));

        mockMvc.perform(post("/api/v1/asset-repository/collections/" + collectionId + "/objects/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "filters", Map.of("name", "greet")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payload.name").value("greet"));

        mockMvc.perform(post("/api/v1/asset-repository/collections/" + collectionId + "/objects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "type", "Dataset",
                                "schemaVersion", "1.0.0",
                                "payload", Map.of(
                                        "datasetId", "DS-X",
                                        "name", "x",
                                        "purpose", "Other",
                                        "classification", "Internal")
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExposeSeededSchemaViaDomainApi() throws Exception {
        mockMvc.perform(get("/api/v1/asset-repository/schemas/Dataset/1.0.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Dataset"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.contentSchema.type").value("OBJECT"))
                .andExpect(jsonPath("$.contentSchema.fields[0].name").value("datasetId"))
                .andExpect(jsonPath("$.tags[0]").value("data"));

        mockMvc.perform(get("/api/v1/asset-repository/schemas").param("type", "Dataset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("Dataset"));
    }

    @Test
    void shouldExposeSchemaCatalogWithUsedInCollections() throws Exception {
        String createBody = mapper.writeValueAsString(Map.of(
                "name", "schema-catalog-test",
                "owner", "data-eng",
                "objectWriteMode", "UUID_OR_IDENTIFIER",
                "types", java.util.List.of(Map.of("objectType", "Dataset"))
        ));
        MvcResult created = mockMvc.perform(post("/api/v1/asset-repository/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        String collectionId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        MvcResult catalog = mockMvc.perform(get("/api/v1/asset-repository/schema-catalog"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rows = mapper.readTree(catalog.getResponse().getContentAsString());
        assertThat(rows.isArray()).isTrue();
        JsonNode dataset = null;
        for (JsonNode row : rows) {
            assertThat(row.has("versions")).isTrue();
            assertThat(row.get("latestVersion").asText()).isEqualTo(
                    row.get("versions").get(row.get("versions").size() - 1).asText());
            if ("Dataset".equals(row.get("type").asText())) {
                dataset = row;
            }
        }
        assertThat(dataset).isNotNull();
        assertThat(dataset.get("latestVersion").asText()).isEqualTo("1.0.0");
        boolean found = false;
        for (JsonNode used : dataset.get("usedIn")) {
            if (collectionId.equals(used.get("id").asText())) {
                found = true;
                assertThat(used.get("name").asText()).isEqualTo("schema-catalog-test");
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void shouldListObjectRelationsForComposition() throws Exception {
        String createBody = mapper.writeValueAsString(Map.of(
                "name", "dp-rel-test",
                "owner", "data-eng",
                "objectWriteMode", "UUID_OR_IDENTIFIER",
                "types", java.util.List.of(
                        Map.of("objectType", "Dataset"),
                        Map.of("objectType", "LlmModel"))
        ));
        MvcResult created = mockMvc.perform(post("/api/v1/asset-repository/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        String collectionId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        String composition = mapper.writeValueAsString(Map.of(
                "objects", java.util.List.of(
                        Map.of("type", "Dataset", "schemaVersion", "1.0.0",
                                "payload", Map.of(
                                        "datasetId", "DS-A",
                                        "name", "ds-a",
                                        "purpose", "Evaluation",
                                        "classification", "Internal")),
                        Map.of("type", "LlmModel", "schemaVersion", "1.0.0",
                                "payload", Map.of(
                                        "modelId", "gpt-4.1",
                                        "name", "GPT-4.1",
                                        "vendor", "OpenAI",
                                        "modelType", "General Purpose",
                                        "status", "Approved"))),
                "relations", java.util.List.of(
                        Map.of("sourceKey", "obj-0", "role", "EVALUATES", "targetKey", "obj-1"))
        ));
        MvcResult written = mockMvc.perform(post("/api/v1/asset-repository/collections/" + collectionId + "/compositions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(composition))
                .andReturn();
        assertThat(written.getResponse().getStatus())
                .as(written.getResponse().getContentAsString())
                .isEqualTo(201);
        JsonNode saved = mapper.readTree(written.getResponse().getContentAsString());
        String dbId = saved.get(0).get("id").asText();
        String dsId = saved.get(1).get("id").asText();

        mockMvc.perform(get("/api/v1/asset-repository/collections/" + collectionId + "/objects/" + dbId + "/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("EVALUATES"))
                .andExpect(jsonPath("$[0].direction").value("OUTGOING"))
                .andExpect(jsonPath("$[0].related.id").value(dsId));

        mockMvc.perform(get("/api/v1/asset-repository/collections/" + collectionId + "/objects/" + dsId + "/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("EVALUATES"))
                .andExpect(jsonPath("$[0].direction").value("INCOMING"))
                .andExpect(jsonPath("$[0].related.id").value(dbId));
    }

    @Test
    void shouldExposeAllowedEdgesForTypeWithMetadata() throws Exception {
        MvcResult agent = mockMvc.perform(get("/api/v1/asset-repository/schema-catalog/AiAgent/allowed-edges"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode outgoing = mapper.readTree(agent.getResponse().getContentAsString()).get("outgoing");
        JsonNode usesModel = null;
        for (JsonNode rule : outgoing) {
            if ("USES_MODEL".equals(rule.get("role").asText())
                    && "LlmModel".equals(rule.get("targetType").asText())) {
                usesModel = rule;
            }
        }
        assertThat(usesModel).isNotNull();
        assertThat(usesModel.get("description").asText()).isEqualTo("Agent invokes the language model");
        assertThat(usesModel.get("sourceVerb").asText()).isEqualTo("uses");
        assertThat(usesModel.get("targetVerb").asText()).isEqualTo("used by");
        assertThat(usesModel.get("tags").get(0).asText()).isEqualTo("runtime");

        MvcResult dataset = mockMvc.perform(get("/api/v1/asset-repository/schema-catalog/Dataset/allowed-edges"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode incoming = mapper.readTree(dataset.getResponse().getContentAsString()).get("incoming");
        boolean foundIncoming = false;
        for (JsonNode rule : incoming) {
            if ("USES_DATA".equals(rule.get("role").asText())
                    && "AiAgent".equals(rule.get("sourceType").asText())) {
                foundIncoming = true;
            }
        }
        assertThat(foundIncoming).isTrue();
    }

    @Test
    void shouldDefaultCreateToHighestSchemaVersion() throws Exception {
        Schema v1 = schemas.get("Prompt", "1.0.0");
        assertThat(v1).isNotNull();
        schemas.register(new Schema(
                v1.getType(),
                "2.0.0",
                v1.getContentSchema(),
                v1.getUsage(),
                v1.getTags(),
                v1.getAttributes()
        ));

        String createBody = mapper.writeValueAsString(Map.of(
                "name", "latest-schema-create",
                "owner", "ai-platform",
                "objectWriteMode", "UUID_OR_IDENTIFIER",
                "types", java.util.List.of(Map.of("objectType", "Prompt"))
        ));
        MvcResult created = mockMvc.perform(post("/api/v1/asset-repository/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();
        assertThat(created.getResponse().getStatus())
                .as(created.getResponse().getContentAsString())
                .isEqualTo(201);
        String collectionId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        String writeBody = mapper.writeValueAsString(Map.of(
                "type", "Prompt",
                "payload", Map.of(
                        "promptId", "prompt-latest-001",
                        "name", "latest",
                        "promptType", "Task",
                        "owner", "ai-platform",
                        "status", "Approved",
                        "body", "Hello {{name}}")
        ));
        mockMvc.perform(post("/api/v1/asset-repository/collections/" + collectionId + "/objects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.schemaVersion").value("2.0.0"));
    }

    @Test
    void shouldExposeDomainOpenApiGroup() throws Exception {
        mockMvc.perform(get("/v3/api-docs/asset-repository"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Asset repository API"))
                .andExpect(jsonPath("$.paths['/api/v1/asset-repository/collections']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/asset-repository/schemas/{type}/{version}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/asset-repository/schema-catalog']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/asset-repository/schema-catalog/{type}/allowed-edges']").exists());
    }
}
