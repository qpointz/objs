package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMIdentityProjection
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.sbom.domain.CategoryAssetPage
import org.poc.objs.sbom.domain.CategoryAssetRow
import org.poc.objs.sbom.resolution.PortfolioGraphSelector
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CategoryAssetsService(
    private val portfolios: PortfolioService,
    private val graphs: PortfolioGraphSelector,
    private val namedGraphs: BoMNamedGraphStore,
    private val schemas: BoMSchemaCatalog,
) {
    fun list(
        portfolioId: UUID,
        level: String,
        includeSubcategories: Boolean,
        page: Int,
        size: Int,
    ): CategoryAssetPage {
        val tree = portfolios.getTree(portfolioId)
        val apps = portfolios.applicationsInScope(portfolioId, level, includeSubcategories)
        val resolution = graphs.selectCurrentBom(tree.portfolio.uniqueness, apps)
        val nameByApp = apps.associate { it.applicationId to it.applicationName }
        data class Acc(val entity: BoMEntity, val apps: MutableSet<UUID>)
        val groups = linkedMapOf<String, Acc>()
        for ((appId, graphIds) in resolution.graphByApp) {
            for (graphId in graphIds) {
                val contents = namedGraphs.get(graphId)?.contents ?: continue
                for (entity in contents.entities) {
                    val schema =
                        schemas.listByType(entity.type)
                            .filter { it.usage == BoMSchemaUsage.ENTITY }
                            .maxByOrNull { it.version }
                    val identity = schema?.let { BoMIdentityProjection.project(it.contentSchema, entity.payload) }.orEmpty()
                    val identityKey =
                        if (identity.isEmpty()) {
                            entity.id?.toString() ?: continue
                        } else {
                            identity.entries.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" }
                        }
                    val key = "${entity.type}|$identityKey"
                    groups.getOrPut(key) { Acc(entity, linkedSetOf()) }.apps.add(appId)
                }
            }
        }
        val rows =
            groups.values
                .map { acc ->
                    val schema =
                        schemas.listByType(acc.entity.type)
                            .filter { it.usage == BoMSchemaUsage.ENTITY }
                            .maxByOrNull { it.version }
                    val identity =
                        schema?.let { BoMIdentityProjection.project(it.contentSchema, acc.entity.payload) }.orEmpty()
                    val appIds = acc.apps.sortedBy { nameByApp[it]?.lowercase() }
                    CategoryAssetRow(
                        assetId = acc.entity.id ?: UUID(0, 0),
                        type = acc.entity.type,
                        label = AssetViews.label(acc.entity.payload, acc.entity.type),
                        identity = identity,
                        usedInApplicationIds = appIds,
                        usedInApplicationNames = appIds.mapNotNull { nameByApp[it] },
                    )
                }
                .sortedWith(compareBy({ it.type.lowercase() }, { it.label.lowercase() }))
        val p = page.coerceAtLeast(1)
        val s = size.coerceIn(1, 200)
        val from = ((p - 1) * s).coerceAtMost(rows.size)
        val to = (from + s).coerceAtMost(rows.size)
        return CategoryAssetPage(
            portfolioId = portfolioId,
            level = level,
            includeSubcategories = includeSubcategories,
            items = rows.subList(from, to),
            total = rows.size,
            page = p,
            size = s,
            notes =
                if (resolution.omitted.isEmpty()) {
                    emptyList()
                } else {
                    listOf("${resolution.omitted.size} application(s) have no BOM to list")
                },
        )
    }
}
