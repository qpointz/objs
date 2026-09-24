package org.poc.objs.sbom.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
import org.poc.objs.sbom.domain.PayloadRepresentation
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
class ApplicationInventoryController(
    private val inventory: ApplicationInventoryService,
    private val versions: ApplicationVersionService,
    private val boms: ApplicationBomService,
    private val cycloneDx: CycloneDxExportService,
) {
    @GetMapping
    @Operation(tags = ["applications"], summary = "Search or list applications")
    fun search(@RequestParam(required = false) q: String?): List<ApplicationSummary> =
        inventory.search(q)

    @PostMapping
    @Operation(
        tags = ["applications"],
        summary = "Create an application with a required target version and one empty BOM",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Created application",
            content = [Content(schema = Schema(implementation = ApplicationSummary::class))],
        ),
    )
    fun create(@RequestBody body: CreateApplicationRequest): ApplicationSummary {
        if (body.targetVersion.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "targetVersion is required")
        }
        return inventory.create(body)
    }

    @GetMapping("/{id}")
    @Operation(tags = ["applications"], summary = "Fetch an application by id")
    fun get(@PathVariable id: UUID): ApplicationSummary =
        inventory.get(id)

    @PutMapping("/{id}")
    @Operation(tags = ["applications"], summary = "Update application name, description, or tags")
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: UpdateApplicationRequest,
    ): ApplicationSummary = inventory.update(id, body)

    @GetMapping("/{id}/stats")
    @Operation(
        tags = ["applications"],
        summary = "Lazy portal stats for one application (versions, BOMs, latest RELEASED)",
    )
    fun stats(@PathVariable id: UUID): ApplicationPortalStats = inventory.portalStats(id)

    @GetMapping("/{id}/depends-on")
    @Operation(
        tags = ["applications"],
        summary = "Inferred application dependencies from the latest released BOM",
    )
    fun dependsOn(@PathVariable id: UUID): List<InferredAppDependency> =
        inventory.inferDependsOn(id)

    @GetMapping("/{id}/versions")
    @Operation(tags = ["application-versions"], summary = "List application versions")
    fun listVersions(@PathVariable id: UUID): List<ApplicationVersionSummary> =
        versions.list(id)

    @PostMapping("/{id}/versions")
    @Operation(
        tags = ["application-versions"],
        summary = "Create a DRAFT from a version or fingerprint, with a unique target version",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Draft version with Combined SBOM view",
            content = [Content(schema = Schema(implementation = VersionBomView::class))],
        ),
    )
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
    @Operation(tags = ["application-versions"], summary = "Latest RELEASED version metadata")
    fun latestVersion(@PathVariable id: UUID): ApplicationVersionSummary =
        versions.latest(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No released versions for application: $id")

    @GetMapping("/{id}/versions/{versionId}")
    @Operation(
        tags = ["application-versions"],
        summary = "Version metadata plus Combined SBOM (ephemeral union of all BOMs)",
    )
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
    @Operation(tags = ["application-versions"], summary = "Patch DRAFT target version and/or tags")
    fun patchVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: PatchVersionRequest,
    ): ApplicationVersionSummary = versions.patchDraft(id, versionId, body)

    @DeleteMapping("/{id}/versions/{versionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        tags = ["application-versions"],
        summary = "Delete a DRAFT and cascade dependent drafts after confirm",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Draft deleted"),
    )
    fun deleteVersion(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestParam(defaultValue = "false") confirmDependents: Boolean,
    ) {
        versions.deleteDraft(id, versionId, confirmDependents)
    }

    @GetMapping("/{id}/versions/{versionId}/dependents")
    @Operation(
        tags = ["application-versions"],
        summary = "List drafts that would be deleted with this DRAFT",
    )
    fun deleteImpact(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): List<ApplicationVersionSummary> = versions.deleteImpact(id, versionId)

    @GetMapping("/{id}/versions/{versionId}/combined")
    @Operation(
        tags = ["application-versions"],
        summary = "Ephemeral Combined SBOM union (optionally a selected BOM subset)",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Combined SBOM view",
            content = [Content(schema = Schema(implementation = CombinedBomView::class))],
        ),
    )
    fun combined(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestParam(required = false) sbomIds: List<UUID>?,
    ): CombinedBomView = boms.combined(id, versionId, sbomIds)

    @PutMapping("/{id}/versions/{versionId}/combined")
    @Operation(
        tags = ["application-versions"],
        summary = "Combined SBOM is read-only; writes are rejected with 405",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "405",
            description = "Combined SBOM is ephemeral and read-only",
        ),
    )
    fun rejectCombinedPut(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody(required = false) body: Map<String, Any?>?,
    ): Nothing = throw ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Combined SBOM is read-only")

    @GetMapping("/{id}/versions/{versionId}/sboms")
    @Operation(tags = ["application-sboms"], summary = "List constituent BOMs for a version")
    fun listBoms(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): List<BomSummary> = boms.list(id, versionId)

    @PostMapping("/{id}/versions/{versionId}/sboms")
    @Operation(tags = ["application-sboms"], summary = "Create a constituent BOM on a DRAFT")
    fun createBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: CreateBomRequest,
    ): BomSummary = boms.create(id, versionId, body)

    @GetMapping("/{id}/versions/{versionId}/sboms/{sbomId}")
    @Operation(tags = ["application-sboms"], summary = "Get one constituent BOM graph")
    fun getBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable sbomId: UUID,
    ): VersionBomView = boms.get(id, versionId, sbomId)

    @PutMapping("/{id}/versions/{versionId}/sboms/{sbomId}")
    @Operation(tags = ["application-sboms"], summary = "Replace assets and relations on one DRAFT BOM")
    fun putBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable sbomId: UUID,
        @RequestBody body: ReplaceVersionBomRequest,
    ): VersionBomView = versions.replaceBom(id, versionId, body, sbomId)

    @PatchMapping("/{id}/versions/{versionId}/sboms/{sbomId}")
    @Operation(tags = ["application-sboms"], summary = "Patch BOM name, description, or tags")
    fun patchBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable sbomId: UUID,
        @RequestBody body: UpdateBomRequest,
    ): BomSummary = boms.update(id, versionId, sbomId, body)

    @DeleteMapping("/{id}/versions/{versionId}/sboms/{sbomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(tags = ["application-sboms"], summary = "Delete a constituent BOM (not the last one)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "BOM deleted"),
    )
    fun deleteBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable sbomId: UUID,
    ) {
        boms.delete(id, versionId, sbomId)
    }

    @PutMapping("/{id}/versions/{versionId}")
    @Operation(
        tags = ["application-sboms"],
        summary = "Replace the primary DRAFT BOM graph",
        description = "Writes assets and relations onto the version's default/primary BOM.",
    )
    fun saveBom(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: ReplaceVersionBomRequest,
    ): VersionBomView = versions.replaceBom(id, versionId, body)

    @PostMapping("/{id}/versions/{versionId}/promote")
    @Operation(tags = ["application-versions"], summary = "Promote a DRAFT to RELEASED")
    fun promote(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: PromoteVersionRequest,
    ): VersionBomView = versions.promote(id, versionId, body)

    @PostMapping("/{id}/versions/{versionId}/assets")
    @Operation(tags = ["application-sboms"], summary = "Add an asset to the DRAFT primary BOM")
    fun addAsset(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: DraftAssetWrite,
    ): VersionBomView = versions.addAsset(id, versionId, body)

    @DeleteMapping("/{id}/versions/{versionId}/assets/{assetId}")
    @Operation(tags = ["application-sboms"], summary = "Remove an asset from the DRAFT primary BOM")
    fun removeAsset(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable assetId: UUID,
    ): VersionBomView = versions.removeAsset(id, versionId, assetId)

    @PostMapping("/{id}/versions/{versionId}/relations")
    @Operation(tags = ["application-sboms"], summary = "Add a relation to the DRAFT primary BOM")
    fun addRelation(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @RequestBody body: DraftRelationWrite,
    ): VersionBomView = versions.addRelation(id, versionId, body)

    @DeleteMapping("/{id}/versions/{versionId}/relations/{relationId}")
    @Operation(tags = ["application-sboms"], summary = "Remove a relation from the DRAFT primary BOM")
    fun removeRelation(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable relationId: UUID,
    ): VersionBomView = versions.removeRelation(id, versionId, relationId)

    @GetMapping("/{id}/versions/{versionId}/depends-on")
    @Operation(
        tags = ["application-versions"],
        summary = "Inferred application dependencies for one version",
    )
    fun versionDependsOn(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): List<InferredAppDependency> = versions.inferDependsOn(id, versionId)

    @GetMapping("/{id}/versions/{versionId}/fingerprints")
    @Operation(tags = ["application-versions"], summary = "List fingerprints for a version")
    fun listFingerprints(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
    ): List<ApplicationFingerprintSummary> = versions.listFingerprints(id, versionId)

    @GetMapping("/{id}/versions/{versionId}/fingerprints/{fingerprintId}")
    @Operation(tags = ["application-versions"], summary = "Fetch a fingerprint BOM snapshot")
    fun getFingerprint(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable fingerprintId: UUID,
        @RequestParam(defaultValue = "both") representation: String,
    ): VersionBomView {
        val parsed = try {
            PayloadRepresentation.parse(representation, PayloadRepresentation.BOTH)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
        }
        return versions.getFingerprintBom(id, versionId, fingerprintId, parsed)
    }

    @PostMapping("/{id}/versions/{versionId}/fingerprints")
    @Operation(
        tags = ["application-versions"],
        summary = "Snapshot the full Combined SBOM as a named fingerprint",
    )
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
    @Operation(tags = ["application-versions"], summary = "Fingerprints are immutable; PUT is rejected")
    @ApiResponses(
        ApiResponse(responseCode = "403", description = "Fingerprints are immutable"),
    )
    fun rejectFingerprintPut(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable fingerprintId: UUID,
        @RequestBody(required = false) body: ReplaceVersionBomRequest?,
    ): VersionBomView = versions.rejectFingerprintWrite(id, versionId, fingerprintId)

    @DeleteMapping("/{id}/versions/{versionId}/fingerprints/{fingerprintId}")
    @Operation(
        tags = ["application-versions"],
        summary = "Fingerprints are immutable; DELETE is rejected",
    )
    @ApiResponses(
        ApiResponse(responseCode = "403", description = "Fingerprints are immutable"),
    )
    fun rejectFingerprintDelete(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable fingerprintId: UUID,
    ): VersionBomView = versions.rejectFingerprintWrite(id, versionId, fingerprintId)

    @PostMapping("/{id}/versions/{versionId}/fingerprints/{fingerprintId}")
    @Operation(tags = ["application-versions"], summary = "Fingerprints are immutable; POST is rejected")
    @ApiResponses(
        ApiResponse(responseCode = "403", description = "Fingerprints are immutable"),
    )
    fun rejectFingerprintPost(
        @PathVariable id: UUID,
        @PathVariable versionId: UUID,
        @PathVariable fingerprintId: UUID,
        @RequestBody(required = false) body: Map<String, Any?>?,
    ): VersionBomView = versions.rejectFingerprintWrite(id, versionId, fingerprintId)

    @GetMapping("/{id}/versions/{versionId}/export/cyclonedx")
    @Operation(
        tags = ["application-versions"],
        summary = "Export a version Combined SBOM as CycloneDX JSON",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "CycloneDX JSON document (attachment)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = Map::class),
                ),
            ],
        ),
    )
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
