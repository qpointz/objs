package org.poc.objs.sbom.domain

import java.util.UUID

enum class PortfolioUniqueness {
    UNIQUE_APP,
    UNIQUE_APP_VERSION,
    NOT_UNIQUE,
    ;

    companion object {
        fun parse(raw: String?): PortfolioUniqueness {
            if (raw.isNullOrBlank()) return UNIQUE_APP
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
                ?: error("Unknown uniqueness: $raw")
        }
    }
}

enum class PortfolioOrigin {
    MANUAL,
    AUTOMATED,
    ;

    companion object {
        fun parse(raw: String?): PortfolioOrigin {
            if (raw.isNullOrBlank()) return MANUAL
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
                ?: error("Unknown origin: $raw")
        }
    }
}

data class PortfolioSummary(
    val id: UUID,
    val name: String,
    val description: String?,
    val uniqueness: PortfolioUniqueness = PortfolioUniqueness.UNIQUE_APP,
    val origin: PortfolioOrigin = PortfolioOrigin.MANUAL,
    val source: String? = null,
)

data class CreatePortfolioRequest(
    val name: String,
    val description: String? = null,
    val uniqueness: String? = null,
    val origin: String? = null,
    val source: String? = null,
    val id: UUID? = null,
)

data class UpdatePortfolioRequest(
    val name: String? = null,
    val description: String? = null,
    val uniqueness: String? = null,
    val origin: String? = null,
    val source: String? = null,
)

data class CreateSubjectAreaRequest(
    val name: String,
    val description: String? = null,
    val parentId: UUID? = null,
    val id: UUID? = null,
)

data class UpdateSubjectAreaRequest(
    val name: String? = null,
    val description: String? = null,
)

data class PlaceApplicationRequest(
    val applicationId: UUID,
    val subjectAreaId: UUID? = null,
    val versionId: UUID? = null,
)

data class PortfolioTreeView(
    val portfolio: PortfolioSummary,
    val subjectAreas: List<SubjectAreaView>,
    val rootApplications: List<PortfolioAppRef>,
    val rootLeafCount: Int,
)

data class SubjectAreaView(
    val id: UUID,
    val name: String,
    val description: String?,
    val parentId: UUID?,
    val leafCount: Int,
    val applications: List<PortfolioAppRef>,
    val children: List<SubjectAreaView>,
)

data class PortfolioAppRef(
    val applicationId: UUID,
    val applicationName: String,
    val applicationDescription: String? = null,
    val placementId: UUID? = null,
    val nodeId: UUID? = null,
    val versionId: UUID? = null,
)

data class MovePlacementsRequest(
    val placementIds: List<UUID>,
    val subjectAreaId: UUID? = null,
)

data class DeletePlacementsRequest(
    val placementIds: List<UUID>,
)

data class PortfolioLevelApps(
    val portfolioId: UUID,
    val level: String,
    val includeSubcategories: Boolean,
    val applications: List<PortfolioAppRef>,
    val total: Int,
)

data class CategoryAssetRow(
    val assetId: UUID,
    val type: String,
    val label: String,
    val identity: Map<String, Any?>,
    val usedInApplicationIds: List<UUID>,
    val usedInApplicationNames: List<String>,
)

data class CategoryAssetPage(
    val portfolioId: UUID,
    val level: String,
    val includeSubcategories: Boolean,
    val items: List<CategoryAssetRow>,
    val total: Int,
    val page: Int,
    val size: Int,
    val notes: List<String> = emptyList(),
)

data class MiReportTable(
    val portfolioId: UUID,
    val level: String,
    val includeSubcategories: Boolean,
    val report: String,
    val title: String,
    val columns: List<String>,
    val rows: List<Map<String, String>>,
    val total: Int,
    val page: Int,
    val size: Int,
    val notes: List<String> = emptyList(),
)
