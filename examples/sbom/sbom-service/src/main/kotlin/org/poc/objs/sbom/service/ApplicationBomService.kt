package org.poc.objs.sbom.service

import org.poc.objs.core.domain.GraphSpec
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.sbom.domain.AssetView
import org.poc.objs.sbom.domain.ApplicationVersionSummary
import org.poc.objs.sbom.domain.BomSummary
import org.poc.objs.sbom.domain.BomUnion
import org.poc.objs.sbom.domain.CombinedBomView
import org.poc.objs.sbom.domain.CreateBomRequest
import org.poc.objs.sbom.domain.RelationView
import org.poc.objs.sbom.domain.UpdateBomRequest
import org.poc.objs.sbom.domain.VersionBomView
import org.poc.objs.sbom.annotations.SbomAnnotationKeys
import org.poc.objs.sbom.persistence.ApplicationVersionStatus
import org.poc.objs.sbom.persistence.SbomApplicationRecord
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.persistence.SbomApplicationSbomRecord
import org.poc.objs.sbom.persistence.SbomApplicationSbomRepository
import org.poc.objs.sbom.persistence.SbomApplicationVersionRecord
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ApplicationBomService(
    private val applications: SbomApplicationRepository,
    private val versions: SbomApplicationVersionRepository,
    private val boms: SbomApplicationSbomRepository,
    private val namedGraphs: NamedGraphStore,
    private val graphs: BomGraphSupport,
) {
    fun list(applicationId: UUID, versionId: UUID): List<BomSummary> {
        requireVersion(applicationId, versionId)
        return boms.findByVersionIdOrderBySortOrderAscIdAsc(versionId).map { it.toSummary() }
    }

    fun get(applicationId: UUID, versionId: UUID, bomId: UUID): VersionBomView {
        val app = requireApplication(applicationId)
        val row = requireVersion(applicationId, versionId)
        val bom = requireBom(versionId, bomId)
        val resolved =
            namedGraphs.get(bom.graphId)
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "BOM graph missing")
        return VersionBomView(
            version = row.toSummary(),
            applicationName = app.name,
            assets = resolved.contents.entities.map { it.toAssetView() },
            relations = resolved.contents.edges.map { it.toRelationView() },
        )
    }

    fun combined(applicationId: UUID, versionId: UUID, selectedBomIds: List<UUID>? = null): CombinedBomView {
        val app = requireApplication(applicationId)
        val row = requireVersion(applicationId, versionId)
        val all = boms.findByVersionIdOrderBySortOrderAscIdAsc(versionId)
        if (all.isEmpty()) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Version has no BOM")
        }
        val selected =
            if (selectedBomIds.isNullOrEmpty()) {
                all
            } else {
                val wanted = selectedBomIds.toSet()
                val picked = all.filter { it.id in wanted }
                if (picked.size != wanted.size) {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "One or more BOMs were not found")
                }
                picked
            }
        val contents = graphs.union(selected.map { it.graphId })
        return CombinedBomView(
            version = row.toSummary(),
            applicationName = app.name,
            assets = contents.entities.map { it.toAssetView() },
            relations = contents.edges.map { it.toRelationView() },
            combinedTags = combinedTags(app, row, all),
            selectedBomIds = selected.map { it.id },
        )
    }

    fun combinedTags(applicationId: UUID, versionId: UUID): List<String> {
        val app = requireApplication(applicationId)
        val row = requireVersion(applicationId, versionId)
        val all = boms.findByVersionIdOrderBySortOrderAscIdAsc(versionId)
        return combinedTags(app, row, all)
    }

    fun graphIds(versionId: UUID): List<UUID> =
        boms.findByVersionIdOrderBySortOrderAscIdAsc(versionId).map { it.graphId }

    @Transactional
    fun create(applicationId: UUID, versionId: UUID, request: CreateBomRequest): BomSummary {
        val app = requireApplication(applicationId)
        val row = requireDraftVersion(applicationId, versionId)
        val name = requireName(request.name)
        if (boms.findByVersionIdAndName(versionId, name) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "BOM name already exists: $name")
        }
        val bomId = UUID.randomUUID()
        val nextOrder = (boms.findByVersionIdOrderBySortOrderAscIdAsc(versionId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val graph =
            namedGraphs.create(
                GraphSpec(
                    annotations =
                        mapOf(
                            "kind" to "application-bom",
                            "status" to row.status,
                            "applicationId" to app.id.toString(),
                            "applicationName" to app.name,
                            "versionId" to row.id.toString(),
                            "bomId" to bomId.toString(),
                        ),
                ),
            )
        return boms.save(
            SbomApplicationSbomRecord(
                id = bomId,
                versionId = versionId,
                name = name,
                description = request.description?.trim()?.takeIf { it.isNotEmpty() },
                tags = BomUnion.sanitizeTags(request.tags),
                graphId = graph.id,
                sortOrder = nextOrder,
            ),
        ).toSummary()
    }

    @Transactional
    fun update(applicationId: UUID, versionId: UUID, bomId: UUID, request: UpdateBomRequest): BomSummary {
        requireDraftVersion(applicationId, versionId)
        val bom = requireBom(versionId, bomId)
        request.name?.let { raw ->
            val name = requireName(raw)
            val clash = boms.findByVersionIdAndName(versionId, name)
            if (clash != null && clash.id != bomId) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "BOM name already exists: $name")
            }
            bom.name = name
        }
        if (request.description != null) {
            bom.description = request.description.trim().takeIf { it.isNotEmpty() }
        }
        if (request.tags != null) {
            bom.tags = BomUnion.sanitizeTags(request.tags)
        }
        return boms.save(bom).toSummary()
    }

    @Transactional
    fun delete(applicationId: UUID, versionId: UUID, bomId: UUID) {
        requireDraftVersion(applicationId, versionId)
        val bom = requireBom(versionId, bomId)
        if (boms.countByVersionId(versionId) <= 1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the last BOM")
        }
        val graphId = bom.graphId
        boms.delete(bom)
        namedGraphs.delete(graphId)
    }

    private fun combinedTags(
        app: SbomApplicationRecord,
        row: SbomApplicationVersionRecord,
        all: List<SbomApplicationSbomRecord>,
    ): List<String> = BomUnion.combinedTags(app.tags, row.tags, all.map { it.tags })

    private fun requireName(raw: String): String {
        val name = raw.trim()
        if (name.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required")
        }
        return name
    }

    private fun requireApplication(id: UUID) =
        applications.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: $id")
        }

    private fun requireVersion(applicationId: UUID, versionId: UUID): SbomApplicationVersionRecord =
        versions.findByIdAndApplicationId(versionId, applicationId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found: $versionId")

    private fun requireDraftVersion(applicationId: UUID, versionId: UUID): SbomApplicationVersionRecord {
        val row = requireVersion(applicationId, versionId)
        if (row.status != ApplicationVersionStatus.DRAFT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a draft version's BOMs can be changed")
        }
        return row
    }

    private fun requireBom(versionId: UUID, bomId: UUID): SbomApplicationSbomRecord =
        boms.findByIdAndVersionId(bomId, versionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found: $bomId")

    private fun SbomApplicationSbomRecord.toSummary() =
        BomSummary(
            id = id,
            versionId = versionId,
            name = name,
            description = description,
            tags = tags.toList(),
            sortOrder = sortOrder,
        )

    private fun SbomApplicationVersionRecord.toSummary() =
        ApplicationVersionSummary(
            id = id,
            applicationId = applicationId,
            status = status,
            version = version,
            label = label ?: version.ifBlank { null } ?: if (status == ApplicationVersionStatus.DRAFT) "Draft" else null,
            capturedAt = capturedAt,
            promotedAt = promotedAt,
            tags = tags.toList(),
            basedOnVersionId = basedOnVersionId,
            basedOnFingerprintId = basedOnFingerprintId,
            bomCount = boms.countByVersionId(id).toInt(),
        )

    private fun org.poc.objs.api.domain.Entity.toAssetView(): AssetView {
        val id = requireNotNull(id) { "asset missing id" }
        return AssetView(
            id = id,
            type = type,
            schemaVersion = schemaVersion,
            label = AssetViews.label(payload, type),
            payload = payload.toMap(),
            owner = annotations[SbomAnnotationKeys.OWNER],
        )
    }

    private fun org.poc.objs.api.domain.Edge.toRelationView(): RelationView {
        val id = requireNotNull(id) { "relation missing id" }
        return RelationView(
            id = id,
            role = role,
            label = RelationLabels.display(role),
            fromAssetId = source,
            toAssetId = target,
        )
    }
}
