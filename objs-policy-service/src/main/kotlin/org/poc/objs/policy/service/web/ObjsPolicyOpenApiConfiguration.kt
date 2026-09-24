package org.poc.objs.policy.service.web

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.poc.objs.service.web.ObjsOpenApiTags

/** Separate OpenAPI definition for policy REST (C-42 G-3). */
@Configuration
class ObjsPolicyOpenApiConfiguration {

    @Bean
    fun objsPolicyApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("policy")
            .pathsToMatch(
                "/api/v1/objs/policy",
                "/api/v1/objs/policy/**",
            )
            .addOpenApiCustomizer(
                ObjsOpenApiTags.describe(
                    "catalog" to "Capabilities, seed export, categories, and policy CRUD",
                    "evaluate" to "Compile a policy body and evaluate a single policy",
                    "suites" to "Suite CRUD, selection preview, suite evaluate, and archive save",
                    "archives" to "Stored evaluation archives: list, load, delete",
                ),
            )
            .packagesToScan("org.poc.objs.policy.service.web")
            .build()
}
