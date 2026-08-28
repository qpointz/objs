package org.poc.objs.sbom.web

import org.poc.objs.core.domain.AllowedEdgeCatalog
import org.poc.objs.core.domain.Schema
import org.poc.objs.sbom.domain.AssetRelationshipSpec
import org.poc.objs.sbom.domain.AssetTypeDetail
import org.poc.objs.sbom.domain.AssetTypeSummary
import org.poc.objs.sbom.domain.SchemaCatalogEntry
import org.poc.objs.sbom.domain.SchemaUsedInRef
import org.poc.objs.sbom.service.AssetTypeCatalogService
import org.poc.objs.sbom.service.RelationLabels
import org.poc.objs.sbom.service.SchemaBrowseService
import org.poc.objs.sbom.service.SbomService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Product-language schema browse for Applications chrome (Journey 2 forms).
 * Backed by objs SchemaCatalog — not foundation registry REST.
 */
@RestController
@RequestMapping("/api/v1/inventory")
class InventorySchemaController(
    private val assetTypes: AssetTypeCatalogService,
    private val schemaBrowse: SchemaBrowseService,
    private val sbom: SbomService,
    private val edges: AllowedEdgeCatalog,
) {
    @GetMapping("/asset-types")
    fun listAssetTypes(): List<AssetTypeSummary> = assetTypes.listEntityTypes()

    @GetMapping("/asset-types/{type}")
    fun getAssetType(
        @PathVariable type: String,
        @RequestParam(required = false) version: String?,
    ): AssetTypeDetail =
        assetTypes.getEntityType(type, version)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown asset type: $type")

    @GetMapping("/schema-catalog")
    fun schemaCatalog(): List<SchemaCatalogEntry> = schemaBrowse.catalog()

    @GetMapping("/schema-catalog/{type}/used-in")
    fun schemaUsedIn(@PathVariable type: String): List<SchemaUsedInRef> =
        schemaBrowse.usedInForType(type)

    @GetMapping("/schemas")
    fun listSchemas(@RequestParam(required = false) type: String?): List<Schema> =
        schemaBrowse.list(type)

    @GetMapping("/schemas/{type}")
    fun listSchemasByType(@PathVariable type: String): List<Schema> =
        schemaBrowse.listByType(type)

    @GetMapping("/schemas/{type}/{version}")
    fun getSchema(
        @PathVariable type: String,
        @PathVariable version: String,
    ): Schema = schemaBrowse.get(type, version)

    @GetMapping("/schema-catalog/{type}/allowed-edges")
    fun allowedEdgesForType(@PathVariable type: String) = schemaBrowse.allowedEdgesForType(type)

    @GetMapping("/relation-labels")
    fun relationLabel(@RequestParam role: String): Map<String, String> =
        mapOf("role" to role, "label" to RelationLabels.display(role))

    @GetMapping("/asset-types/{type}/relationships")
    fun relationshipsForType(@PathVariable type: String): List<AssetRelationshipSpec> {
        sbom.ensureRegistry()
        return edges.all().flatMap { rule ->
            val out =
                if (rule.sourceType.equals(type, ignoreCase = true)) {
                    listOf(
                        AssetRelationshipSpec(
                            role = rule.role,
                            label = RelationLabels.display(rule.role),
                            targetType = rule.targetType,
                            section = RelationLabels.display(rule.role),
                            cardinality = rule.cardinality.name,
                            direction = "OUT",
                        ),
                    )
                } else {
                    emptyList()
                }
            val incoming =
                if (rule.targetType.equals(type, ignoreCase = true)) {
                    listOf(
                        AssetRelationshipSpec(
                            role = rule.role,
                            label = RelationLabels.display(rule.role),
                            targetType = rule.sourceType,
                            section = RelationLabels.display(rule.role),
                            cardinality = rule.cardinality.name,
                            direction = "IN",
                        ),
                    )
                } else {
                    emptyList()
                }
            out + incoming
        }
    }
}
