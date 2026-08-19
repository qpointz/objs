package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.match.BoMGraphIdsMatcher
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.gremlin.core.BoMGremlinEngine
import org.poc.objs.gremlin.core.BoMGremlinItem
import org.poc.objs.sbom.domain.MiCompositionSection
import org.poc.objs.sbom.domain.MiDependencyEdge
import org.poc.objs.sbom.domain.MiDuplicateSignal
import org.poc.objs.sbom.domain.MiReportId
import org.poc.objs.sbom.domain.MiReportResult
import org.poc.objs.sbom.domain.MiRiskSignal
import org.poc.objs.sbom.domain.MiSharedAsset
import org.poc.objs.sbom.domain.MiReportTable
import org.poc.objs.sbom.domain.PortfolioAppRef
import org.poc.objs.sbom.domain.RunMiReportRequest
import org.poc.objs.sbom.registry.SbomRoles
import org.poc.objs.sbom.resolution.PortfolioGraphSelector
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Portfolio MI reports: apps in the selected category (optional subtree) → current BOM
 * (edit draft, else latest version) → `graphs-in` (+ Gremlin for MI-1).
 */
@Service
class MiReportService(
    private val portfolios: PortfolioService,
    private val graphs: PortfolioGraphSelector,
    private val store: BoMGraphStore,
    private val namedGraphs: BoMNamedGraphStore,
    private val engine: BoMGremlinEngine = BoMGremlinEngine(),
) {
    fun run(portfolioId: UUID, request: RunMiReportRequest): MiReportResult {
        val report =
            try {
                MiReportId.parse(request.report)
            } catch (ex: IllegalStateException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
            }
        val uniqueness = portfolios.getTree(portfolioId).portfolio.uniqueness
        val inScope = portfolios.applicationsInScope(portfolioId, request.level, request.includeSubcategories)
        val resolution = graphs.selectCurrentBom(uniqueness, inScope)
        val withVersion = inScope.filter { it.applicationId in resolution.graphByApp }
        val withoutVersion = resolution.omitted
        val graphIds = resolution.graphByApp.values.flatten().distinct()
        val graphByApp = resolution.graphByApp
        val nameByApp = inScope.associate { it.applicationId to it.applicationName }

        val notes = mutableListOf<String>()
        if (withoutVersion.isNotEmpty()) {
            notes += "${withoutVersion.size} application(s) have no version and were omitted from graph selection"
        }
        if (graphIds.isEmpty()) {
            notes += "No latest-version graphs in scope"
        }

        val base =
            MiReportResult(
                portfolioId = portfolioId,
                level = request.level.trim(),
                report = report.name,
                title = title(report),
                applicationsInScope = inScope,
                applicationsWithoutVersion = withoutVersion,
                graphCount = graphIds.size,
                notes = notes,
            )

        return when (report) {
            MiReportId.`MI-1` -> base.copy(composition = composition(graphIds))
            MiReportId.`MI-2` ->
                base.copy(
                    dependencyMap = dependencyMap(withVersion, graphByApp, nameByApp),
                )
            MiReportId.`MI-3` ->
                base.copy(
                    sharedAssets = sharedAssets(withVersion, graphByApp, nameByApp),
                )
            MiReportId.`MI-4` -> {
                val (dupes, risks) = duplicateAndRisk(graphIds)
                base.copy(duplicateSignals = dupes, riskSignals = risks)
            }
        }
    }

    private fun composition(graphIds: List<UUID>): MiCompositionSection {
        if (graphIds.isEmpty()) {
            return MiCompositionSection(emptyMap(), 0, 0)
        }
        val matcher = BoMGraphIdsMatcher(graphIds)
        val byType = engine.selectAndEval(store, matcher, "g.V().label().groupCount()")
        val typeCounts: Map<String, Long> =
            when (val item = byType.items.singleOrNull()) {
                is BoMGremlinItem.MapValue ->
                    item.value.mapKeys { it.key.toString() }.mapValues { (_, v) ->
                        (v as? Number)?.toLong() ?: 0L
                    }
                is BoMGremlinItem.Scalar ->
                    (item.value as? Map<*, *>)?.entries?.associate { (k, v) ->
                        k.toString() to ((v as? Number)?.toLong() ?: 0L)
                    } ?: emptyMap()
                else -> emptyMap()
            }
        val relations =
            (engine.selectAndEval(store, matcher, "g.E().count()").views.scalar as? Number)?.toLong() ?: 0L
        val dependsOn =
            (
                engine.selectAndEval(store, matcher, "g.E().hasLabel('${SbomRoles.DEPENDS_ON}').count()")
                    .views.scalar as? Number
                )?.toLong() ?: 0L
        return MiCompositionSection(
            assetCountsByType = typeCounts.toSortedMap(),
            relationCount = relations,
            dependsOnCount = dependsOn,
        )
    }

    private fun dependencyMap(
        apps: List<PortfolioAppRef>,
        graphByApp: Map<UUID, List<UUID>>,
        nameByApp: Map<UUID, String>,
    ): List<MiDependencyEdge> {
        val members =
            apps.associate { app ->
                val entityIds =
                    graphByApp.getValue(app.applicationId)
                        .flatMap { namedGraphs.listEntityIdsInGraph(it) }
                        .toSet()
                app.applicationId to entityIds
            }
        val out = mutableListOf<MiDependencyEdge>()
        for (i in apps.indices) {
            for (j in apps.indices) {
                if (i == j) continue
                val a = apps[i].applicationId
                val b = apps[j].applicationId
                val shared = members.getValue(a).intersect(members.getValue(b))
                if (shared.isNotEmpty()) {
                    out +=
                        MiDependencyEdge(
                            fromApplicationId = a,
                            fromApplicationName = nameByApp.getValue(a),
                            toApplicationId = b,
                            toApplicationName = nameByApp.getValue(b),
                            sharedAssetCount = shared.size,
                        )
                }
            }
        }
        return out.sortedWith(
            compareBy({ it.fromApplicationName.lowercase() }, { it.toApplicationName.lowercase() }),
        )
    }

    private fun sharedAssets(
        apps: List<PortfolioAppRef>,
        graphByApp: Map<UUID, List<UUID>>,
        nameByApp: Map<UUID, String>,
    ): List<MiSharedAsset> {
        val owners = linkedMapOf<UUID, MutableSet<UUID>>()
        val entityById = linkedMapOf<UUID, BoMEntity>()
        for (app in apps) {
            for (gId in graphByApp.getValue(app.applicationId)) {
                for (id in namedGraphs.listEntityIdsInGraph(gId)) {
                    owners.getOrPut(id) { linkedSetOf() }.add(app.applicationId)
                    if (id !in entityById) {
                        store.getEntity(id)?.let { entityById[id] = it }
                    }
                }
            }
        }
        return owners.entries
            .filter { it.value.size > 1 }
            .map { (assetId, appIds) ->
                val entity = entityById.getValue(assetId)
                MiSharedAsset(
                    assetId = assetId,
                    label = AssetViews.label(entity.payload, entity.type),
                    type = entity.type,
                    applicationIds = appIds.sortedBy { nameByApp[it]?.lowercase() },
                    applicationNames = appIds.map { nameByApp.getValue(it) }.sortedBy { it.lowercase() },
                )
            }
            .sortedByDescending { it.applicationIds.size }
    }

    private fun duplicateAndRisk(graphIds: List<UUID>): Pair<List<MiDuplicateSignal>, List<MiRiskSignal>> {
        if (graphIds.isEmpty()) {
            return emptyList<MiDuplicateSignal>() to emptyList()
        }
        val contents = store.select(BoMGraphIdsMatcher(graphIds))
        val inScope = contents.entities.mapNotNull { it.id }.toSet()
        val dupes = mutableListOf<MiDuplicateSignal>()
        for (type in contents.entities.map { it.type }.distinct()) {
            for (group in store.findDuplicateGroups(type)) {
                val distinct = group.entities.filter { it.id in inScope }.distinctBy { it.id }
                if (distinct.size <= 1) continue
                dupes +=
                    MiDuplicateSignal(
                        type = type,
                        identity = group.identity,
                        assetIds = distinct.mapNotNull { it.id },
                        labels = distinct.map { AssetViews.label(it.payload, it.type) },
                    )
            }
        }
        val vulnEntities = contents.entities.count { it.type == "Vulnerability" }
        val vulnEdges = contents.edges.count { it.role == SbomRoles.HAS_VULNERABILITY }
        val risks = mutableListOf<MiRiskSignal>()
        if (vulnEntities > 0 || vulnEdges > 0) {
            risks +=
                MiRiskSignal(
                    kind = "vulnerability",
                    summary = "Vulnerability assets / HAS_VULNERABILITY relations in scope",
                    count = maxOf(vulnEntities, vulnEdges),
                )
        }
        risks +=
            MiRiskSignal(
                kind = "duplicate-groups",
                summary = "Identifier duplicate groups in latest-version selection",
                count = dupes.size,
            )
        return dupes.sortedBy { it.type } to risks
    }

    fun runTable(portfolioId: UUID, request: RunMiReportRequest): MiReportTable {
        val result = run(portfolioId, request)
        val (columns, allRows) = flatten(result)
        val p = request.page.coerceAtLeast(1)
        val s = request.size.coerceIn(1, 200)
        val from = ((p - 1) * s).coerceAtMost(allRows.size)
        val to = (from + s).coerceAtMost(allRows.size)
        return MiReportTable(
            portfolioId = portfolioId,
            level = result.level,
            includeSubcategories = request.includeSubcategories,
            report = result.report,
            title = result.title,
            columns = columns,
            rows = allRows.subList(from, to),
            total = allRows.size,
            page = p,
            size = s,
            notes = result.notes,
        )
    }

    fun csv(portfolioId: UUID, request: RunMiReportRequest): String {
        val result = run(portfolioId, request)
        val (columns, allRows) = flatten(result)
        val sb = StringBuilder()
        sb.appendLine(columns.joinToString(",") { csvCell(it) })
        for (row in allRows) {
            sb.appendLine(columns.joinToString(",") { csvCell(row[it] ?: "") })
        }
        return sb.toString()
    }

    private fun flatten(result: MiReportResult): Pair<List<String>, List<Map<String, String>>> {
        result.composition?.let { c ->
            val columns = listOf("type", "count")
            val rows =
                c.assetCountsByType.map { (type, count) ->
                    mapOf("type" to type, "count" to count.toString())
                } +
                    listOf(
                        mapOf("type" to "_relations", "count" to c.relationCount.toString()),
                        mapOf("type" to "_dependsOn", "count" to c.dependsOnCount.toString()),
                    )
            return columns to rows
        }
        result.dependencyMap?.let { edges ->
            val columns =
                listOf(
                    "fromApplicationId",
                    "fromApplicationName",
                    "toApplicationId",
                    "toApplicationName",
                    "sharedAssetCount",
                )
            return columns to
                edges.map {
                    mapOf(
                        "fromApplicationId" to it.fromApplicationId.toString(),
                        "fromApplicationName" to it.fromApplicationName,
                        "toApplicationId" to it.toApplicationId.toString(),
                        "toApplicationName" to it.toApplicationName,
                        "sharedAssetCount" to it.sharedAssetCount.toString(),
                    )
                }
        }
        result.sharedAssets?.let { assets ->
            val columns = listOf("assetId", "label", "type", "applicationNames")
            return columns to
                assets.map {
                    mapOf(
                        "assetId" to it.assetId.toString(),
                        "label" to it.label,
                        "type" to it.type,
                        "applicationNames" to it.applicationNames.joinToString("; "),
                    )
                }
        }
        if (result.duplicateSignals != null || result.riskSignals != null) {
            val columns = listOf("kind", "summary", "detail")
            val rows = mutableListOf<Map<String, String>>()
            for (d in result.duplicateSignals.orEmpty()) {
                rows +=
                    mapOf(
                        "kind" to "duplicate",
                        "summary" to d.type,
                        "detail" to d.labels.joinToString("; "),
                    )
            }
            for (r in result.riskSignals.orEmpty()) {
                rows +=
                    mapOf(
                        "kind" to r.kind,
                        "summary" to r.summary,
                        "detail" to r.count.toString(),
                    )
            }
            return columns to rows
        }
        return listOf("note") to result.notes.map { mapOf("note" to it) }
    }

    private fun csvCell(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    private fun title(report: MiReportId): String =
        when (report) {
            MiReportId.`MI-1` -> "Portfolio composition"
            MiReportId.`MI-2` -> "Application dependency map"
            MiReportId.`MI-3` -> "Shared asset hotspots"
            MiReportId.`MI-4` -> "Duplicate & risk signals"
        }
}
