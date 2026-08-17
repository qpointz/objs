package org.poc.objs.sbom.web

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InventoryOpenApiCustomizerTest {
    @Test
    fun shouldDocumentCombinedSbomReadOnlyAndFingerprintCategory() {
        val openApi =
            OpenAPI()
                .components(
                    Components().addSchemas(
                        "CreateFingerprintRequest",
                        ObjectSchema().addProperty("category", StringSchema()),
                    ),
                )
                .paths(
                    Paths().addPathItem(
                        "/api/v1/inventory/applications/{id}/versions/{versionId}/combined",
                        PathItem().put(Operation().responses(ApiResponses())),
                    ),
                )
        InventoryOpenApiCustomizer().customise(openApi)

        assertThat(openApi.info.description).contains("Combined SBOM")
        assertThat(
            openApi.paths["/api/v1/inventory/applications/{id}/versions/{versionId}/combined"]!!
                .put
                .responses["405"]!!
                .description,
        ).contains("read-only")
        assertThat(openApi.components.schemas["CreateFingerprintRequest"]!!.properties["category"]!!.enum)
            .containsExactly("approval", "history", "unknown")
    }
}
