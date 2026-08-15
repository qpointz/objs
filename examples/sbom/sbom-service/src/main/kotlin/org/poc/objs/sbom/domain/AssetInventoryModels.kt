package org.poc.objs.sbom.domain

import java.util.UUID

data class AssetSearchRequest(
    val type: String? = null,
    val schemaVersion: String? = null,
    /** Values keyed by schema **searchable** field path only. Used when [type] is set. */
    val filters: Map<String, String> = emptyMap(),
    /** Pool `obj-expr`. Combined with [type] when both are set. */
    val objExpr: String? = null,
)

data class AssetSearchPage(
    val items: List<AssetView>,
    val total: Long,
    val page: Int,
    val size: Int,
)

data class CreatePoolAssetRequest(
    val type: String,
    val schemaVersion: String? = null,
    val payload: Map<String, Any?>,
    /** Optional owning application **name** (G-P5). */
    val owner: String? = null,
)

data class SetAssetOwnerRequest(
    /** Application name, or null/omit to clear. */
    val owner: String? = null,
)

data class UpdatePoolAssetRequest(
    val payload: Map<String, Any?>,
)

data class AssetDetailView(
    val asset: AssetView,
    val usage: List<AssetUsageEntry>,
)

data class AssetUsageEntry(
    val applicationId: UUID,
    val applicationName: String,
    /** `DRAFT` or `VERSION`. */
    val context: String,
    val versionId: UUID? = null,
    val versionLabel: String? = null,
    val relations: List<AssetUsageRelation>,
)

data class AssetUsageRelation(
    val role: String,
    val label: String,
    /** `OUT` = this asset is source; `IN` = this asset is target. */
    val direction: String,
    val otherAssetId: UUID,
)

/** Find-only duplicate group (G-P7). */
data class AssetTypeStatistics(
    val type: String,
    val objectCount: Long,
)

data class AssetDuplicateGroup(
    val type: String,
    val schemaVersion: String,
    val identity: Map<String, Any?>,
    val assets: List<AssetView>,
)
