package org.poc.objs.service.web

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * SpringDoc groups (qpointz / mill-service-common style).
 */
@Configuration
class ObjsOpenApiConfiguration {

    @Bean
    fun objsGraphApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("graph")
            .pathsToMatch("/api/v1/objs/graph", "/api/v1/objs/graph/**", "/api/v1/objs/status")
            .build()

    @Bean
    fun objsRegistryApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("registry")
            .pathsToMatch("/api/v1/objs/registry/**")
            .build()

    @Bean
    fun objsSeedsApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("seeds")
            .pathsToMatch("/api/v1/objs/seeds/**")
            .build()
}
