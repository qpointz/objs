package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMIdentityProjection
import org.poc.objs.core.domain.BoMSchema
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
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Journey 2 assets inventory: pool search, usage (FB-1 stopgap), duplicates (FB-2 stopgap), owner.
 */
@Service
class AssetInventoryService(
    private val store: BoMGraphStore,
    private val namedGraphs: BoMNamedGraphStore,
    private val schemas: BoMSchemaCatalog,
    private val assetTypes: AssetTypeCatalogService,
    private val applications: SbomApplicationRepository,
    private val versions: SbomApplicationVersionRepository,
    private val sbom: SbomService,
) {
    fun search(request: AssetSearchRequest): List<AssetView> {
        sbom.ensureRegistry()
        val type = request.type?.trim().orEmpty()
        val objExpr = request.objExpr?.trim().orEmpty()
        val knownTypes = assetTypes.listEntityTypes().map { it.type }.distinct()
        if (knownTypes.isEmpty()) {
            return emptyList()
        }
        val knownSet = knownTypes.toSet()

        fun query(expr: String): List<AssetView> =
            try {
                store.selectFromPool(BoMObjExprMatcher(expr)).entities
                    .filter { it.type in knownSet }
                    .map(AssetViews::asset)
            } catch (ex: BoMValidationException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
            }

        if (type.isEmpty()) {
            if (request.filters.isNotEmpty()) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Filters require a selected asset type",
                )
            }
            val expr =
                if (objExpr.isEmpty()) {
                    knownTypes.joinToString(" || ") { "type == '${escape(it)}'" }
                } else {
                    objExpr
                }
            return query(expr)
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

        val clauses = mutableListOf("type == '${escape(type)}'")
        if (!request.schemaVersion.isNullOrBlank()) {
            clauses += "schemaVersion == '${escape(request.schemaVersion.trim())}'"
        }
        for ((path, raw) in request.filters) {
            val value = raw.trim()
            if (value.isEmpty()) continue
            clauses += "p['${escape(path)}'] == '${escape(value)}'"
        }
        if (objExpr.isNotEmpty()) {
            clauses += "($objExpr)"
        }
        return query(clauses.joinToString(" && "))
    }

    fun searchPage(request: AssetSearchRequest, page: Int, size: Int): AssetSearchPage {
        val p = page.coerceAtLeast(1)
        val s = size.coerceIn(1, 100)
        val all = search(request).sortedBy { it.label.lowercase() }
        val from = (p - 1) * s
        val items =
            if (from >= all.size) {
                emptyList()
            } else {
                all.subList(from, minOf(from + s, all.size))
            }
        return AssetSearchPage(items = items, total = all.size.toLong(), page = p, size = s)
    }

    fun statistics(type: String): AssetTypeStatistics {
        val trimmed = type.trim()
        assetTypes.getEntityType(trimmed, null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown asset type: $trimmed")
        val objectCount = search(AssetSearchRequest(type = trimmed)).size.toLong()
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
        val schema =
            resolveSchema(entity.type, entity.schemaVersion)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown asset type: ${entity.type}")
        val identifiers = identifierFieldNames(schema.contentSchema)
        val next = request.payload.toMutableMap()
        for (name in identifiers) {
            val current = entity.payload[name]
            if (next.containsKey(name) && next[name] != current) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifier field '$name' cannot be changed after creation",
                )
            }
            if (entity.payload.containsKey(name)) {
                next[name] = current
            }
        }
        entity.payload.clear()
        entity.payload.putAll(next)
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
     * Find-only duplicate groups by schema identifier fields (G-P7 / FB-2 stopgap).
     */
    fun findDuplicates(type: String, schemaVersion: String? = null): List<AssetDuplicateGroup> {
        sbom.ensureRegistry()
        val trimmed = type.trim()
        if (trimmed.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required")
        }
        val schema = resolveSchema(trimmed, schemaVersion)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown asset type: $trimmed")
        val clauses = mutableListOf("type == '${escape(trimmed)}'")
        if (!schemaVersion.isNullOrBlank()) {
            clauses += "schemaVersion == '${escape(schemaVersion.trim())}'"
        }
        val entities = store.selectFromPool(BoMObjExprMatcher(clauses.joinToString(" && "))).entities
        val groups = linkedMapOf<String, MutableList<BoMEntity>>()
        val identities = linkedMapOf<String, Map<String, Any?>>()
        for (entity in entities) {
            val identity = BoMIdentityProjection.project(schema.contentSchema, entity.payload)
            if (identity.isEmpty()) continue
            val key = identity.entries.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" }
            groups.getOrPut(key) { mutableListOf() }.add(entity)
            identities.putIfAbsent(key, identity)
        }
        return groups.entries
            .filter { it.value.size > 1 }
            .map { (key, members) ->
                AssetDuplicateGroup(
                    type = schema.type,
                    schemaVersion = schema.version,
                    identity = identities.getValue(key),
                    assets = members.map(AssetViews::asset),
                )
            }
            .sortedBy { it.identity.toString() }
    }

    /**
     * FB-1 stopgap: scan known SBOM draft/version graphs only.
     */
    private fun usageFor(assetId: UUID): List<AssetUsageEntry> {
        val out = mutableListOf<AssetUsageEntry>()
        for (version in versions.findAll()) {
            val app = applications.findById(version.applicationId).orElse(null) ?: continue
            val graph = namedGraphs.get(version.graphId) ?: continue
            if (graph.contents.entities.none { it.id == assetId }) continue
            out +=
                AssetUsageEntry(
                    applicationId = app.id,
                    applicationName = app.name,
                    context = version.status,
                    versionId = version.id,
                    versionLabel = version.version ?: version.label,
                    relations = incidentRelations(graph.contents.edges, assetId),
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

    private fun identifierFieldNames(node: org.poc.objs.core.domain.BoMSchemaNode): Set<String> =
        node.fields.orEmpty().mapNotNull { field ->
            if (field.identifier == true) field.name else null
        }.toSet()

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
