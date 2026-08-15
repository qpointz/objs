package org.poc.objs.sbom.web

import org.poc.objs.sbom.domain.ApplicationFingerprintSummary
import org.poc.objs.sbom.domain.ApplicationSummary
import org.poc.objs.sbom.domain.ApplicationVersionSummary
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.CreateDraftVersionRequest
import org.poc.objs.sbom.domain.CreateFingerprintRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.DraftRelationWrite
import org.poc.objs.sbom.domain.InferredAppDependency
import org.poc.objs.sbom.domain.PromoteVersionRequest
import org.poc.objs.sbom.domain.ReplaceVersionBomRequest
import org.poc.objs.sbom.domain.UpdateApplicationRequest
import org.poc.objs.sbom.domain.VersionBomView
import org.poc.objs.sbom.service.ApplicationInventoryService
import org.poc.objs.sbom.service.ApplicationVersionService
import org.poc.objs.sbom.service.CycloneDxExportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/applications")
class ApplicationInventoryController(
    private val inventory: ApplicationInventoryService,
    private val versions: ApplicationVersionService,
    private val cycloneDx: CycloneDxExportService,
) {
    @GetMapping
    fun search(@RequestParam(required = false) q: String?): List<ApplicationSummary> =
        inventory.search(q)

    @PostMapping
    fun create(@RequestBody body: CreateApplicationRequest): ApplicationSummary =
        inventory.create(body)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ApplicationSummary =
        inventory.get(id)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: UpdateApplicationRequest,
    ): ApplicationSummary = inventory.update(id, body)

    @GetMapping("/{id}/depends-on")
    fun dependsOn(@PathVariable id: UUID): List<InferredAppDependency> =
        inventory.inferDependsOn(id)

    @GetMapping("/{id}/versions")
    fun listVersions(@PathVariable id: UUID): List<ApplicationVersionSummary> =
        versions.list(id)

    @PostMapping("/{id}/versions")
    fun createDraft(
        @PathVariable id: UUID,
        @RequestBody(required = false) body: CreateDraftVersionRequest?,
    ): VersionBomView = versions.createDraft(id, body ?: CreateDraftVersionRequest())

    @GetMapping("/{id}/versions/latest")
    fun latestVersion(@PathVariable id: UUID): ApplicationVersionSummary =
        versions.latest(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No released versions for application: $id")

    @GetMapping("/{id}/versions/{versionId}")
    fun getVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): VersionBomView = versions.getBom(id, versionId)

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
    fun createFingerprint(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody(required = false) body: CreateFingerprintRequest?,
    ): ApplicationFingerprintSummary = versions.fingerprint(id, versionId, body ?: CreateFingerprintRequest())

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
