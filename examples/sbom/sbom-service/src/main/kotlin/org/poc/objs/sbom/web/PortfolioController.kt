package org.poc.objs.sbom.web

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
class PortfolioController(
    private val portfolios: PortfolioService,
    private val reports: MiReportService,
    private val categoryAssets: CategoryAssetsService,
) {
    @GetMapping
    fun list(): List<PortfolioSummary> = portfolios.list()

    @PostMapping
    fun create(@RequestBody body: CreatePortfolioRequest): PortfolioSummary =
        portfolios.create(body)

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: UpdatePortfolioRequest,
    ): PortfolioSummary = portfolios.update(id, body)

    @GetMapping("/{id}")
    fun getTree(@PathVariable id: UUID): PortfolioTreeView =
        portfolios.getTree(id)

    @PostMapping("/{id}/subject-areas")
    fun addSubjectArea(
        @PathVariable id: UUID,
        @RequestBody body: CreateSubjectAreaRequest,
    ): SubjectAreaView = portfolios.addSubjectArea(id, body)

    @PatchMapping("/{id}/subject-areas/{nodeId}")
    fun updateSubjectArea(
        @PathVariable id: UUID,
        @PathVariable nodeId: UUID,
        @RequestBody body: UpdateSubjectAreaRequest,
    ): SubjectAreaView = portfolios.updateSubjectArea(id, nodeId, body)

    @DeleteMapping("/{id}/subject-areas/{nodeId}")
    fun deleteSubjectArea(
        @PathVariable id: UUID,
        @PathVariable nodeId: UUID,
    ) {
        portfolios.deleteSubjectArea(id, nodeId)
    }

    @PostMapping("/{id}/applications")
    fun placeApplication(
        @PathVariable id: UUID,
        @RequestBody body: PlaceApplicationRequest,
    ): PortfolioTreeView = portfolios.placeApplication(id, body)

    @PostMapping("/{id}/placements")
    fun place(
        @PathVariable id: UUID,
        @RequestBody body: PlaceApplicationRequest,
    ): PortfolioTreeView = portfolios.placeApplication(id, body)

    @DeleteMapping("/{id}/placements/{placementId}")
    fun removePlacement(
        @PathVariable id: UUID,
        @PathVariable placementId: UUID,
    ): PortfolioTreeView = portfolios.removePlacement(id, placementId)

    @DeleteMapping("/{id}/applications/{applicationId}")
    fun removeApplication(
        @PathVariable id: UUID,
        @PathVariable applicationId: UUID,
    ): PortfolioTreeView = portfolios.removeApplication(id, applicationId)

    @PostMapping("/{id}/placements/move")
    fun movePlacements(
        @PathVariable id: UUID,
        @RequestBody body: MovePlacementsRequest,
    ): PortfolioTreeView = portfolios.movePlacements(id, body.placementIds, body.subjectAreaId)

    @PostMapping("/{id}/placements/delete")
    fun deletePlacements(
        @PathVariable id: UUID,
        @RequestBody body: DeletePlacementsRequest,
    ): PortfolioTreeView = portfolios.removePlacements(id, body.placementIds)

    @GetMapping("/{id}/applications")
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
    fun assets(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "root") level: String,
        @RequestParam(defaultValue = "true") includeSubcategories: Boolean,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): CategoryAssetPage =
        categoryAssets.list(id, level, includeSubcategories, page, size)

    @PostMapping("/{id}/reports")
    fun runReport(
        @PathVariable id: UUID,
        @RequestBody body: RunMiReportRequest,
    ): MiReportTable = reports.runTable(id, body)

    @GetMapping("/{id}/reports/{report}.csv", produces = [MediaType.TEXT_PLAIN_VALUE])
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
