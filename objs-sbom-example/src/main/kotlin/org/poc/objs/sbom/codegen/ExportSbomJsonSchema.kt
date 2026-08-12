package org.poc.objs.sbom.codegen

import org.poc.objs.core.domain.BoMJsonSchema
import org.poc.objs.core.domain.BoMJsonSchemaEdgeInclusion
import org.poc.objs.core.domain.BoMJsonSchemaExportOptions
import org.poc.objs.core.domain.FullCatalogJsonSchemaExporter
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog
import org.poc.objs.sbom.registry.SbomRegistry
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Writes the SBOM full-catalog JSON Schema (linked edges).
 *
 * Usage (via Gradle JavaExec):
 * `ExportSbomJsonSchema <output-dir>`
 *
 * Produces:
 * - `sbom-catalog-linked.schema.json` — full catalog with `$defs` + root props `$ref`ing each def
 * - `types/<DefKey>.json` — payload-only projection per ENTITY type (closer to Wave* payloads;
 *   no relation props)
 */
object ExportSbomJsonSchema {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isNotEmpty()) { "Usage: ExportSbomJsonSchema <output-dir>" }
        val outDir = Path.of(args[0])
        Files.createDirectories(outDir)
        val typesDir = outDir.resolve("types")
        Files.createDirectories(typesDir)

        val schemas = InMemoryBoMSchemaCatalog()
        val edges = InMemoryBoMAllowedEdgeCatalog()
        SbomRegistry.pack().registerInto(schemas, edges)

        val exporter = FullCatalogJsonSchemaExporter(schemas, edges)
        val linked = exporter.export(
            BoMJsonSchemaExportOptions(includeEdges = BoMJsonSchemaEdgeInclusion.LINKED),
        ).toMutableMap()

        @Suppress("UNCHECKED_CAST")
        val defs = linked["\$defs"] as? Map<String, Any?> ?: emptyMap()
        val rootProps = linkedMapOf<String, Any?>()
        for (key in defs.keys.sorted()) {
            rootProps[key.replaceFirstChar { it.lowercase() }] =
                linkedMapOf("\$ref" to "#/\$defs/$key")
        }
        linked["type"] = "object"
        linked["title"] = "SbomCatalog"
        linked["properties"] = rootProps

        val mapper = JsonMapper.builder().findAndAddModules().build()
        // Keep catalog at the jsonschema root.
        val catalogFile = outDir.resolve("sbom-catalog-linked.schema.json")
        mapper.writerWithDefaultPrettyPrinter().writeValue(catalogFile.toFile(), linked)

        // Payload-only schemas for TypedEntity / Wave* replacement experiments.
        for (schema in schemas.all().filter { it.usage == org.poc.objs.core.domain.BoMSchemaUsage.ENTITY }) {
            val projected = BoMJsonSchema.from(schema).toMutableMap()
            val defKey = FullCatalogJsonSchemaExporter.jsonSchemaDefKey(schema.type)
            val file = typesDir.resolve("$defKey.json")
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), projected)
        }

        println("Wrote ${catalogFile.toAbsolutePath()}")
        println("Wrote ${defs.size} \$defs; payload-only type schemas in $typesDir")
    }
}
