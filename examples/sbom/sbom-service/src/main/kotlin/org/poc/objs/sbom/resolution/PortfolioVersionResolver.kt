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
    val graphByApp: Map<UUID, List<UUID>>,
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
     * Current BOM graphs for the Assets tab: newest draft when present, else latest RELEASED.
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
        val graphByApp = linkedMapOf<UUID, List<UUID>>()
        val omitted = mutableListOf<PortfolioAppRef>()
        for (app in apps) {
            val draft =
                versionRows.findByApplicationIdAndStatus(app.applicationId, ApplicationVersionStatus.DRAFT)
                    .maxWithOrNull(
                        compareBy<org.poc.objs.sbom.persistence.SbomApplicationVersionRecord> { it.capturedAt }
                            .thenBy { it.id },
                    )
            val graphIds = draft?.let { bomGraphIds(it.id) } ?: latestIds[app.applicationId].orEmpty()
            if (graphIds.isEmpty()) {
                omitted += app
            } else {
                graphByApp[app.applicationId] = graphIds
            }
        }
        return GraphResolution(graphByApp, omitted)
    }

    private fun pinnedVersions(apps: List<PortfolioAppRef>): GraphResolution {
        val graphByApp = linkedMapOf<UUID, List<UUID>>()
        val omitted = mutableListOf<PortfolioAppRef>()
        for (app in apps) {
            val versionId = app.versionId
            if (versionId == null) {
                omitted += app
                continue
            }
            val row = versionRows.findByIdAndApplicationId(versionId, app.applicationId)
            val graphIds = row?.let { bomGraphIds(it.id) }.orEmpty()
            if (graphIds.isEmpty()) {
                omitted += app
            } else {
                graphByApp[app.applicationId] = graphIds
            }
        }
        return GraphResolution(graphByApp, omitted)
    }

    private fun bomGraphIds(versionId: UUID): List<UUID> =
        boms.findByVersionIdOrderBySortOrderAscIdAsc(versionId).map { it.graphId }
}
