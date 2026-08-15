package org.poc.objs.sbom.web

import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

/**
 * Publishes registry JSON Schemas into OpenAPI components and rewires example-sbom
 * operation request/response bodies so PUT/GET payloads use `oneOf` those domain schemas
 * (not a bare `Map` payload).
 */
@Component
class SbomDomainOpenApiCustomizer(
    private val schemas: BoMSchemaCatalog,
    private val edgeRules: BoMAllowedEdgeCatalog,
) : OpenApiCustomizer {

    override fun customise(openApi: OpenAPI) {
        val components = openApi.components ?: Components().also { openApi.components = it }
        val published = linkedMapOf<String, Schema<*>>()

        for (entry in schemas.all()) {
            val name = schemaComponentName(entry)
            published[name] = jsonSchemaMapToOpenApiSchema(entry)
        }
        for ((name, schema) in published) {
            components.addSchemas(name, schema)
        }

        val entityPayloadRefs = published.keys
            .filter { !it.startsWith("CanonicalEdge.") }
            .map { refSchema(it) }
        val edgePropRefs = published.keys
            .filter { it.startsWith("CanonicalEdge.") }
            .map { refSchema(it) }
            .ifEmpty { listOf(ObjectSchema().additionalProperties(true)) }

        components.addSchemas(SBOM_ENTITY, sbomEntitySchema(entityPayloadRefs))
        components.addSchemas(SBOM_EDGE, sbomEdgeSchema(edgePropRefs))
        components.addSchemas(SBOM_GRAPH, sbomGraphSchema())
        components.addSchemas(SBOM_SUBGRAPH, sbomSubgraphSchema())

        rewriteOperationBodies(openApi)

        val typeList = published.keys.joinToString(", ")
        val edges = edgeRules.all().joinToString("\n") {
            "- `${it.sourceType}` —[${it.role}]→ `${it.targetType}` (${it.propertiesPolicy})"
        }
        val extra = buildString {
            appendLine()
            appendLine("## Domain object schemas (from registry)")
            appendLine()
            appendLine(
                "Entity `payload` on PUT/GET is documented as **oneOf** registered domain schemas " +
                    "(`$SBOM_GRAPH` / `$SBOM_ENTITY`). Edge `properties` oneOf `CanonicalEdge.*`. " +
                    "Catalog source: `BoMSchemaCatalog` (ontology seed). " +
                    "Foundation registry REST (`/api/v1/objs/**`) is the `:objs-app` side service only.",
            )
            appendLine()
            if (typeList.isNotBlank()) {
                appendLine("Registered: $typeList")
                appendLine()
            }
            if (edges.isNotBlank()) {
                appendLine("### Allowed edges")
                appendLine()
                appendLine(edges)
            }
        }
        val info = openApi.info ?: Info().also { openApi.info = it }
        val base = info.description.orEmpty()
        if (!base.contains("Domain object schemas (from registry)")) {
            info.description = base + extra
        }
    }

    private fun rewriteOperationBodies(openApi: OpenAPI) {
        val paths = openApi.paths ?: return
        val graphMedia = MediaType().schema(refSchema(SBOM_GRAPH))
        val subgraphMedia = MediaType().schema(refSchema(SBOM_SUBGRAPH))
        val graphContent = Content().addMediaType("application/json", graphMedia)
        val subgraphContent = Content().addMediaType("application/json", subgraphMedia)

        for ((path, item) in paths) {
            if (!path.startsWith("/api/v1/example/sbom")) continue
            item.readOperationsMap().forEach { (method, op) ->
                when (method.name) {
                    "PUT", "POST", "PATCH" -> {
                        op.requestBody = (op.requestBody ?: RequestBody())
                            .description(
                                "BoMGraph whose entity payloads match registry schemas " +
                                    "(see oneOf on $SBOM_ENTITY.payload). " +
                                    "Set entity.type / schemaVersion to select the schema " +
                                    "(e.g. Component / 1.0.0).",
                            )
                            .required(true)
                            .content(graphContent)
                        op.responses?.forEach { (code, resp) ->
                            if (code.startsWith("2")) {
                                applyContent(resp, graphContent)
                            }
                        }
                    }
                    "GET" -> {
                        op.responses?.forEach { (code, resp) ->
                            if (code.startsWith("2")) {
                                applyContent(resp, subgraphContent)
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun applyContent(response: ApiResponse, content: Content) {
        response.content = content
        if (response.description.isNullOrBlank()) {
            response.description = "OK"
        }
    }

    companion object {
        const val SBOM_ENTITY = "SbomEntity"
        const val SBOM_EDGE = "SbomEdge"
        const val SBOM_GRAPH = "SbomGraph"
        const val SBOM_SUBGRAPH = "SbomSubgraph"

        fun schemaComponentName(entry: BoMSchema): String = "${entry.type}.${entry.version}"

        fun refSchema(name: String): Schema<*> =
            Schema<Any>().`$ref`("#/components/schemas/$name")

        fun jsonSchemaMapToOpenApiSchema(entry: BoMSchema): Schema<*> {
            @Suppress("UNCHECKED_CAST")
            val schema = Json.mapper().convertValue(entry.toJsonSchema(), Schema::class.java) as Schema<Any>
            schema.name = schemaComponentName(entry)
            schema.title = schema.title ?: "${entry.type} (${entry.version})"
            schema.description = listOfNotNull(
                schema.description,
                "Registry key: type=${entry.type}, schemaVersion=${entry.version}.",
            ).joinToString(" ")
            return schema
        }

        fun sbomEntitySchema(payloadOneOf: List<Schema<*>>): Schema<*> {
            val payload = Schema<Any>().apply {
                description =
                    "Domain payload; must match JSON Schema for entity.type + schemaVersion. oneOf registered types."
                oneOf = payloadOneOf.ifEmpty { listOf(ObjectSchema().additionalProperties(true)) }
            }
            return ObjectSchema()
                .name(SBOM_ENTITY)
                .description("Foundation entity with registry-typed payload")
                .addProperty("id", StringSchema().format("uuid").description("Optional; assigned on create"))
                .addProperty("type", StringSchema().description("Registry type, e.g. Component"))
                .addProperty("schemaVersion", StringSchema().description("Registry schema version, e.g. 1.0.0"))
                .addProperty("payload", payload)
                .addProperty(
                    "annotations",
                    MapSchema().additionalProperties(StringSchema())
                        .description("e.g. app, appVersion, source, origin"),
                )
                .required(listOf("type", "schemaVersion", "payload"))
        }

        fun sbomEdgeSchema(propertiesOneOf: List<Schema<*>>): Schema<*> {
            val props = Schema<Any>().apply {
                description = "Edge properties when policy is SCHEMA (CanonicalEdge)"
                oneOf = propertiesOneOf
                nullable = true
            }
            return ObjectSchema()
                .name(SBOM_EDGE)
                .addProperty("id", StringSchema().format("uuid"))
                .addProperty("source", StringSchema().format("uuid"))
                .addProperty("target", StringSchema().format("uuid"))
                .addProperty("role", StringSchema().description("e.g. DEPENDS_ON"))
                .addProperty("type", StringSchema().nullable(true).description("e.g. CanonicalEdge"))
                .addProperty("schemaVersion", StringSchema().nullable(true))
                .addProperty("properties", props)
                .required(listOf("source", "target", "role"))
        }

        fun sbomGraphSchema(): Schema<*> =
            ObjectSchema()
                .name(SBOM_GRAPH)
                .description("Batch upsert body (entities + edges)")
                .addProperty("entities", ArraySchema().items(refSchema(SBOM_ENTITY)))
                .addProperty("edges", ArraySchema().items(refSchema(SBOM_EDGE)))

        fun sbomSubgraphSchema(): Schema<*> =
            ObjectSchema()
                .name(SBOM_SUBGRAPH)
                .description("Induced subgraph result")
                .addProperty("entities", ArraySchema().items(refSchema(SBOM_ENTITY)))
                .addProperty("edges", ArraySchema().items(refSchema(SBOM_EDGE)))
    }
}
