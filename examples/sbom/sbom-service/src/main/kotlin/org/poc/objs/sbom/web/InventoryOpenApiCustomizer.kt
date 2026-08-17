package org.poc.objs.sbom.web

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

/**
 * Product notes for the inventory group: Combined SBOM is ephemeral; fingerprints have a category enum.
 */
@Component
class InventoryOpenApiCustomizer : OpenApiCustomizer {
    override fun customise(openApi: OpenAPI) {
        val info = openApi.info ?: Info()
        val extra = info.description?.takeIf { it.isNotBlank() }
        openApi.info =
            info.description(
                listOfNotNull(extra, DESCRIPTION).joinToString("\n\n"),
            )
        openApi.paths
            ?.get("/api/v1/inventory/applications/{id}/versions/{versionId}/combined")
            ?.put
            ?.let { operation ->
                val responses = operation.responses ?: ApiResponses().also { operation.responses = it }
                responses.addApiResponse(
                    "405",
                    ApiResponse().description("Combined SBOM is ephemeral and read-only"),
                )
            }
        openApi.components
            ?.schemas
            ?.get("CreateFingerprintRequest")
            ?.properties
            ?.get("category")
            ?.setEnum(listOf("approval", "history", "unknown"))
    }

    companion object {
        const val DESCRIPTION =
            "Application inventory uses product language: BOM constituents, ephemeral Combined SBOM " +
                "(read-only union), tags on app/version/BOM, and fingerprints with name plus category " +
                "(approval | history | unknown). Portal stats are lazy per application."
    }
}
