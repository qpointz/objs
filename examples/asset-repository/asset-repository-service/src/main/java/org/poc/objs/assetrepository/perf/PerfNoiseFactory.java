package org.poc.objs.assetrepository.perf;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.poc.objs.api.domain.Entity;

/**
 * Minimal schema-valid payloads for AR ontology types (noise only).
 */
final class PerfNoiseFactory {

    static final String SCHEMA_VERSION = "1.0.0";
    static final String ID_PREFIX = "perf-";

    static final List<String> TYPES = List.of(
            "Dataset",
            "LlmModel",
            "AiAgent",
            "Prompt",
            "Skill",
            "Tool",
            "Guardrail",
            "KnowledgeSource",
            "Template",
            "McpServer");

    /** Allowed edge triples used for noise wiring (subset of ontology). */
    static final List<EdgeRule> EDGE_RULES = List.of(
            new EdgeRule("AiAgent", "USES_MODEL", "LlmModel"),
            new EdgeRule("AiAgent", "USES_DATA", "Dataset"),
            new EdgeRule("AiAgent", "USES_SKILL", "Skill"),
            new EdgeRule("AiAgent", "USES_PROMPT", "Prompt"),
            new EdgeRule("AiAgent", "USES_TOOL", "Tool"),
            new EdgeRule("AiAgent", "USES_KNOWLEDGE", "KnowledgeSource"),
            new EdgeRule("AiAgent", "USES_TEMPLATE", "Template"),
            new EdgeRule("AiAgent", "PROTECTED_BY", "Guardrail"),
            new EdgeRule("AiAgent", "USES_MCP_SERVER", "McpServer"),
            new EdgeRule("Dataset", "TRAINS", "LlmModel"),
            new EdgeRule("Dataset", "VALIDATES", "LlmModel"),
            new EdgeRule("Dataset", "EVALUATES", "LlmModel"),
            new EdgeRule("Skill", "USES_MODEL", "LlmModel"),
            new EdgeRule("Skill", "USES_PROMPT", "Prompt"),
            new EdgeRule("Skill", "USES_TOOL", "Tool"),
            new EdgeRule("Skill", "USES_KNOWLEDGE", "KnowledgeSource"),
            new EdgeRule("Skill", "USES_TEMPLATE", "Template"),
            new EdgeRule("Skill", "PROTECTED_BY", "Guardrail"),
            new EdgeRule("Skill", "DEPENDS_ON", "Skill"),
            new EdgeRule("Prompt", "DESIGNED_FOR", "LlmModel"),
            new EdgeRule("Prompt", "USES_TEMPLATE", "Template"),
            new EdgeRule("Tool", "ACCESSES", "Dataset"),
            new EdgeRule("KnowledgeSource", "SOURCED_FROM", "Dataset"),
            new EdgeRule("KnowledgeSource", "USES_MODEL", "LlmModel"),
            new EdgeRule("Guardrail", "APPLIES_TO", "LlmModel"),
            new EdgeRule("Guardrail", "APPLIES_TO", "Dataset"),
            new EdgeRule("McpServer", "PROVIDES_TOOL", "Tool"),
            new EdgeRule("McpServer", "PROVIDES_PROMPT", "Prompt"),
            new EdgeRule("McpServer", "PROVIDES_KNOWLEDGE", "KnowledgeSource"));

    private PerfNoiseFactory() {
    }

    static String typeForIndex(int index) {
        return TYPES.get(index % TYPES.size());
    }

    /** Deterministic entity id (unique across graphs in the shared pool). */
    static UUID entityId(String collection, int graphIndex, int localIndex) {
        String key = "ar.perf|" + collection + "|g" + graphIndex + "|" + localIndex;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    static UUID edgeId(String collection, int graphIndex, int edgeIndex) {
        String key = "ar.perf.edge|" + collection + "|g" + graphIndex + "|" + edgeIndex;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Entity entity(String collection, int graphIndex, int localIndex) {
        String type = typeForIndex(localIndex);
        Map<String, Object> payload = payload(type, graphIndex, localIndex);
        return new Entity(
                entityId(collection, graphIndex, localIndex),
                type,
                SCHEMA_VERSION,
                (Map) new HashMap<>(payload),
                new HashMap<>());
    }

    static Map<String, Object> payload(String type, int graphIndex, int localIndex) {
        String id = ID_PREFIX + graphIndex + "-" + localIndex;
        String name = "noise-" + graphIndex + "-" + localIndex;
        return switch (type) {
            case "Dataset" -> map(
                    "datasetId", id,
                    "name", name,
                    "purpose", "Other",
                    "classification", "Internal");
            case "LlmModel" -> map(
                    "modelId", id,
                    "name", name,
                    "vendor", "perf",
                    "modelType", "Other",
                    "status", "Candidate");
            case "AiAgent" -> map(
                    "agentId", id,
                    "name", name,
                    "category", "perf",
                    "status", "Development",
                    "owner", "perf");
            case "Prompt" -> map(
                    "promptId", id,
                    "name", name,
                    "promptType", "Task",
                    "owner", "perf",
                    "status", "Draft");
            case "Skill" -> map(
                    "skillId", id,
                    "name", name,
                    "owner", "perf",
                    "status", "Draft");
            case "Tool" -> map(
                    "toolId", id,
                    "name", name,
                    "owner", "perf",
                    "status", "Draft");
            case "Guardrail" -> map(
                    "guardrailId", id,
                    "name", name,
                    "owner", "perf",
                    "status", "Draft");
            case "KnowledgeSource" -> map(
                    "knowledgeSourceId", id,
                    "name", name,
                    "owner", "perf",
                    "status", "Development");
            case "Template" -> map(
                    "templateId", id,
                    "name", name,
                    "owner", "perf",
                    "status", "Draft");
            case "McpServer" -> map(
                    "mcpServerId", id,
                    "name", name,
                    "owner", "perf",
                    "status", "Development");
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put((String) kv[i], kv[i + 1]);
        }
        return out;
    }

    record EdgeRule(String sourceType, String role, String targetType) {
    }
}
