package org.poc.objs.sbom.web

import io.swagger.v3.oas.annotations.Operation
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
@Tag(name = "assessment")
class AssessmentController(
    private val assessment: SbomAssessmentService,
) {
    @GetMapping("/assessment/suites")
    @Operation(summary = "List assessment policy suites (SBOM demo packs)")
    fun listSuites(): List<AssessmentSuiteSummary> = assessment.listSuites()

    @PostMapping("/applications/{applicationId}/assessment/run")
    @Operation(summary = "Run a policy suite against an application BOM (session result; not persisted)")
    fun runApplication(
        @PathVariable applicationId: UUID,
        @RequestBody body: ApplicationAssessmentRunRequest,
    ): ApplicationAssessmentResult =
        assessment.runApplication(applicationId, body.suiteId, body.versionId, body.bomIds)

    @PostMapping("/portfolios/{portfolioId}/assessment/run")
    @Operation(summary = "Run a policy suite for applications in the current portfolio level (matrix; not persisted)")
    fun runPortfolio(
        @PathVariable portfolioId: UUID,
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
