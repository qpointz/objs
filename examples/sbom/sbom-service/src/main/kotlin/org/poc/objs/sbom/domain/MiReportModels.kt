package org.poc.objs.sbom.domain

import java.util.UUID

enum class MiReportId {
    `MI-1`,
    `MI-2`,
    `MI-3`,
    `MI-4`,
    ;

    companion object {
        fun parse(raw: String): MiReportId =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
                ?: error("Unknown report: $raw")
    }
}

data class RunMiReportRequest(
    /** `root` or subject-area UUID. */
    val level: String = "root",
    val includeSubcategories: Boolean = true,
    /** MI-1 … MI-4 */
    val report: String,
    val versionResolution: String? = "LATEST",
    val page: Int = 1,
    val size: Int = 20,
)

data class MiReportResult(
    val portfolioId: UUID,
    val level: String,
    val report: String,
    val title: String,
    val applicationsInScope: List<PortfolioAppRef>,
    val applicationsWithoutVersion: List<PortfolioAppRef>,
    val graphCount: Int,
    val composition: MiCompositionSection? = null,
    val dependencyMap: List<MiDependencyEdge>? = null,
    val sharedAssets: List<MiSharedAsset>? = null,
    val duplicateSignals: List<MiDuplicateSignal>? = null,
    val riskSignals: List<MiRiskSignal>? = null,
    val notes: List<String> = emptyList(),
)

data class MiCompositionSection(
    val assetCountsByType: Map<String, Long>,
    val relationCount: Long,
    val dependsOnCount: Long,
)

data class MiDependencyEdge(
    val fromApplicationId: UUID,
    val fromApplicationName: String,
    val toApplicationId: UUID,
    val toApplicationName: String,
    val sharedAssetCount: Int,
)

data class MiSharedAsset(
    val assetId: UUID,
    val label: String,
    val type: String,
    val applicationIds: List<UUID>,
    val applicationNames: List<String>,
)

data class MiDuplicateSignal(
    val type: String,
    val identity: Map<String, Any?>,
    val assetIds: List<UUID>,
    val labels: List<String>,
)

data class MiRiskSignal(
    val kind: String,
    val summary: String,
    val count: Int,
)
