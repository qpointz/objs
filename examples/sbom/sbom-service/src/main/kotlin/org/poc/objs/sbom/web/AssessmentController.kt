package org.poc.objs.sbom.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.sbom.assessment.SbomAssessmentService
import org.poc.objs.sbom.domain.ApplicationAssessmentResult
import org.poc.objs.sbom.domain.ApplicationAssessmentRunRequest
import org.poc.objs.sbom.domain.AssessmentSuiteSummary
import org.poc.objs.sbom.domain.PortfolioAssessmentMatrix
import org.poc.objs.sbom.domain.PortfolioAssessmentRunRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "assessment", description = "Run SBOM policy suites over an application or a portfolio")
class AssessmentController(
    private val assessment: SbomAssessmentService,
) {
    @GetMapping("/assessment/suites")
    @Operation(summary = "List assessment policy suites (SBOM demo packs)")
    @ApiResponse(
        responseCode = "200",
        description = "Suites available to run",
        content = [
            Content(array = ArraySchema(schema = Schema(implementation = AssessmentSuiteSummary::class))),
        ],
    )
    fun listSuites(): List<AssessmentSuiteSummary> = assessment.listSuites()

    @PostMapping("/applications/{applicationId}/assessment/run")
    @Operation(summary = "Run a policy suite against an application BOM (session result; not persisted)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Assessment outcome for the application",
            content = [Content(schema = Schema(implementation = ApplicationAssessmentResult::class))],
        ),
        ApiResponse(responseCode = "404", description = "Application, version, or suite not found"),
    )
    fun runApplication(
        @Parameter(description = "Application to assess") @PathVariable applicationId: UUID,
        @RequestBody body: ApplicationAssessmentRunRequest,
    ): ApplicationAssessmentResult =
        assessment.runApplication(applicationId, body.suiteId, body.versionId, body.bomIds)

    @PostMapping("/portfolios/{portfolioId}/assessment/run")
    @Operation(summary = "Run a policy suite for applications in the current portfolio level (matrix; not persisted)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Application-by-policy assessment matrix",
            content = [Content(schema = Schema(implementation = PortfolioAssessmentMatrix::class))],
        ),
        ApiResponse(responseCode = "404", description = "Portfolio or suite not found"),
    )
    fun runPortfolio(
        @Parameter(description = "Portfolio whose applications are assessed") @PathVariable portfolioId: UUID,
        @RequestBody body: PortfolioAssessmentRunRequest,
    ): PortfolioAssessmentMatrix =
        assessment.runPortfolio(
            portfolioId = portfolioId,
            suiteId = body.suiteId,
            applicationIds = body.applicationIds,
            level = body.level,
            includeSubcategories = body.includeSubcategories,
        )
}
