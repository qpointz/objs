package org.poc.objs.sbom.service

import org.poc.objs.sbom.domain.ApplicationPortalStats
import org.poc.objs.sbom.domain.ApplicationSummary
import org.poc.objs.sbom.domain.BomUnion
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.DraftRelationWrite
import org.poc.objs.sbom.domain.InferredAppDependency
import org.poc.objs.sbom.domain.UpdateApplicationRequest
import org.poc.objs.sbom.domain.VersionBomView
import org.poc.objs.sbom.persistence.SbomApplicationRecord
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.persistence.SbomApplicationSbomRepository
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class ApplicationInventoryService(
    private val applications: SbomApplicationRepository,
    private val versionRows: SbomApplicationVersionRepository,
    private val boms: SbomApplicationSbomRepository,
    private val versions: ApplicationVersionService,
) {
    fun search(q: String?): List<ApplicationSummary> {
        val rows =
            if (q.isNullOrBlank()) {
                applications.findAll().sortedBy { it.name.lowercase() }
            } else {
                applications.search(q.trim())
            }
        return rows.map { it.toSummary() }
    }

    fun get(id: UUID): ApplicationSummary =
        applications.findById(id).orElseThrow { notFound("Application", id) }.toSummary()

    @Transactional
    fun create(request: CreateApplicationRequest): ApplicationSummary {
        val name = request.name.trim()
        if (name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required")
        }
        if (applications.findByNameIgnoreCase(name) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Application name already exists: $name")
        }
        val now = Instant.now()
        val id = request.id ?: UUID.randomUUID()
        if (applications.existsById(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Application already exists: $id")
        }
        val tags = BomUnion.sanitizeTags(request.tags)
        val app =
            applications.save(
                SbomApplicationRecord(
                    id = id,
                    name = name,
                    description = request.description?.trim()?.takeIf { it.isNotEmpty() },
                    tags = tags,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        versions.createEmptyDraft(app, request.targetVersion)
        return app.toSummary()
    }

    @Transactional
    fun update(id: UUID, request: UpdateApplicationRequest): ApplicationSummary {
        val app = applications.findById(id).orElseThrow { notFound("Application", id) }
        request.name?.trim()?.takeIf { it.isNotEmpty() }?.let { newName ->
            val clash = applications.findByNameIgnoreCase(newName)
            if (clash != null && clash.id != id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Application name already exists: $newName")
            }
            app.name = newName
        }
        if (request.description != null) {
            app.description = request.description.trim().takeIf { it.isNotEmpty() }
        }
        if (request.tags != null) {
            app.tags = BomUnion.sanitizeTags(request.tags)
        }
        app.updatedAt = Instant.now()
        return applications.save(app).toSummary()
    }

    fun portalStats(id: UUID): ApplicationPortalStats {
        applications.findById(id).orElseThrow { notFound("Application", id) }
        val rows = versionRows.findByApplicationIdOrderByCapturedAtDescIdDesc(id)
        val latest = versions.latestReleased(id)
        val latestMultiBom = latest != null && boms.countByVersionId(latest.id) >= 2
        return ApplicationPortalStats(
            applicationId = id,
            versionCount = rows.size,
            bomCount = rows.sumOf { boms.countByVersionId(it.id).toInt() },
            latestVersion = latest,
            latestMultiBom = latestMultiBom,
        )
    }

    fun getDraft(applicationId: UUID): VersionBomView {
        val row =
            versions.draft(applicationId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No draft for application: $applicationId")
        return versions.getBom(applicationId, row.id)
    }

    fun addAsset(applicationId: UUID, write: DraftAssetWrite): VersionBomView =
        versions.addAssetToDraft(applicationId, write)

    fun removeAsset(applicationId: UUID, assetId: UUID): VersionBomView =
        versions.removeAssetFromDraft(applicationId, assetId)

    fun addRelation(applicationId: UUID, write: DraftRelationWrite): VersionBomView =
        versions.addRelationToDraft(applicationId, write)

    fun removeRelation(applicationId: UUID, relationId: UUID): VersionBomView =
        versions.removeRelationFromDraft(applicationId, relationId)

    fun inferDependsOn(applicationId: UUID): List<InferredAppDependency> =
        versions.inferDependsOnDraft(applicationId)

    private fun notFound(what: String, id: UUID): ResponseStatusException =
        ResponseStatusException(HttpStatus.NOT_FOUND, "$what not found: $id")

    private fun SbomApplicationRecord.toSummary() =
        ApplicationSummary(id = id, name = name, description = description, tags = tags.toList())
}
