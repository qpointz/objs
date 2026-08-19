package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMCatalogSupport
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.match.BoMObjExprMatcher
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.sbom.annotations.SbomAnnotationKeys
import org.poc.objs.sbom.domain.AssetDetailView
import org.poc.objs.sbom.domain.AssetDuplicateGroup
import org.poc.objs.sbom.domain.AssetSearchPage
import org.poc.objs.sbom.domain.AssetSearchRequest
import org.poc.objs.sbom.domain.AssetTypeStatistics
import org.poc.objs.sbom.domain.AssetUsageEntry
import org.poc.objs.sbom.domain.AssetUsageRelation
import org.poc.objs.sbom.domain.AssetView
import org.poc.objs.sbom.domain.CreatePoolAssetRequest
import org.poc.objs.sbom.domain.SetAssetOwnerRequest
import org.poc.objs.sbom.domain.UpdatePoolAssetRequest
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.persistence.SbomApplicationSbomRepository
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Journey 2 assets inventory: pool search, usage (store reverse lookup), duplicates (store identity query), owner.
 */
@Service
class AssetInventoryService(
    private val store: BoMGraphStore,
    private val namedGraphs: BoMNamedGraphStore,
    private val schemas: BoMSchemaCatalog,
    private val assetTypes: AssetTypeCatalogService,
    private val catalog: BoMCatalogSupport,
    private val applications: SbomApplicationRepository,
    private val versions: SbomApplicationVersionRepository,
    private val boms: SbomApplicationSbomRepository,
    private val sbom: SbomService,
) {
    fun search(request: AssetSearchRequest): List<AssetView> {
        sbom.ensureRegistry()
        val knownSet = assetTypes.listEntityTypes().map { it.type }.toSet()
        if (knownSet.isEmpty()) return emptyList()
        return try {
            store.selectFromPool(BoMObjExprMatcher(matcherExpr(request))).entities
                .filter { it.type in knownSet }
                .map(AssetViews::asset)
        } catch (ex: BoMValidationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
        }
    }

    fun searchPage(request: AssetSearchRequest, page: Int, size: Int): AssetSearchPage {
        sbom.ensureRegistry()
        val expr = matcherExpr(request)
        val paged =
            try {
                store.selectFromPool(BoMObjExprMatcher(expr), org.poc.objs.core.domain.BoMPageRequest.of(page, size))
            } catch (ex: BoMValidationException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
            }
        val known = assetTypes.listEntityTypes().map { it.type }.toSet()
        val items = paged.items.filter { it.type in known }.map(AssetViews::asset)
        return AssetSearchPage(items = items, total = paged.total, page = paged.page, size = paged.size)
    }

    fun statistics(type: String): AssetTypeStatistics {
        val trimmed = type.trim()
        assetTypes.getEntityType(trimmed, null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown asset type: $trimmed")
        val objectCount = store.countByType()[trimmed] ?: 0L
        return AssetTypeStatistics(type = trimmed, objectCount = objectCount)
    }

    fun get(id: UUID): AssetDetailView {
        sbom.ensureRegistry()
        val entity =
            store.getEntity(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: $id")
        return AssetDetailView(asset = AssetViews.asset(entity), usage = usageFor(id))
    }

    @Transactional
    fun create(request: CreatePoolAssetRequest): AssetView {
        sbom.ensureRegistry()
        val type = request.type.trim()
        val detail =
            assetTypes.getEntityType(type, request.schemaVersion)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown asset type: $type")
        val ownerName = request.owner?.trim()?.takeIf { it.isNotEmpty() }
        if (ownerName != null && applications.findByNameIgnoreCase(ownerName) == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown owning application: $ownerName")
        }
        val annotations = mutableMapOf<String, String>()
        if (ownerName != null) {
            annotations[SbomAnnotationKeys.OWNER] = ownerName
        }
        val entity =
            BoMEntity(
                type = type,
                schemaVersion = request.schemaVersion?.trim()?.takeIf { it.isNotEmpty() } ?: detail.version,
                payload = request.payload.toMutableMap(),
                annotations = annotations,
            )
        val result = store.write(BoMGraph(entities = mutableListOf(entity)))
        if (!result.isValid) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                result.issues.joinToString("; ") { it.message },
            )
        }
        return AssetViews.asset(entity)
    }

    @Transactional
    fun update(id: UUID, request: UpdatePoolAssetRequest): AssetView {
        sbom.ensureRegistry()
        val entity =
            store.getEntity(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: $id")
        entity.payload.clear()
        entity.payload.putAll(request.payload)
        val result = store.write(BoMGraph(entities = mutableListOf(entity)))
        if (!result.isValid) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                result.issues.joinToString("; ") { it.message },
            )
        }
        return AssetViews.asset(entity)
    }

    @Transactional
    fun setOwner(id: UUID, request: SetAssetOwnerRequest): AssetView {
        sbom.ensureRegistry()
        val entity =
            store.getEntity(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: $id")
        val ownerName = request.owner?.trim()?.takeIf { it.isNotEmpty() }
        if (ownerName != null && applications.findByNameIgnoreCase(ownerName) == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown owning application: $ownerName")
        }
        if (ownerName == null) {
            entity.annotations.remove(SbomAnnotationKeys.OWNER)
        } else {
            entity.annotations[SbomAnnotationKeys.OWNER] = ownerName
        }
        store.upsertEntities(listOf(entity))
        return AssetViews.asset(entity)
    }

    /**
     * Find-only duplicate groups by schema identifier fields (store `findDuplicateGroups`).
     */
    fun findDuplicates(type: String, schemaVersion: String? = null): List<AssetDuplicateGroup> {
        sbom.ensureRegistry()
        val trimmed = type.trim()
        if (trimmed.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required")
        }
        val schema = resolveSchema(trimmed, schemaVersion)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown asset type: $trimmed")
        val wantedVersion = schemaVersion?.trim()?.takeIf { it.isNotEmpty() }
        return store.findDuplicateGroups(trimmed)
            .map { group ->
                val members =
                    if (wantedVersion == null) {
                        group.entities
                    } else {
                        group.entities.filter { it.schemaVersion == wantedVersion }
                    }
                group to members
            }
            .filter { it.second.size > 1 }
            .map { (group, members) ->
                AssetDuplicateGroup(
                    type = schema.type,
                    schemaVersion = schema.version,
                    identity = group.identity,
                    assets = members.map(AssetViews::asset),
                )
            }
            .sortedBy { it.identity.toString() }
    }

    private fun matcherExpr(request: AssetSearchRequest): String {
        val type = request.type?.trim().orEmpty()
        val objExpr = request.objExpr?.trim().orEmpty()
        val knownTypes = assetTypes.listEntityTypes().map { it.type }.distinct()
        if (type.isEmpty()) {
            if (request.filters.isNotEmpty()) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Filters require a selected asset type",
                )
            }
            return if (objExpr.isEmpty()) {
                knownTypes.joinToString(" || ") { "type == '${escape(it)}'" }
            } else {
                objExpr
            }
        }
        val detail =
            assetTypes.getEntityType(type, request.schemaVersion)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown asset type: $type")
        val searchable = detail.searchableFields.map { it.path }.toSet()
        val unknown = request.filters.keys.filter { it !in searchable }
        if (unknown.isNotEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Filters must use searchable fields only; rejected: ${unknown.sorted().joinToString()}",
            )
        }
        val filterExpr = catalog.filterMapToObjExpr(request.filters)
        val clauses = mutableListOf("type == '${escape(type)}'")
        if (!request.schemaVersion.isNullOrBlank()) {
            clauses += "schemaVersion == '${escape(request.schemaVersion.trim())}'"
        }
        if (filterExpr.isNotBlank()) clauses += filterExpr
        if (objExpr.isNotEmpty()) clauses += "($objExpr)"
        return clauses.joinToString(" && ")
    }

    /** Store reverse lookup, then domain join graph → application/version/BOM. */
    private fun usageFor(assetId: UUID): List<AssetUsageEntry> {
        val out = mutableListOf<AssetUsageEntry>()
        for (graphId in namedGraphs.listGraphIdsForEntity(assetId)) {
            val bom = boms.findByGraphId(graphId) ?: continue
            val version = versions.findById(bom.versionId).orElse(null) ?: continue
            val app = applications.findById(version.applicationId).orElse(null) ?: continue
            val edges = namedGraphs.listIncidentEdges(assetId, graphId)
            out +=
                AssetUsageEntry(
                    applicationId = app.id,
                    applicationName = app.name,
                    context = version.status,
                    versionId = version.id,
                    versionLabel = version.version.ifBlank { null } ?: version.label,
                    relations = incidentRelations(edges, assetId),
                )
        }
        return out.sortedWith(
            compareBy({ it.applicationName.lowercase() }, { it.context }, { it.versionLabel ?: "" }),
        )
    }

    private fun incidentRelations(
        edges: List<org.poc.objs.core.domain.BoMEdge>,
        assetId: UUID,
    ): List<AssetUsageRelation> =
        edges.mapNotNull { edge ->
            when (assetId) {
                edge.source ->
                    AssetUsageRelation(
                        role = edge.role,
                        label = RelationLabels.display(edge.role),
                        direction = "OUT",
                        otherAssetId = edge.target,
                    )
                edge.target ->
                    AssetUsageRelation(
                        role = edge.role,
                        label = RelationLabels.display(edge.role),
                        direction = "IN",
                        otherAssetId = edge.source,
                    )
                else -> null
            }
        }

    private fun resolveSchema(type: String, version: String?): BoMSchema? {
        if (version != null) {
            return schemas.get(type, version)?.takeIf { it.usage == BoMSchemaUsage.ENTITY }
        }
        return schemas.listByType(type)
            .filter { it.usage == BoMSchemaUsage.ENTITY }
            .maxByOrNull { it.version }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
