package org.poc.objs.sbom.domain

import java.util.UUID

/**
 * Product-facing view of an asset type from the graph schema catalog (G-A8).
 * Built from [org.poc.objs.core.domain.BoMSchemaCatalog], not from hand Wave* models.
 */
data class AssetTypeSummary(
    val type: String,
    val version: String,
    val title: String,
    val description: String,
)

data class AssetFieldHint(
    val path: String,
    val title: String,
    val fieldType: String,
    val searchable: Boolean,
    val identifier: Boolean,
)

data class AssetTypeDetail(
    val type: String,
    val version: String,
    val title: String,
    val description: String,
    val searchableFields: List<AssetFieldHint>,
    val identifierFields: List<AssetFieldHint>,
    val firstLevelScalarFields: List<AssetFieldHint>,
)

data class SchemaUsedInRef(
    val id: UUID,
    val name: String,
)

data class SchemaCatalogEntry(
    val type: String,
    val latestVersion: String,
    val versions: List<String>,
    val title: String,
    val description: String,
    val usage: String,
    val usedIn: List<SchemaUsedInRef>,
)
