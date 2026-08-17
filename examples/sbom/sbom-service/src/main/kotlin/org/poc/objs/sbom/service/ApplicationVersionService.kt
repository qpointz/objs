package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraphDelete
import org.poc.objs.core.domain.BoMGraphException
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.domain.BoMGraphUpsert
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.sbom.annotations.SbomAnnotationKeys
import org.poc.objs.sbom.domain.ApplicationFingerprintSummary
import org.poc.objs.sbom.domain.ApplicationVersionSummary
import org.poc.objs.sbom.domain.AssetView
import org.poc.objs.sbom.domain.CreateDraftVersionRequest
import org.poc.objs.sbom.domain.CreateFingerprintRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.DraftRelationWrite
import org.poc.objs.sbom.domain.InferredAppDependency
import org.poc.objs.sbom.domain.PromoteVersionRequest
import org.poc.objs.sbom.domain.RelationView
import org.poc.objs.sbom.domain.ReplaceVersionBomRequest
import org.poc.objs.sbom.domain.VersionBomView
import org.poc.objs.sbom.model.CanonicalEdgeType
import org.poc.objs.sbom.domain.SemVerVersionComparer
import org.poc.objs.sbom.domain.VersionComparer
import org.poc.objs.sbom.persistence.ApplicationVersionStatus
import org.poc.objs.sbom.persistence.SbomApplicationFingerprintRecord
import org.poc.objs.sbom.persistence.SbomApplicationFingerprintRepository
import org.poc.objs.sbom.persistence.FingerprintCategory
import org.poc.objs.sbom.persistence.SbomApplicationSbomRecord
import org.poc.objs.sbom.persistence.SbomApplicationSbomRepository
import org.poc.objs.sbom.persistence.SbomApplicationRecord
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.persistence.SbomApplicationVersionRecord
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
class ApplicationVersionService(
    private val applications: SbomApplicationRepository,
    private val versions: SbomApplicationVersionRepository,
    private val fingerprints: SbomApplicationFingerprintRepository,
    private val boms: SbomApplicationSbomRepository,
    private val namedGraphs: BoMNamedGraphStore,
    private val sbom: SbomService,
) {
    private val versionComparer: VersionComparer = SemVerVersionComparer()
    fun list(applicationId: UUID): List<ApplicationVersionSummary> {
        requireApplication(applicationId)
        return versions.findByApplicationIdOrderByCapturedAtDescIdDesc(applicationId).map { it.toSummary() }
    }

    fun draft(applicationId: UUID): SbomApplicationVersionRecord? =
        versions.findByApplicationIdAndStatus(applicationId, ApplicationVersionStatus.DRAFT).firstOrNull()

    fun latestReleased(applicationId: UUID): ApplicationVersionSummary? =
        versions.findByApplicationIdOrderByCapturedAtDescIdDesc(applicationId)
            .filter { it.status == ApplicationVersionStatus.RELEASED }
            .maxWithOrNull(compareBy<SbomApplicationVersionRecord> { it.versionSerial }.thenBy { it.id })
            ?.toSummary()

    fun latest(applicationId: UUID): ApplicationVersionSummary? = latestReleased(applicationId)

    fun latestGraphIds(applicationIds: Collection<UUID>): Map<UUID, UUID> {
        val out = linkedMapOf<UUID, UUID>()
        for (appId in applicationIds.distinct()) {
            val row =
                versions.findByApplicationIdOrderByCapturedAtDescIdDesc(appId)
                    .filter { it.status == ApplicationVersionStatus.RELEASED }
                    .maxWithOrNull(
                        compareBy<SbomApplicationVersionRecord> { it.versionSerial }.thenBy { it.id },
                    )
            if (row != null) {
                out[appId] = bomGraphId(row)
            }
        }
        return out
    }

    fun currentGraphId(applicationId: UUID): UUID? =
        draft(applicationId)?.let { bomGraphId(it) } ?: latestGraphIds(listOf(applicationId))[applicationId]

    fun getBom(applicationId: UUID, versionId: UUID): VersionBomView {
        val app = requireApplication(applicationId)
        val row = requireVersion(applicationId, versionId)
        return toBomView(app, row)
    }

    fun getFingerprintBom(applicationId: UUID, versionId: UUID, fingerprintId: UUID): VersionBomView {
        val app = requireApplication(applicationId)
        val row = requireVersion(applicationId, versionId)
        val fingerprint = requireFingerprint(versionId, fingerprintId)
        return toBomView(app, row, fingerprint.graphId)
    }

    fun rejectFingerprintWrite(applicationId: UUID, versionId: UUID, fingerprintId: UUID): Nothing {
        requireApplication(applicationId)
        requireVersion(applicationId, versionId)
        requireFingerprint(versionId, fingerprintId)
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "Fingerprints are immutable")
    }

    @Transactional
    fun createEmptyDraft(app: SbomApplicationRecord): SbomApplicationVersionRecord {
        val ident = nextDraftVersion(app.id)
        val row =
            versions.save(
                SbomApplicationVersionRecord(
                    applicationId = app.id,
                    capturedAt = Instant.now(),
                    status = ApplicationVersionStatus.DRAFT,
                    version = ident,
                    label = ident,
                    versionSerial = versionComparer.toSerial(ident),
                ),
            )
        createPrimaryBom(app, row, ApplicationVersionStatus.DRAFT)
        return row
    }

    @Transactional
    fun createDraft(applicationId: UUID, request: CreateDraftVersionRequest): VersionBomView {
        sbom.ensureRegistry()
        val app = requireApplication(applicationId)
        if (draft(applicationId) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Application already has a draft")
        }
        val sourceId = request.fromVersionId
        val source = sourceId?.let { requireVersion(applicationId, it) }
        val ident = nextDraftVersion(applicationId)
        val row =
            versions.save(
                SbomApplicationVersionRecord(
                    applicationId = app.id,
                    capturedAt = Instant.now(),
                    status = ApplicationVersionStatus.DRAFT,
                    version = ident,
                    label = ident,
                    versionSerial = versionComparer.toSerial(ident),
                    basedOnVersionId = source?.id,
                ),
            )
        createPrimaryBom(app, row, ApplicationVersionStatus.DRAFT, source?.let { bomGraphId(it) })
        return toBomView(app, row)
    }

    @Transactional
    fun promote(applicationId: UUID, versionId: UUID, request: PromoteVersionRequest): VersionBomView {
        val app = requireApplication(applicationId)
        val row = requireVersion(applicationId, versionId)
        if (row.status != ApplicationVersionStatus.DRAFT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a draft can be promoted")
        }
        val ident = request.version.trim()
        if (ident.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "version is required")
        }
        val clash =
            versions.findByApplicationIdOrderByCapturedAtDescIdDesc(applicationId).any {
                it.status == ApplicationVersionStatus.RELEASED && it.version.equals(ident, ignoreCase = true)
            }
        if (clash) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Version already exists: $ident")
        }
        val now = Instant.now()
        row.status = ApplicationVersionStatus.RELEASED
        row.version = ident
        row.label = ident
        row.versionSerial = versionComparer.toSerial(ident)
        row.promotedAt = now
        val bom = primaryBom(row)
        namedGraphs.updateAnnotations(
            bom.graphId,
            mapOf(
                "kind" to "application-bom",
                "status" to ApplicationVersionStatus.RELEASED,
                "applicationId" to app.id.toString(),
                "applicationName" to app.name,
                "version" to ident,
                "versionId" to row.id.toString(),
                "bomId" to bom.id.toString(),
            ),
        )
        return toBomView(app, versions.save(row))
    }

    @Transactional
    fun addAsset(applicationId: UUID, versionId: UUID, write: DraftAssetWrite): VersionBomView {
        sbom.ensureRegistry()
        val app = requireApplication(applicationId)
        val row = requireWritable(applicationId, versionId)
        forbidFingerprintGraph(bomGraphId(row))
        applyAssetWrite(app, bomGraphId(row), write)
        return toBomView(app, row)
    }

    @Transactional
    fun addAssetToDraft(applicationId: UUID, write: DraftAssetWrite): VersionBomView {
        val row = requireDraft(applicationId)
        return addAsset(applicationId, row.id, write)
    }

    @Transactional
    fun removeAsset(applicationId: UUID, versionId: UUID, assetId: UUID): VersionBomView {
        val app = requireApplication(applicationId)
        val row = requireWritable(applicationId, versionId)
        forbidFingerprintGraph(bomGraphId(row))
        try {
            namedGraphs.detach(bomGraphId(row), assetId)
        } catch (ex: BoMGraphException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
        }
        return toBomView(app, row)
    }

    @Transactional
    fun removeAssetFromDraft(applicationId: UUID, assetId: UUID): VersionBomView {
        val row = requireDraft(applicationId)
        return removeAsset(applicationId, row.id, assetId)
    }

    @Transactional
    fun addRelation(applicationId: UUID, versionId: UUID, write: DraftRelationWrite): VersionBomView {
        sbom.ensureRegistry()
        val app = requireApplication(applicationId)
        val row = requireWritable(applicationId, versionId)
        forbidFingerprintGraph(bomGraphId(row))
        applyRelation(bomGraphId(row), write)
        return toBomView(app, row)
    }

    @Transactional
    fun addRelationToDraft(applicationId: UUID, write: DraftRelationWrite): VersionBomView {
        val row = requireDraft(applicationId)
        return addRelation(applicationId, row.id, write)
    }

    @Transactional
    fun removeRelation(applicationId: UUID, versionId: UUID, relationId: UUID): VersionBomView {
        val app = requireApplication(applicationId)
        val row = requireWritable(applicationId, versionId)
        forbidFingerprintGraph(bomGraphId(row))
        applyDeleteEdges(bomGraphId(row), listOf(relationId))
        return toBomView(app, row)
    }

    @Transactional
    fun removeRelationFromDraft(applicationId: UUID, relationId: UUID): VersionBomView {
        val row = requireDraft(applicationId)
        return removeRelation(applicationId, row.id, relationId)
    }

    @Transactional
    fun replaceBom(applicationId: UUID, versionId: UUID, request: ReplaceVersionBomRequest): VersionBomView {
        sbom.ensureRegistry()
        val app = requireApplication(applicationId)
        val row = requireWritable(applicationId, versionId)
        forbidFingerprintGraph(bomGraphId(row))
        val resolved =
            namedGraphs.get(bomGraphId(row))
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Version graph missing")
        val currentIds = resolved.contents.entities.mapNotNull { it.id }.toSet()
        val desired = request.assetIds.toSet()
        for (id in desired - currentIds) {
            try {
                namedGraphs.attach(bomGraphId(row), id)
            } catch (ex: BoMGraphException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
            }
        }
        for (id in currentIds - desired) {
            try {
                namedGraphs.detach(bomGraphId(row), id)
            } catch (ex: BoMGraphException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
            }
        }
        val after =
            namedGraphs.get(bomGraphId(row))
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Version graph missing")
        val existingEdgeIds = after.contents.edges.mapNotNull { it.id }
        if (existingEdgeIds.isNotEmpty()) {
            applyDeleteEdges(bomGraphId(row), existingEdgeIds)
        }
        for (rel in request.relations) {
            applyRelation(bomGraphId(row), rel)
        }
        return toBomView(app, row)
    }

    @Transactional
    fun fingerprint(
        applicationId: UUID,
        versionId: UUID,
        request: CreateFingerprintRequest = CreateFingerprintRequest(),
    ): ApplicationFingerprintSummary {
        sbom.ensureRegistry()
        requireApplication(applicationId)
        val row = requireVersion(applicationId, versionId)
        val graphId =
            copyGraph(
                bomGraphId(row),
                mapOf(
                    "kind" to "application-fingerprint",
                    "applicationId" to applicationId.toString(),
                    "versionId" to versionId.toString(),
                ),
            )
        val hash = contentHash(graphId)
        val name =
            request.name?.trim()?.takeIf { it.isNotEmpty() }
                ?: request.note?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Fingerprint"
        val category =
            request.category?.trim()?.lowercase()?.takeIf { it in FingerprintCategory.ALL }
                ?: FingerprintCategory.UNKNOWN
        val saved =
            fingerprints.save(
                SbomApplicationFingerprintRecord(
                    versionId = versionId,
                    graphId = graphId,
                    createdAt = Instant.now(),
                    name = name,
                    category = category,
                    contentSha256 = hash,
                ),
            )
        return saved.toSummary()
    }

    fun listFingerprints(applicationId: UUID, versionId: UUID): List<ApplicationFingerprintSummary> {
        requireVersion(applicationId, versionId)
        return fingerprints.findByVersionIdOrderByCreatedAtDesc(versionId).map { it.toSummary() }
    }

    fun inferDependsOn(applicationId: UUID, versionId: UUID): List<InferredAppDependency> {
        requireApplication(applicationId)
        val row = requireVersion(applicationId, versionId)
        val source = namedGraphs.get(bomGraphId(row)) ?: return emptyList()
        val sourceIds = source.contents.entities.mapNotNull { it.id }.toSet()
        if (sourceIds.isEmpty()) {
            return emptyList()
        }
        val out = mutableListOf<InferredAppDependency>()
        for (other in applications.findAll()) {
            if (other.id == applicationId) continue
            val peerGraphId = currentGraphId(other.id) ?: continue
            val peer = namedGraphs.get(peerGraphId) ?: continue
            val shared =
                peer.contents.entities
                    .mapNotNull { it.id }
                    .filter { it in sourceIds }
                    .sorted()
            if (shared.isNotEmpty()) {
                out +=
                    InferredAppDependency(
                        applicationId = other.id,
                        applicationName = other.name,
                        sharedAssetIds = shared,
                    )
            }
        }
        return out.sortedBy { it.applicationName.lowercase() }
    }

    fun inferDependsOnDraft(applicationId: UUID): List<InferredAppDependency> {
        val row = requireDraft(applicationId)
        return inferDependsOn(applicationId, row.id)
    }

    private fun applyAssetWrite(app: SbomApplicationRecord, graphId: UUID, write: DraftAssetWrite) {
        forbidFingerprintGraph(graphId)
        when {
            write.assetId != null && write.type == null && write.payload == null -> {
                try {
                    namedGraphs.attach(graphId, write.assetId)
                } catch (ex: BoMGraphException) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
                }
            }
            write.assetId == null && !write.type.isNullOrBlank() && write.payload != null -> {
                val annotations = mutableMapOf<String, String>()
                if (write.setOwner) {
                    annotations[SbomAnnotationKeys.OWNER] = app.name
                }
                val entity =
                    BoMEntity(
                        type = write.type.trim(),
                        schemaVersion = write.schemaVersion?.trim()?.takeIf { it.isNotEmpty() } ?: "1.0.0",
                        payload = write.payload.toMutableMap(),
                        annotations = annotations,
                    )
                val result =
                    namedGraphs.mutate(
                        graphId,
                        BoMGraphMutation(upsert = BoMGraphUpsert(entities = mutableListOf(entity))),
                    )
                if (!result.isValid) {
                    throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        result.issues.joinToString("; ") { it.message },
                    )
                }
            }
            else ->
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide assetId to reuse, or type+payload to create",
                )
        }
    }

    private fun applyRelation(graphId: UUID, write: DraftRelationWrite) {
        forbidFingerprintGraph(graphId)
        val role = write.role.trim()
        if (role.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "role is required")
        }
        val edge =
            BoMEdge(
                source = write.fromAssetId,
                target = write.toAssetId,
                role = role,
                type = CanonicalEdgeType.meta.type,
                schemaVersion = CanonicalEdgeType.meta.schemaVersion,
                properties = mutableMapOf(),
            )
        val result =
            namedGraphs.mutate(
                graphId,
                BoMGraphMutation(upsert = BoMGraphUpsert(edges = mutableListOf(edge))),
            )
        if (!result.isValid) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                result.issues.joinToString("; ") { it.message },
            )
        }
    }

    private fun applyDeleteEdges(graphId: UUID, ids: List<UUID>) {
        forbidFingerprintGraph(graphId)
        val result =
            namedGraphs.mutate(
                graphId,
                BoMGraphMutation(delete = BoMGraphDelete(edges = ids.toMutableList())),
            )
        if (!result.isValid) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                result.issues.joinToString("; ") { it.message },
            )
        }
    }

    private fun copyGraph(sourceGraphId: UUID, annotations: Map<String, String>): UUID {
        val source =
            namedGraphs.get(sourceGraphId)
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Source graph missing")
        val graph =
            namedGraphs.create(
                BoMGraphSpec(
                    annotations = annotations,
                    entityIds = source.contents.entities.mapNotNull { it.id }.toSet(),
                ),
            )
        val edgeCopies =
            source.contents.edges
                .map { edge ->
                    BoMEdge(
                        source = edge.source,
                        target = edge.target,
                        role = edge.role,
                        type = edge.type,
                        schemaVersion = edge.schemaVersion,
                        properties = edge.properties?.toMutableMap(),
                    )
                }.toMutableList()
        if (edgeCopies.isNotEmpty()) {
            val result =
                namedGraphs.mutate(
                    graph.id,
                    BoMGraphMutation(upsert = BoMGraphUpsert(edges = edgeCopies)),
                )
            if (!result.isValid) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    result.issues.joinToString("; ") { it.message },
                )
            }
        }
        return graph.id
    }

    private fun contentHash(graphId: UUID): String {
        val resolved = namedGraphs.get(graphId) ?: return ""
        val entities =
            resolved.contents.entities
                .mapNotNull { it.id }
                .sorted()
                .joinToString(",")
        val edges =
            resolved.contents.edges
                .map { "${it.source}>${it.role}>${it.target}" }
                .sorted()
                .joinToString(",")
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$entities|$edges".toByteArray())
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }

    private fun toBomView(
        app: SbomApplicationRecord,
        row: SbomApplicationVersionRecord,
        graphId: UUID = bomGraphId(row),
    ): VersionBomView {
        val resolved =
            namedGraphs.get(graphId)
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Version graph missing")
        return VersionBomView(
            version = row.toSummary(),
            applicationName = app.name,
            assets = resolved.contents.entities.map { it.toAssetView() },
            relations = resolved.contents.edges.map { it.toRelationView() },
        )
    }

    private fun requireApplication(id: UUID) =
        applications.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: $id")
        }

    private fun requireVersion(applicationId: UUID, versionId: UUID): SbomApplicationVersionRecord =
        versions.findByIdAndApplicationId(versionId, applicationId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found: $versionId")

    private fun requireDraft(applicationId: UUID): SbomApplicationVersionRecord =
        draft(applicationId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No draft for application: $applicationId")

    private fun requireFingerprint(versionId: UUID, fingerprintId: UUID): SbomApplicationFingerprintRecord =
        fingerprints.findByIdAndVersionId(fingerprintId, versionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Fingerprint not found: $fingerprintId")

    private fun forbidFingerprintGraph(graphId: UUID) {
        val graph = namedGraphs.get(graphId) ?: return
        if (graph.annotations["kind"] == "application-fingerprint") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Fingerprints are immutable")
        }
    }

    private fun requireWritable(applicationId: UUID, versionId: UUID): SbomApplicationVersionRecord {
        // Draft and released version graphs can be edited in place. Fingerprints are separate
        // immutable snapshot graphs and are never mutated through this path.
        return requireVersion(applicationId, versionId)
    }

    private fun bomGraphId(row: SbomApplicationVersionRecord): UUID = primaryBom(row).graphId

    private fun primaryBom(row: SbomApplicationVersionRecord): SbomApplicationSbomRecord =
        boms.findByVersionIdOrderBySortOrderAscIdAsc(row.id).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Version has no BOM")

    private fun createPrimaryBom(
        app: SbomApplicationRecord,
        row: SbomApplicationVersionRecord,
        status: String,
        sourceGraphId: UUID? = null,
    ): SbomApplicationSbomRecord {
        val bomId = UUID.randomUUID()
        val annotations =
            linkedMapOf(
                "kind" to "application-bom",
                "status" to status,
                "applicationId" to app.id.toString(),
                "applicationName" to app.name,
                "versionId" to row.id.toString(),
                "bomId" to bomId.toString(),
            )
        row.version.takeIf { it.isNotBlank() }?.let { annotations["version"] = it }
        val graphId =
            if (sourceGraphId == null) {
                namedGraphs.create(BoMGraphSpec(annotations = annotations)).id
            } else {
                copyGraph(sourceGraphId, annotations)
            }
        return boms.save(
            SbomApplicationSbomRecord(
                id = bomId,
                versionId = row.id,
                name = "BOM",
                graphId = graphId,
                sortOrder = 0,
            ),
        )
    }

    private fun nextDraftVersion(applicationId: UUID): String {
        val used =
            versions.findByApplicationIdOrderByCapturedAtDescIdDesc(applicationId)
                .map { it.version }
                .toMutableSet()
        if ("0.1.0" !in used) return "0.1.0"
        var i = 1
        while (true) {
            val candidate = "0.0.0-draft.$i"
            if (candidate !in used) return candidate
            i++
        }
    }

    private fun SbomApplicationVersionRecord.toSummary() =
        ApplicationVersionSummary(
            id = id,
            applicationId = applicationId,
            status = status,
            version = version,
            label = label ?: version.ifBlank { null } ?: if (status == ApplicationVersionStatus.DRAFT) "Draft" else null,
            capturedAt = capturedAt,
            promotedAt = promotedAt,
        )

    private fun SbomApplicationFingerprintRecord.toSummary() =
        ApplicationFingerprintSummary(
            id = id,
            versionId = versionId,
            createdAt = createdAt,
            note = name.takeIf { it.isNotEmpty() },
            name = name,
            category = category,
            contentSha256 = contentSha256,
        )

    private fun BoMEntity.toAssetView(): AssetView {
        val id = requireNotNull(id) { "asset missing id" }
        return AssetView(
            id = id,
            type = type,
            schemaVersion = schemaVersion,
            label = assetLabel(payload, type),
            payload = payload.toMap(),
            owner = annotations[SbomAnnotationKeys.OWNER],
        )
    }

    private fun BoMEdge.toRelationView(): RelationView {
        val id = requireNotNull(id) { "relation missing id" }
        return RelationView(
            id = id,
            role = role,
            label = RelationLabels.display(role),
            fromAssetId = source,
            toAssetId = target,
        )
    }

    private fun assetLabel(payload: Map<String, Any?>, type: String): String {
        val name = payload["name"]?.toString()?.takeIf { it.isNotBlank() }
        val version = payload["version"]?.toString()?.takeIf { it.isNotBlank() }
        return when {
            name != null && version != null -> "$name@$version"
            name != null -> name
            else -> type
        }
    }
}
