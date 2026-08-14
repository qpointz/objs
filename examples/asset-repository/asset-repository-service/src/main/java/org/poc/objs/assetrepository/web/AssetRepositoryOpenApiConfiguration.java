package org.poc.objs.assetrepository.web;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssetRepositoryOpenApiConfiguration {

    @Bean
    public GroupedOpenApi assetRepositoryApi() {
        return GroupedOpenApi.builder()
                .group("asset-repository")
                .displayName("Asset repository")
                .pathsToMatch("/api/v1/asset-repository", "/api/v1/asset-repository/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                        .title("Asset repository API")
                        .description(
                                "Domain REST for collections, objects, compositions, search, and schema reads. "
                                        + "Use this group — not foundation /api/v1/objs/** — from the domain SPA and Python client.")
                        .version("v1")))
                .build();
    }
}
