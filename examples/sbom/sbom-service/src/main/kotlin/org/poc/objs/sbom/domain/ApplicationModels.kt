package org.poc.objs.sbom.domain

import java.util.UUID

data class ApplicationSummary(
    val id: UUID,
    val name: String,
    val description: String?,
    val tags: List<String> = emptyList(),
)

data class CreateApplicationRequest(
    val name: String,
    val description: String? = null,
    val id: UUID? = null,
    val targetVersion: String? = null,
    val tags: List<String> = emptyList(),
)

data class UpdateApplicationRequest(
    val name: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
)

/** Product view of an edit-draft BOM (no graph vocabulary). */
data class DraftBomView(
    val applicationId: UUID,
    val applicationName: String,
    val assets: List<AssetView>,
    val relations: List<RelationView>,
)

data class AssetView(
    val id: UUID,
    val type: String,
    val schemaVersion: String,
    val label: String,
    val payload: Map<String, Any?>,
    val owner: String?,
)

data class RelationView(
    val id: UUID,
    val role: String,
    val label: String,
    val fromAssetId: UUID,
    val toAssetId: UUID,
)

/**
 * Attach an existing pool asset, or create one then attach.
 * Provide [assetId] **or** [type] + [payload] (not both).
 */
data class DraftAssetWrite(
    val assetId: UUID? = null,
    val type: String? = null,
    val schemaVersion: String? = null,
    val payload: Map<String, Any?>? = null,
    /** When creating, set annotation owner = application name (G-P5). */
    val setOwner: Boolean = false,
)

data class DraftRelationWrite(
    val fromAssetId: UUID,
    val toAssetId: UUID,
    val role: String,
)

/** Inferred app→app dependency via shared assets (G-P4). */
data class InferredAppDependency(
    val applicationId: UUID,
    val applicationName: String,
    val sharedAssetIds: List<UUID>,
)

data class CreateDraftVersionRequest(
    val fromVersionId: UUID? = null,
    val fromFingerprintId: UUID? = null,
    val targetVersion: String? = null,
    val combineConstituents: Boolean? = null,
)

data class RenameVersionRequest(
    val version: String,
)

data class PatchVersionRequest(
    val version: String? = null,
    val tags: List<String>? = null,
)

data class ApplicationPortalStats(
    val applicationId: UUID,
    val versionCount: Int,
    val bomCount: Int,
    val latestVersion: ApplicationVersionSummary?,
    val latestMultiBom: Boolean,
)

data class PromoteVersionRequest(
    val version: String,
)

data class CreateFingerprintRequest(
    val note: String? = null,
    val name: String? = null,
    val category: String? = null,
)

data class CreateBomRequest(
    val name: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
)

data class UpdateBomRequest(
    val name: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
)

data class BomSummary(
    val id: UUID,
    val versionId: UUID,
    val name: String,
    val description: String?,
    val tags: List<String>,
    val sortOrder: Int,
)

data class CombinedBomView(
    val version: ApplicationVersionSummary,
    val applicationName: String,
    val assets: List<AssetView>,
    val relations: List<RelationView>,
    val combinedTags: List<String>,
    val selectedBomIds: List<UUID>,
)

data class ReplaceVersionBomRequest(
    val assetIds: List<UUID> = emptyList(),
    val relations: List<DraftRelationWrite> = emptyList(),
)

data class ApplicationVersionSummary(
    val id: UUID,
    val applicationId: UUID,
    val status: String,
    val version: String?,
    val label: String?,
    val capturedAt: java.time.Instant,
    val promotedAt: java.time.Instant?,
    val tags: List<String> = emptyList(),
    val basedOnVersionId: UUID? = null,
    val basedOnFingerprintId: UUID? = null,
)

data class VersionBomView(
    val version: ApplicationVersionSummary,
    val applicationName: String,
    val assets: List<AssetView>,
    val relations: List<RelationView>,
    val combinedTags: List<String> = emptyList(),
)

data class ApplicationFingerprintSummary(
    val id: UUID,
    val versionId: UUID,
    val createdAt: java.time.Instant,
    val note: String?,
    val name: String = note.orEmpty(),
    val category: String = "unknown",
    val contentSha256: String,
)

data class AssetRelationshipSpec(
    val role: String,
    val label: String,
    val targetType: String,
    val section: String,
    val cardinality: String,
    /** `OUT` = this type is the source; `IN` = this type is the target. */
    val direction: String = "OUT",
)
