package org.poc.objs.assetrepository.demo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.domain.CollectionTypeSpec;
import org.poc.objs.assetrepository.domain.ObjectWriteMode;
import org.poc.objs.assetrepository.service.CollectionService;
import org.poc.objs.assetrepository.service.ObjectWriteService;
import org.poc.objs.assetrepository.web.dto.ApiDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Demo profile: library collections (asset shelves) + product collections (minigraphs).
 * Ontology schemas must already be seeded ({@code objs.seeds} on the same profile).
 */
@Component
@Profile("demo")
@Order(100)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String V = "1.0.0";

    private final CollectionService collections;
    private final ObjectWriteService objects;

    public DemoDataSeeder(CollectionService collections, ObjectWriteService objects) {
        this.collections = collections;
        this.objects = objects;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!collections.list(null, null, null).isEmpty()) {
            log.info("Demo collections already present; skipping demo seed");
            return;
        }

        seedLibrary("databases", "Database inventory (library shelf)", "data-eng",
                "Database", Map.of(
                        "name", "analytics-warehouse",
                        "engine", "snowflake",
                        "version", "8",
                        "environment", "prod",
                        "description", "Shared analytics warehouse"));

        seedLibrary("datasets", "Dataset inventory (library shelf)", "data-eng",
                "Dataset", Map.of(
                        "name", "orders_facts",
                        "datasetType", "table",
                        "classification", "internal",
                        "description", "Orders fact table (library copy)"));

        seedLibrary("prompts", "Prompt inventory (library shelf)", "ai-platform",
                "Prompt", Map.of(
                        "name", "triage-system",
                        "template", "You are a support triage assistant. Classify the ticket.",
                        "description", "Shared triage system prompt"));

        seedLibrary("skills", "Skill inventory (library shelf)", "ai-platform",
                "Skill", Map.of(
                        "name", "summarize-thread",
                        "description", "Summarize a long support thread"));

        seedLibrary("tools", "Tool inventory (library shelf)", "ai-platform",
                "Tool", Map.of(
                        "name", "ticket-lookup",
                        "kind", "http",
                        "description", "Lookup ticket by id"));

        seedLibrary("mcp-servers", "MCP server inventory (library shelf)", "ai-platform",
                "McpServer", Map.of(
                        "name", "docs-mcp",
                        "endpoint", "https://mcp.example.com/docs",
                        "transport", "sse",
                        "description", "Internal docs MCP"));

        seedLibrary("model-families", "Model family inventory (library shelf)", "ai-platform",
                "ModelFamily", Map.of(
                        "name", "claude",
                        "vendor", "Anthropic",
                        "description", "Claude family (library entry)"));

        seedLibrary("modalities", "Modality inventory (library shelf)", "ai-platform",
                "Modality", Map.of(
                        "name", "embedding",
                        "description", "Vector embedding modality"));

        UUID dataProductId = seedDataProduct();
        UUID agentProductId = seedAgentProduct();
        UUID modelProductId = seedModelProduct();

        log.info(
                "Demo seed complete: libraries + products dp-customers={}, agent-support={}, models-openai={}",
                dataProductId,
                agentProductId,
                modelProductId);
    }

    private void seedLibrary(
            String name,
            String description,
            String owner,
            String type,
            Map<String, Object> sample
    ) {
        CollectionEntity col = collections.create(
                name,
                description,
                owner,
                owner + "@example.com",
                null,
                "P3 — best effort",
                ObjectWriteMode.UUID_OR_IDENTIFIER,
                List.of(CollectionTypeSpec.of(type)));
        objects.writeObject(col.getId(), new ApiDtos.WriteObjectRequest(null, type, V, sample));
    }

    private UUID seedDataProduct() {
        CollectionEntity col = collections.create(
                "dp-customers",
                "Customer data product — Database CONTAINS Dataset assets",
                "data-eng",
                "data@example.com",
                "data-oncall@example.com",
                "P2 — next business day",
                ObjectWriteMode.UUID_OR_IDENTIFIER,
                List.of(CollectionTypeSpec.of("Database"), CollectionTypeSpec.of("Dataset")));

        objects.writeComposition(col.getId(), new ApiDtos.CompositionRequest(
                List.of(
                        obj("Database", Map.of(
                                "name", "customers-db",
                                "engine", "postgresql",
                                "version", "16",
                                "environment", "prod",
                                "description", "Customer system of record")),
                        obj("Dataset", Map.of(
                                "name", "customers",
                                "datasetType", "table",
                                "classification", "confidential",
                                "description", "Customer master table")),
                        obj("Dataset", Map.of(
                                "name", "customer_events",
                                "datasetType", "topic",
                                "classification", "internal",
                                "description", "Downstream event feed"))),
                List.of(
                        rel("obj-0", "CONTAINS", "obj-1"),
                        rel("obj-0", "CONTAINS", "obj-2"))));
        return col.getId();
    }

    private UUID seedAgentProduct() {
        CollectionEntity col = collections.create(
                "agent-support",
                "Support agent product — agent wires prompts, skills, tools, MCP (copy-on-assemble)",
                "ai-platform",
                "ai@example.com",
                "ai-oncall@example.com",
                "P2 — next business day",
                ObjectWriteMode.UUID_OR_IDENTIFIER,
                List.of(
                        CollectionTypeSpec.of("AiAgent"),
                        CollectionTypeSpec.of("Prompt"),
                        CollectionTypeSpec.of("Skill"),
                        CollectionTypeSpec.of("Tool"),
                        CollectionTypeSpec.of("McpServer")));

        // indices: 0 agent, 1 prompt, 2 skill, 3 tool, 4 mcp
        objects.writeComposition(col.getId(), new ApiDtos.CompositionRequest(
                List.of(
                        obj("AiAgent", Map.of(
                                "name", "support-triage",
                                "role", "triage",
                                "runtime", "cursor-agent",
                                "description", "Routes and summarizes support tickets")),
                        obj("Prompt", Map.of(
                                "name", "support-system",
                                "template", "You triage support tickets for Acme.",
                                "description", "Local copy of system prompt")),
                        obj("Skill", Map.of(
                                "name", "classify-severity",
                                "description", "Assign P1–P4 severity")),
                        obj("Tool", Map.of(
                                "name", "kb-search",
                                "kind", "mcp",
                                "description", "Search knowledge base via MCP")),
                        obj("McpServer", Map.of(
                                "name", "support-kb-mcp",
                                "endpoint", "https://mcp.example.com/support-kb",
                                "transport", "sse",
                                "description", "Support KB MCP"))),
                List.of(
                        rel("obj-0", "USES_PROMPT", "obj-1"),
                        rel("obj-0", "HAS_SKILL", "obj-2"),
                        rel("obj-0", "USES_TOOL", "obj-3"),
                        rel("obj-0", "CONNECTS_TO", "obj-4"),
                        rel("obj-2", "USES_TOOL", "obj-3"),
                        rel("obj-3", "BACKED_BY", "obj-4"))));
        return col.getId();
    }

    private UUID seedModelProduct() {
        CollectionEntity col = collections.create(
                "models-openai",
                "OpenAI model catalog product — family, versions, modalities",
                "ai-platform",
                "ai@example.com",
                null,
                "P3 — best effort",
                ObjectWriteMode.UUID_OR_IDENTIFIER,
                List.of(
                        CollectionTypeSpec.of("ModelFamily"),
                        CollectionTypeSpec.of("ModelVersion"),
                        CollectionTypeSpec.of("Modality")));

        // 0 family, 1 version gpt-4.1, 2 version gpt-4.1-mini, 3 text, 4 vision
        objects.writeComposition(col.getId(), new ApiDtos.CompositionRequest(
                List.of(
                        obj("ModelFamily", Map.of(
                                "name", "gpt",
                                "vendor", "OpenAI",
                                "description", "GPT family")),
                        obj("ModelVersion", Map.of(
                                "name", "gpt-4.1",
                                "contextWindow", 1_047_576,
                                "releasedAt", "2025-04-14",
                                "description", "Flagship GPT-4.1")),
                        obj("ModelVersion", Map.of(
                                "name", "gpt-4.1-mini",
                                "contextWindow", 1_047_576,
                                "releasedAt", "2025-04-14",
                                "description", "Smaller GPT-4.1")),
                        obj("Modality", Map.of(
                                "name", "text",
                                "description", "Text in/out")),
                        obj("Modality", Map.of(
                                "name", "vision",
                                "description", "Image understanding"))),
                List.of(
                        rel("obj-0", "HAS_VERSION", "obj-1"),
                        rel("obj-0", "HAS_VERSION", "obj-2"),
                        rel("obj-1", "SUPPORTS", "obj-3"),
                        rel("obj-1", "SUPPORTS", "obj-4"),
                        rel("obj-2", "SUPPORTS", "obj-3"))));
        return col.getId();
    }

    private static ApiDtos.WriteObjectRequest obj(String type, Map<String, Object> payload) {
        return new ApiDtos.WriteObjectRequest(null, type, V, payload);
    }

    private static ApiDtos.RelationInput rel(String source, String role, String target) {
        return new ApiDtos.RelationInput(source, role, target);
    }
}
