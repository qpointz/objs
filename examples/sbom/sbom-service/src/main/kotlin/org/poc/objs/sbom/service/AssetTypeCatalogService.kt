package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaField
import org.poc.objs.core.domain.BoMSchemaNode
import org.poc.objs.core.domain.BoMSchemaType
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.sbom.domain.AssetFieldHint
import org.poc.objs.sbom.domain.AssetTypeDetail
import org.poc.objs.sbom.domain.AssetTypeSummary
import org.poc.objs.sbom.domain.SemVerVersionComparer
import org.springframework.stereotype.Service

/**
 * Runtime asset-type model from the **graph schema catalog** (seed SoT / G-A8).
 * Journey 2 advanced search forms use [AssetTypeDetail.searchableFields] only.
 */
@Service
class AssetTypeCatalogService(
    private val schemas: BoMSchemaCatalog,
) {
    private val versions = SemVerVersionComparer()

    fun listEntityTypes(): List<AssetTypeSummary> =
        schemas.all()
            .filter { it.usage == BoMSchemaUsage.ENTITY }
            .groupBy { it.type }
            .toSortedMap()
            .map { (_, rows) ->
                val schema = rows.maxWith { a, b -> versions.compare(a.version, b.version) }
                AssetTypeSummary(
                    type = schema.type,
                    version = schema.version,
                    title = schema.contentSchema.title.ifBlank { schema.type },
                    description = schema.contentSchema.description.trim(),
                )
            }

    fun getEntityType(type: String, version: String? = null): AssetTypeDetail? {
        val schema = resolve(type, version) ?: return null
        val hints = collectFieldHints(schema.contentSchema)
        val firstLevel = firstLevelScalarFields(schema.contentSchema)
        return AssetTypeDetail(
            type = schema.type,
            version = schema.version,
            title = schema.contentSchema.title.ifBlank { schema.type },
            description = schema.contentSchema.description.trim(),
            searchableFields = hints.filter { it.searchable },
            identifierFields = hints.filter { it.identifier },
            firstLevelScalarFields = firstLevel,
        )
    }

    private fun resolve(type: String, version: String?): BoMSchema? {
        if (!version.isNullOrBlank()) {
            return schemas.get(type, version)?.takeIf { it.usage == BoMSchemaUsage.ENTITY }
        }
        return schemas.listByType(type)
            .filter { it.usage == BoMSchemaUsage.ENTITY }
            .maxWithOrNull { a, b -> versions.compare(a.version, b.version) }
    }

    private fun firstLevelScalarFields(root: BoMSchemaNode): List<AssetFieldHint> {
        require(root.type == BoMSchemaType.OBJECT) { "entity contentSchema must be OBJECT" }
        return root.fields.orEmpty().mapNotNull { field ->
            when (field.schema.type) {
                BoMSchemaType.OBJECT, BoMSchemaType.ARRAY -> null
                else ->
                    AssetFieldHint(
                        path = field.name,
                        title = fieldDisplayTitle(field),
                        fieldType = field.schema.type.name,
                        searchable = field.searchable,
                        identifier = field.identifier,
                    )
            }
        }
    }

    private fun collectFieldHints(root: BoMSchemaNode): List<AssetFieldHint> {
        require(root.type == BoMSchemaType.OBJECT) { "entity contentSchema must be OBJECT" }
        val out = mutableListOf<AssetFieldHint>()
        walk(root, prefix = "", out = out)
        return out
    }

    private fun walk(node: BoMSchemaNode, prefix: String, out: MutableList<AssetFieldHint>) {
        if (node.type != BoMSchemaType.OBJECT) return
        for (field in node.fields.orEmpty()) {
            val path = if (prefix.isEmpty()) field.name else "$prefix.${field.name}"
            when (field.schema.type) {
                BoMSchemaType.OBJECT -> walk(field.schema, path, out)
                BoMSchemaType.ARRAY -> Unit // searchable/identifier not allowed under ARRAY
                else -> maybeAddScalar(path, field, out)
            }
        }
    }

    private fun maybeAddScalar(path: String, field: BoMSchemaField, out: MutableList<AssetFieldHint>) {
        if (!field.searchable && !field.identifier) return
        out += AssetFieldHint(
            path = path,
            title = fieldDisplayTitle(field),
            fieldType = field.schema.type.name,
            searchable = field.searchable,
            identifier = field.identifier,
        )
    }

    companion object {
        /** Shared scalar nodes in SbomRegistry use type titles, not field titles. */
        private val GENERIC_SCALAR_TITLES =
            setOf("Text", "URI", "Date and time", "Number", "Integer", "Boolean")

        internal fun fieldDisplayTitle(field: BoMSchemaField): String {
            val title = field.schema.title.trim()
            if (title.isNotEmpty() && title !in GENERIC_SCALAR_TITLES) {
                return title
            }
            return field.name
        }
    }
}
