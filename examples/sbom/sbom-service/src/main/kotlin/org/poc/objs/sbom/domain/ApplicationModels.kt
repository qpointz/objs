package org.poc.objs.sbom.domain

import java.util.UUID

data class ApplicationSummary(
    val id: UUID,
    val name: String,
    val description: String?,
)

data class CreateApplicationRequest(
    val name: String,
    val description: String? = null,
    val id: UUID? = null,
)

data class UpdateApplicationRequest(
    val name: String? = null,
    val description: String? = null,
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
)

data class PromoteVersionRequest(
    val version: String,
)

data class CreateFingerprintRequest(
    val note: String? = null,
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
)

data class VersionBomView(
    val version: ApplicationVersionSummary,
    val applicationName: String,
    val assets: List<AssetView>,
    val relations: List<RelationView>,
)

data class ApplicationFingerprintSummary(
    val id: UUID,
    val versionId: UUID,
    val createdAt: java.time.Instant,
    val note: String?,
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
