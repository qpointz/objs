package org.poc.objs.sbom.web

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.sbom.registry.SbomRegistry

class SbomDomainOpenApiCustomizerTest {

    @Test
    fun shouldPublishCatalogSchemasAndWirePutRequestBody() {
        val schemas = BoMSchemaCatalog()
        val edges = BoMAllowedEdgeCatalog()
        SbomRegistry.pack().registerInto(schemas, edges)

        val customizer = SbomDomainOpenApiCustomizer(schemas, edges)
        val openApi = OpenAPI().paths(
            Paths().addPathItem(
                "/api/v1/example/sbom/apps/{appId}/versions/{version}",
                PathItem().put(
                    Operation()
                        .requestBody(
                            RequestBody().content(
                                Content().addMediaType(
                                    "application/json",
                                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/BoMGraph")),
                                ),
                            ),
                        )
                        .responses(
                            ApiResponses().addApiResponse(
                                "200",
                                ApiResponse().content(
                                    Content().addMediaType(
                                        "application/json",
                                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/BoMGraph")),
                                    ),
                                ),
                            ),
                        ),
                ),
            ),
        )
        customizer.customise(openApi)

        assertThat(openApi.components.schemas.keys)
            .contains(
                "Component.1.0.0",
                "Container Image.1.0.0",
                "Kubernetes Cluster.1.0.0",
                "SbomGraph",
                "SbomEntity",
                "SbomEdge",
            )
        assertThat(openApi.components.schemas["Component.1.0.0"]!!.required)
            .contains("name", "version", "ecosystem", "kind")

        val putBody = openApi.paths
            .get("/api/v1/example/sbom/apps/{appId}/versions/{version}")!!
            .put
            .requestBody
            .content["application/json"]!!
            .schema
        assertThat(putBody.`$ref`).isEqualTo("#/components/schemas/SbomGraph")

        val entityPayload = openApi.components.schemas["SbomEntity"]!!.properties["payload"]!!
        assertThat(entityPayload.oneOf).isNotEmpty
        assertThat(entityPayload.oneOf.map { it.`$ref` }).anyMatch { it.contains("Component.1.0.0") }
    }
}
