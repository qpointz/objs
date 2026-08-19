package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMCatalogSupport
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.sbom.domain.AssetFieldHint
import org.poc.objs.sbom.domain.AssetTypeDetail
import org.poc.objs.sbom.domain.AssetTypeSummary
import org.springframework.stereotype.Service

/**
 * Runtime asset-type model from the graph schema catalog (seed SoT / G-A8).
 * Journey 2 advanced search forms use [AssetTypeDetail.searchableFields] only.
 */
@Service
class AssetTypeCatalogService(
    private val schemas: BoMSchemaCatalog,
    private val catalog: BoMCatalogSupport,
) {
    fun listEntityTypes(): List<AssetTypeSummary> =
        catalog.latestEntitySchemas().map { schema ->
            AssetTypeSummary(
                type = schema.type,
                version = schema.version,
                title = schema.contentSchema.title.ifBlank { schema.type },
                description = schema.contentSchema.description.trim(),
            )
        }

    fun getEntityType(type: String, version: String? = null): AssetTypeDetail? {
        val schema = resolve(type, version) ?: return null
        val hints = catalog.fieldHints(schema)
        val firstLevel = catalog.firstLevelScalarFields(schema)
        return AssetTypeDetail(
            type = schema.type,
            version = schema.version,
            title = schema.contentSchema.title.ifBlank { schema.type },
            description = schema.contentSchema.description.trim(),
            searchableFields = hints.filter { it.searchable }.map { it.toHint() },
            identifierFields = hints.filter { it.identifier }.map { it.toHint() },
            firstLevelScalarFields = firstLevel.map { it.toHint() },
        )
    }

    private fun resolve(type: String, version: String?): BoMSchema? {
        if (!version.isNullOrBlank()) {
            return schemas.get(type, version)?.takeIf { it.usage == BoMSchemaUsage.ENTITY }
        }
        return catalog.latestEntitySchema(type)
    }

    private fun org.poc.objs.core.domain.BoMFieldHint.toHint() =
        AssetFieldHint(
            path = path,
            title = title,
            fieldType = fieldType,
            searchable = searchable,
            identifier = identifier,
        )
}
