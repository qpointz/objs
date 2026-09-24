package org.poc.objs.service.web

import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Fills descriptions for topic tags that already appear on operations.
 * Never invents empty tags (that produced ghost Swagger sections like empty `algorithms`).
 */
object ObjsOpenApiTags {
    fun describe(vararg descriptions: Pair<String, String>): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val used =
                openApi.paths
                    ?.values
                    ?.asSequence()
                    ?.flatMap { it.readOperations().asSequence() }
                    ?.flatMap { (it.tags ?: emptyList()).asSequence() }
                    ?.toSet()
                    .orEmpty()
            openApi.tags?.removeIf { it.name !in used }
            descriptions.forEach { (name, description) ->
                openApi.tags
                    ?.firstOrNull { it.name == name }
                    ?.takeIf { it.description.isNullOrBlank() }
                    ?.also { it.description = description }
            }
        }
}

/**
 * SpringDoc groups for foundation objs REST (C-42).
 * Policy has its own group bean in `:objs-policy-service`.
 */
@Configuration
class ObjsOpenApiConfiguration {

    @Bean
    fun objsGraphApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("graph")
            .packagesToScan(
                "org.poc.objs.service.web",
                "org.poc.objs.gremlin.service.web",
                "org.poc.objs.jgrapht.service.web",
            )
            .pathsToMatch(
                "/api/v1/objs/graph",
                "/api/v1/objs/graph/**",
                "/api/v1/objs/graph/algorithms",
                "/api/v1/objs/graph/algorithms/**",
                "/api/v1/objs/graph/traverse",
                "/api/v1/objs/graph/traverse/**",
                "/api/v1/objs/graphs",
                "/api/v1/objs/graphs/**",
                "/api/v1/objs/entities",
                "/api/v1/objs/entities/**",
                "/api/v1/objs/edges",
                "/api/v1/objs/edges/**",
                "/api/v1/objs/status",
            )
            .addOpenApiCustomizer(
                ObjsOpenApiTags.describe(
                    "graphs" to "Named-graph headers: list, search, create, read, annotate, delete",
                    "mutations" to "Graph-scoped MERGE / REPLACE mutate and dry-run validate",
                    "query" to "Matcher DSL selection in a graph, across graphs, or in a frozen version",
                    "versions" to "Deep graph versions: snapshot, list, reconstruct, reset, apply structure",
                    "housekeeping" to "Destructive maintenance: clear, destroy, clone, purge versions",
                    "seeds" to "Graph seed validate, import, and export under /graph",
                    "traverse" to "Gremlin traversal over a materialized graph",
                    "algorithms" to "JGraphT capability probe and cycle analysis",
                    "entities" to "Pool entity CRUD and graph attach/detach",
                    "edges" to "Pool edge history and graph-scoped edge CRUD",
                    "status" to "Service status probe",
                ),
            )
            .build()

    @Bean
    fun objsRegistryApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("registry")
            .packagesToScan("org.poc.objs.service.web")
            .pathsToMatch("/api/v1/objs/registry/**")
            .addOpenApiCustomizer(
                ObjsOpenApiTags.describe(
                    "catalog" to "Registry catalog refresh and seed import/export",
                    "schemas" to "Object-schema type@version CRUD, lint, and JSON Schema export",
                    "edges" to "Relation allow-list rules: list, upsert, delete",
                ),
            )
            .build()
}
