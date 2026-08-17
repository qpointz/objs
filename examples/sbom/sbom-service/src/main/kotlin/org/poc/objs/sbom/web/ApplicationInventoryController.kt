package org.poc.objs.sbom.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.sbom.domain.ApplicationFingerprintSummary
import org.poc.objs.sbom.domain.ApplicationPortalStats
import org.poc.objs.sbom.domain.ApplicationSummary
import org.poc.objs.sbom.domain.ApplicationVersionSummary
import org.poc.objs.sbom.domain.BomSummary
import org.poc.objs.sbom.domain.CombinedBomView
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.CreateBomRequest
import org.poc.objs.sbom.domain.CreateDraftVersionRequest
import org.poc.objs.sbom.domain.CreateFingerprintRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.DraftRelationWrite
import org.poc.objs.sbom.domain.InferredAppDependency
import org.poc.objs.sbom.domain.PatchVersionRequest
import org.poc.objs.sbom.domain.PromoteVersionRequest
import org.poc.objs.sbom.domain.ReplaceVersionBomRequest
import org.poc.objs.sbom.domain.UpdateApplicationRequest
import org.poc.objs.sbom.domain.UpdateBomRequest
import org.poc.objs.sbom.domain.VersionBomView
import org.poc.objs.sbom.service.ApplicationBomService
import org.poc.objs.sbom.service.ApplicationInventoryService
import org.poc.objs.sbom.service.ApplicationVersionService
import org.poc.objs.sbom.service.CycloneDxExportService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/applications")
@Tag(name = "inventory")
class ApplicationInventoryController(
    private val inventory: ApplicationInventoryService,
    private val versions: ApplicationVersionService,
    private val boms: ApplicationBomService,
    private val cycloneDx: CycloneDxExportService,
) {
    @GetMapping
    fun search(@RequestParam(required = false) q: String?): List<ApplicationSummary> =
        inventory.search(q)

    @PostMapping
    @Operation(summary = "Create an application with a required target version and one empty BOM")
    fun create(@RequestBody body: CreateApplicationRequest): ApplicationSummary {
        if (body.targetVersion.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "targetVersion is required")
        }
        return inventory.create(body)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ApplicationSummary =
        inventory.get(id)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: UpdateApplicationRequest,
    ): ApplicationSummary = inventory.update(id, body)

    @GetMapping("/{id}/stats")
    @Operation(summary = "Lazy portal stats for one application (versions, BOMs, latest RELEASED)")
    fun stats(@PathVariable id: UUID): ApplicationPortalStats = inventory.portalStats(id)

    @GetMapping("/{id}/depends-on")
    fun dependsOn(@PathVariable id: UUID): List<InferredAppDependency> =
        inventory.inferDependsOn(id)

    @GetMapping("/{id}/versions")
    fun listVersions(@PathVariable id: UUID): List<ApplicationVersionSummary> =
        versions.list(id)

    @PostMapping("/{id}/versions")
    @Operation(summary = "Create a DRAFT from a version or fingerprint, with a unique target version")
    fun createDraft(
        @PathVariable id: UUID,
        @RequestBody(required = false) body: CreateDraftVersionRequest?,
    ): VersionBomView {
        val request = body ?: CreateDraftVersionRequest()
        if (request.targetVersion.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "targetVersion is required")
        }
        if (request.fromVersionId == null && request.fromFingerprintId == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "fromVersionId or fromFingerprintId is required")
        }
        if (request.fromVersionId != null && request.fromFingerprintId != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide fromVersionId or fromFingerprintId, not both")
        }
        return versions.createDraft(id, request)
    }

    @GetMapping("/{id}/versions/latest")
    fun latestVersion(@PathVariable id: UUID): ApplicationVersionSummary =
        versions.latest(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No released versions for application: $id")

    @GetMapping("/{id}/versions/{versionId}")
    @Operation(summary = "Version metadata plus Combined SBOM (ephemeral union of all BOMs)")
    fun getVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): VersionBomView {
        val combined = boms.combined(id, versionId)
        return VersionBomView(
            version = combined.version,
            applicationName = combined.applicationName,
            assets = combined.assets,
            relations = combined.relations,
            combinedTags = combined.combinedTags,
        )
    }

    @PatchMapping("/{id}/versions/{versionId}")
    @Operation(summary = "Patch DRAFT target version and/or tags")
    fun patchVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: PatchVersionRequest,
    ): ApplicationVersionSummary = versions.patchDraft(id, versionId, body)

    @DeleteMapping("/{id}/versions/{versionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a DRAFT and cascade dependent drafts after confirm")
    fun deleteVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestParam(defaultValue = "false") confirmDependents: Boolean,
    ) {
        versions.deleteDraft(id, versionId, confirmDependents)
    }

    @GetMapping("/{id}/versions/{versionId}/dependents")
    @Operation(summary = "List drafts that would be deleted with this DRAFT")
    fun deleteImpact(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): List<ApplicationVersionSummary> = versions.deleteImpact(id, versionId)

    @GetMapping("/{id}/versions/{versionId}/combined")
    @Operation(summary = "Ephemeral Combined SBOM union (optionally a selected BOM subset)")
    fun combined(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestParam(required = false) sbomIds: List<UUID>?,
    ): CombinedBomView = boms.combined(id, versionId, sbomIds)

    @PutMapping("/{id}/versions/{versionId}/combined")
    @Operation(summary = "Combined SBOM is read-only; writes are rejected with 405")
    fun rejectCombinedPut(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody(required = false) body: Map<String, Any?>?,
    ): Nothing = throw ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Combined SBOM is read-only")

    @GetMapping("/{id}/versions/{versionId}/sboms")
    @Operation(summary = "List constituent BOMs for a version")
    fun listBoms(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): List<BomSummary> = boms.list(id, versionId)

    @PostMapping("/{id}/versions/{versionId}/sboms")
    @Operation(summary = "Create a constituent BOM on a DRAFT")
    fun createBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: CreateBomRequest,
    ): BomSummary = boms.create(id, versionId, body)

    @GetMapping("/{id}/versions/{versionId}/sboms/{sbomId}")
    @Operation(summary = "Get one constituent BOM graph")
    fun getBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable sbomId: UUID,
    ): VersionBomView = boms.get(id, versionId, sbomId)

    @PutMapping("/{id}/versions/{versionId}/sboms/{sbomId}")
    @Operation(summary = "Replace assets and relations on one DRAFT BOM")
    fun putBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable sbomId: UUID,
        @RequestBody body: ReplaceVersionBomRequest,
    ): VersionBomView = versions.replaceBom(id, versionId, body, sbomId)

    @PatchMapping("/{id}/versions/{versionId}/sboms/{sbomId}")
    @Operation(summary = "Patch BOM name, description, or tags")
    fun patchBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable sbomId: UUID,
        @RequestBody body: UpdateBomRequest,
    ): BomSummary = boms.update(id, versionId, sbomId, body)

    @DeleteMapping("/{id}/versions/{versionId}/sboms/{sbomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a constituent BOM (not the last one)")
    fun deleteBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable sbomId: UUID,
    ) {
        boms.delete(id, versionId, sbomId)
    }

    @PutMapping("/{id}/versions/{versionId}")
    fun saveBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: ReplaceVersionBomRequest,
    ): VersionBomView = versions.replaceBom(id, versionId, body)

    @PostMapping("/{id}/versions/{versionId}/promote")
    fun promote(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: PromoteVersionRequest,
    ): VersionBomView = versions.promote(id, versionId, body)

    @PostMapping("/{id}/versions/{versionId}/assets")
    fun addAsset(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: DraftAssetWrite,
    ): VersionBomView = versions.addAsset(id, versionId, body)

    @DeleteMapping("/{id}/versions/{versionId}/assets/{assetId}")
    fun removeAsset(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable assetId: UUID,
    ): VersionBomView = versions.removeAsset(id, versionId, assetId)

    @PostMapping("/{id}/versions/{versionId}/relations")
    fun addRelation(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: DraftRelationWrite,
    ): VersionBomView = versions.addRelation(id, versionId, body)

    @DeleteMapping("/{id}/versions/{versionId}/relations/{relationId}")
    fun removeRelation(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable relationId: UUID,
    ): VersionBomView = versions.removeRelation(id, versionId, relationId)

    @GetMapping("/{id}/versions/{versionId}/depends-on")
    fun versionDependsOn(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): List<InferredAppDependency> = versions.inferDependsOn(id, versionId)

    @GetMapping("/{id}/versions/{versionId}/fingerprints")
    fun listFingerprints(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): List<ApplicationFingerprintSummary> = versions.listFingerprints(id, versionId)

    @GetMapping("/{id}/versions/{versionId}/fingerprints/{fingerprintId}")
    fun getFingerprint(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable fingerprintId: UUID,
    ): VersionBomView = versions.getFingerprintBom(id, versionId, fingerprintId)

    @PostMapping("/{id}/versions/{versionId}/fingerprints")
    @Operation(summary = "Snapshot the full Combined SBOM as a named fingerprint")
    fun createFingerprint(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody(required = false) body: CreateFingerprintRequest?,
    ): ApplicationFingerprintSummary {
        val request = body ?: CreateFingerprintRequest()
        if (request.name.isNullOrBlank() && request.note.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required")
        }
        if (request.category.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required")
        }
        return versions.fingerprint(id, versionId, request)
    }

    @PutMapping("/{id}/versions/{versionId}/fingerprints/{fingerprintId}")
    fun rejectFingerprintPut(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable fingerprintId: UUID,
        @RequestBody(required = false) body: ReplaceVersionBomRequest?,
    ): VersionBomView = versions.rejectFingerprintWrite(id, versionId, fingerprintId)

    @DeleteMapping("/{id}/versions/{versionId}/fingerprints/{fingerprintId}")
    fun rejectFingerprintDelete(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable fingerprintId: UUID,
    ): VersionBomView = versions.rejectFingerprintWrite(id, versionId, fingerprintId)

    @PostMapping("/{id}/versions/{versionId}/fingerprints/{fingerprintId}")
    fun rejectFingerprintPost(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable fingerprintId: UUID,
        @RequestBody(required = false) body: Map<String, Any?>?,
    ): VersionBomView = versions.rejectFingerprintWrite(id, versionId, fingerprintId)

    @GetMapping("/{id}/versions/{versionId}/export/cyclonedx")
    fun exportVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): ResponseEntity<Map<String, Any?>> =
        cdxResponse(
            cycloneDx.exportVersion(id, versionId),
            "application-$id-version-$versionId.cdx.json",
        )

    private fun cdxResponse(body: Map<String, Any?>, filename: String): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
}
