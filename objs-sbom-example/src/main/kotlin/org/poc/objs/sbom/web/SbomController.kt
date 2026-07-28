package org.poc.objs.sbom.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.model.SbomApplicationCatalog
import org.poc.objs.sbom.service.SbomService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody as HttpRequestBody

@RestController
@RequestMapping("/api/v1/example/sbom")
@Tag(name = "example-sbom")
class SbomController(
    private val sbom: SbomService,
) {
    @GetMapping("/apps")
    @Operation(summary = "List applications and their SBOM versions")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Distinct app / version partitions from entity annotations",
            content = [Content(schema = Schema(implementation = SbomApplicationCatalog::class))],
        ),
    )
    fun listApps(): SbomApplicationCatalog = sbom.listApplications()

    @GetMapping("/apps/{appId}")
    @Operation(
        summary = "Fetch SBOM subgraph for an application (all versions unless filtered)",
        parameters = [
            Parameter(
                name = "source",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Annotation filter: manual | detected | enriched",
            ),
            Parameter(
                name = "origin",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Annotation filter: caller channel (ui, batch, api, …)",
            ),
            Parameter(
                name = "sourceDetail",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Annotation filter: catalog id or detector detail",
            ),
            Parameter(
                name = "capturedBy",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Annotation filter: user id for manual captures",
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Induced subgraph",
            content = [Content(schema = Schema(ref = "#/components/schemas/SbomSubgraph"))],
        ),
    )
    fun getByApp(
        @PathVariable appId: String,
        @Parameter(hidden = true) @RequestParam params: Map<String, String>,
    ): BoMSubgraph {
        return sbom.getSbom(app = appId, appVersion = null, extra = SbomQueryAnnotations.fromRequestParams(params))
    }

    @GetMapping("/apps/{appId}/versions/{version}")
    @Operation(
        summary = "Fetch SBOM subgraph for an application version",
        parameters = [
            Parameter(
                name = "source",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Annotation filter: manual | detected | enriched",
            ),
            Parameter(
                name = "origin",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Annotation filter: caller channel (ui, batch, api, …)",
            ),
            Parameter(
                name = "sourceDetail",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Annotation filter: catalog id or detector detail",
            ),
            Parameter(
                name = "capturedBy",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Annotation filter: user id for manual captures",
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Induced subgraph",
            content = [Content(schema = Schema(ref = "#/components/schemas/SbomSubgraph"))],
        ),
    )
    fun getByAppVersion(
        @PathVariable appId: String,
        @PathVariable version: String,
        @Parameter(hidden = true) @RequestParam params: Map<String, String>,
    ): BoMSubgraph {
        return sbom.getSbom(
            app = appId,
            appVersion = version,
            extra = SbomQueryAnnotations.fromRequestParams(params),
        )
    }

    @PutMapping("/apps/{appId}/versions/{version}")
    @Operation(
        summary = "Upsert SBOM graph for an application version",
        parameters = [
            Parameter(
                name = "source",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Default annotation on entities: manual | detected | enriched",
            ),
            Parameter(
                name = "origin",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Default annotation: caller channel (ui, batch, api, …)",
            ),
            Parameter(
                name = "sourceDetail",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Default annotation: catalog id or detector detail",
            ),
            Parameter(
                name = "capturedBy",
                `in` = ParameterIn.QUERY,
                required = false,
                description = "Default annotation: user id for manual captures",
            ),
        ],
    )
    @RequestBody(
        required = true,
        description = "Graph batch; entity payloads are oneOf registered domain schemas",
        content = [Content(schema = Schema(ref = "#/components/schemas/SbomGraph"))],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Upserted graph",
            content = [Content(schema = Schema(ref = "#/components/schemas/SbomGraph"))],
        ),
        ApiResponse(responseCode = "400", description = "Validation failed"),
    )
    fun putByAppVersion(
        @PathVariable appId: String,
        @PathVariable version: String,
        @Parameter(hidden = true) @RequestParam params: Map<String, String>,
        @HttpRequestBody graph: BoMGraph,
    ): ResponseEntity<Any> {
        val result = sbom.save(
            context = SbomContext(appId, version),
            graph = graph,
            requestAnnotations = SbomQueryAnnotations.fromRequestParams(params),
        )
        return if (result.isValid) {
            ResponseEntity.ok(graph)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}
