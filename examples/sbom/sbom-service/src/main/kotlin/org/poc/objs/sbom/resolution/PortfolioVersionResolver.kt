package org.poc.objs.sbom.resolution

import org.poc.objs.sbom.domain.PortfolioAppRef
import org.poc.objs.sbom.domain.PortfolioUniqueness
import org.poc.objs.sbom.persistence.ApplicationVersionStatus
import org.poc.objs.sbom.persistence.SbomApplicationSbomRepository
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.poc.objs.sbom.service.ApplicationVersionService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class GraphResolution(
    val graphByApp: Map<UUID, UUID>,
    val omitted: List<PortfolioAppRef>,
)

fun interface PortfolioVersionResolver {
    fun resolve(apps: List<PortfolioAppRef>): GraphResolution
}

@Component
class LatestVersionResolver(
    private val versions: ApplicationVersionService,
) : PortfolioVersionResolver {
    override fun resolve(apps: List<PortfolioAppRef>): GraphResolution {
        val latest = versions.latestGraphIds(apps.map { it.applicationId })
        val withVersion = apps.filter { it.applicationId in latest }
        val omitted = apps.filter { it.applicationId !in latest }
        return GraphResolution(
            graphByApp = withVersion.associate { it.applicationId to latest.getValue(it.applicationId) },
            omitted = omitted,
        )
    }
}

@Component
class PortfolioGraphSelector(
    private val latest: LatestVersionResolver,
    private val versionRows: SbomApplicationVersionRepository,
    private val boms: SbomApplicationSbomRepository,
) {
    fun select(
        uniqueness: PortfolioUniqueness,
        apps: List<PortfolioAppRef>,
        versionResolution: String?,
    ): GraphResolution {
        if (uniqueness == PortfolioUniqueness.UNIQUE_APP_VERSION) {
            return pinnedVersions(apps)
        }
        val key = versionResolution?.trim()?.ifEmpty { null } ?: "LATEST"
        if (!key.equals("LATEST", ignoreCase = true)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown versionResolution: $key")
        }
        return latest.resolve(apps)
    }

    /**
     * Current BOM for the Assets tab: edit draft when present, else latest version.
     * [UNIQUE_APP_VERSION] still uses the pinned version on the placement.
     */
    fun selectCurrentBom(
        uniqueness: PortfolioUniqueness,
        apps: List<PortfolioAppRef>,
    ): GraphResolution {
        if (uniqueness == PortfolioUniqueness.UNIQUE_APP_VERSION) {
            return pinnedVersions(apps)
        }
        val latestIds = latest.resolve(apps).graphByApp
        val graphByApp = linkedMapOf<UUID, UUID>()
        val omitted = mutableListOf<PortfolioAppRef>()
        for (app in apps) {
            val graphId =
                versionRows.findByApplicationIdAndStatus(app.applicationId, ApplicationVersionStatus.DRAFT)
                    .firstOrNull()
                    ?.let { bomGraphId(it.id) }
                    ?: latestIds[app.applicationId]
            if (graphId == null) {
                omitted += app
            } else {
                graphByApp[app.applicationId] = graphId
            }
        }
        return GraphResolution(graphByApp, omitted)
    }

    private fun pinnedVersions(apps: List<PortfolioAppRef>): GraphResolution {
        val graphByApp = linkedMapOf<UUID, UUID>()
        val omitted = mutableListOf<PortfolioAppRef>()
        for (app in apps) {
            val versionId = app.versionId
            if (versionId == null) {
                omitted += app
                continue
            }
            val row = versionRows.findByIdAndApplicationId(versionId, app.applicationId)
            if (row == null) {
                omitted += app
            } else {
                val graphId = bomGraphId(row.id)
                if (graphId == null) {
                    omitted += app
                } else {
                    graphByApp[app.applicationId] = graphId
                }
            }
        }
        return GraphResolution(graphByApp, omitted)
    }

    private fun bomGraphId(versionId: UUID): UUID? =
        boms.findByVersionIdOrderBySortOrderAscIdAsc(versionId).firstOrNull()?.graphId
}
