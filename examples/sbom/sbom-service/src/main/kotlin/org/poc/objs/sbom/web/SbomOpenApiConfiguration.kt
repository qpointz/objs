package org.poc.objs.sbom.web

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SbomOpenApiConfiguration {
    @Bean
    fun exampleSbomApi(domainSchemas: SbomDomainOpenApiCustomizer): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("example-sbom")
            .pathsToMatch("/api/v1/example/sbom", "/api/v1/example/sbom/**")
            .addOpenApiCustomizer(domainSchemas)
            .build()

    @Bean
    fun inventoryApi(inventoryOpenApi: InventoryOpenApiCustomizer): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("inventory")
            .pathsToMatch("/api/v1/inventory", "/api/v1/inventory/**")
            .addOpenApiCustomizer(inventoryOpenApi)
            .build()
}
