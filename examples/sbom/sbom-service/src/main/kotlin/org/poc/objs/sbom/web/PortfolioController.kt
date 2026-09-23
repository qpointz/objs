package org.poc.objs.sbom.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.sbom.domain.CategoryAssetPage
import org.poc.objs.sbom.domain.CreatePortfolioRequest
import org.poc.objs.sbom.domain.CreateSubjectAreaRequest
import org.poc.objs.sbom.domain.MiReportTable
import org.poc.objs.sbom.domain.DeletePlacementsRequest
import org.poc.objs.sbom.domain.MovePlacementsRequest
import org.poc.objs.sbom.domain.PlaceApplicationRequest
import org.poc.objs.sbom.domain.PortfolioLevelApps
import org.poc.objs.sbom.domain.PortfolioSummary
import org.poc.objs.sbom.domain.PortfolioTreeView
import org.poc.objs.sbom.domain.RunMiReportRequest
import org.poc.objs.sbom.domain.SubjectAreaView
import org.poc.objs.sbom.domain.UpdatePortfolioRequest
import org.poc.objs.sbom.domain.UpdateSubjectAreaRequest
import org.poc.objs.sbom.service.CategoryAssetsService
import org.poc.objs.sbom.service.MiReportService
import org.poc.objs.sbom.service.PortfolioService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/portfolios")
@Tag(name = "portfolios")
class PortfolioController(
    private val portfolios: PortfolioService,
    private val reports: MiReportService,
    private val categoryAssets: CategoryAssetsService,
) {
    @GetMapping
    @Operation(summary = "List portfolios")
    fun list(): List<PortfolioSummary> = portfolios.list()

    @PostMapping
    @Operation(summary = "Create a portfolio")
    fun create(@RequestBody body: CreatePortfolioRequest): PortfolioSummary =
        portfolios.create(body)

    @PatchMapping("/{id}")
    @Operation(summary = "Update portfolio name or description")
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: UpdatePortfolioRequest,
    ): PortfolioSummary = portfolios.update(id, body)

    @GetMapping("/{id}")
    @Operation(summary = "Fetch portfolio tree (subject areas and placements)")
    fun getTree(@PathVariable id: UUID): PortfolioTreeView =
        portfolios.getTree(id)

    @PostMapping("/{id}/subject-areas")
    @Operation(summary = "Add a subject area (category) under the portfolio")
    fun addSubjectArea(
        @PathVariable id: UUID,
        @RequestBody body: CreateSubjectAreaRequest,
    ): SubjectAreaView = portfolios.addSubjectArea(id, body)

    @PatchMapping("/{id}/subject-areas/{nodeId}")
    @Operation(summary = "Update a subject area")
    fun updateSubjectArea(
        @PathVariable id: UUID,
        @PathVariable nodeId: UUID,
        @RequestBody body: UpdateSubjectAreaRequest,
    ): SubjectAreaView = portfolios.updateSubjectArea(id, nodeId, body)

    @DeleteMapping("/{id}/subject-areas/{nodeId}")
    @Operation(summary = "Delete a subject area when empty")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Subject area deleted"),
    )
    fun deleteSubjectArea(
        @PathVariable id: UUID,
        @PathVariable nodeId: UUID,
    ) {
        portfolios.deleteSubjectArea(id, nodeId)
    }

    @PostMapping("/{id}/applications")
    @Operation(summary = "Place an application into a subject area")
    fun placeApplication(
        @PathVariable id: UUID,
        @RequestBody body: PlaceApplicationRequest,
    ): PortfolioTreeView = portfolios.placeApplication(id, body)

    @PostMapping("/{id}/placements")
    @Operation(
        summary = "Place an application (alias of POST …/applications)",
        description = "Same body and result as POST /{id}/applications.",
    )
    fun place(
        @PathVariable id: UUID,
        @RequestBody body: PlaceApplicationRequest,
    ): PortfolioTreeView = portfolios.placeApplication(id, body)

    @DeleteMapping("/{id}/placements/{placementId}")
    @Operation(summary = "Remove one placement by id")
    fun removePlacement(
        @PathVariable id: UUID,
        @PathVariable placementId: UUID,
    ): PortfolioTreeView = portfolios.removePlacement(id, placementId)

    @DeleteMapping("/{id}/applications/{applicationId}")
    @Operation(summary = "Remove all placements of an application from the portfolio")
    fun removeApplication(
        @PathVariable id: UUID,
        @PathVariable applicationId: UUID,
    ): PortfolioTreeView = portfolios.removeApplication(id, applicationId)

    @PostMapping("/{id}/placements/move")
    @Operation(summary = "Move placements to another subject area")
    fun movePlacements(
        @PathVariable id: UUID,
        @RequestBody body: MovePlacementsRequest,
    ): PortfolioTreeView = portfolios.movePlacements(id, body.placementIds, body.subjectAreaId)

    @PostMapping("/{id}/placements/delete")
    @Operation(summary = "Delete multiple placements")
    fun deletePlacements(
        @PathVariable id: UUID,
        @RequestBody body: DeletePlacementsRequest,
    ): PortfolioTreeView = portfolios.removePlacements(id, body.placementIds)

    @GetMapping("/{id}/applications")
    @Operation(summary = "Paged applications for a portfolio level")
    fun applicationsForLevel(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "root") level: String,
        @RequestParam(defaultValue = "true") includeSubcategories: Boolean,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(required = false) q: String?,
    ): PortfolioLevelApps =
        portfolios.applicationsForLevel(id, level, includeSubcategories, page, size, q)

    @GetMapping("/{id}/assets")
    @Operation(summary = "Paged assets for applications in a portfolio level")
    fun assets(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "root") level: String,
        @RequestParam(defaultValue = "true") includeSubcategories: Boolean,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): CategoryAssetPage =
        categoryAssets.list(id, level, includeSubcategories, page, size)

    @PostMapping("/{id}/reports")
    @Operation(summary = "Run an MI report table for a portfolio level")
    fun runReport(
        @PathVariable id: UUID,
        @RequestBody body: RunMiReportRequest,
    ): MiReportTable = reports.runTable(id, body)

    @GetMapping("/{id}/reports/{report}.csv", produces = [MediaType.TEXT_PLAIN_VALUE])
    @Operation(summary = "Export an MI report as CSV")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "CSV attachment",
        ),
    )
    fun exportCsv(
        @PathVariable id: UUID,
        @PathVariable report: String,
        @RequestParam(defaultValue = "root") level: String,
        @RequestParam(defaultValue = "true") includeSubcategories: Boolean,
        @RequestParam(defaultValue = "LATEST") versionResolution: String,
    ): ResponseEntity<String> {
        val csv =
            reports.csv(
                id,
                RunMiReportRequest(
                    level = level,
                    includeSubcategories = includeSubcategories,
                    report = report,
                    versionResolution = versionResolution,
                ),
            )
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$report.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv)
    }
}
