package org.poc.objs.sbom.codegen

import org.poc.objs.core.domain.InMemoryAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemorySchemaCatalog
import org.poc.objs.core.seed.AllowedEdgeRuleSeedHandler
import org.poc.objs.core.seed.ObjectSchemaSeedHandler
import org.poc.objs.core.seed.SeedYaml
import org.poc.objs.sbom.registry.SbomRegistry
import java.nio.file.Files
import java.nio.file.Path

/**
 * Regenerates `seeds/sbom-ontology.yaml` from [SbomRegistry] (parity with typed pack).
 *
 * `./gradlew :sbom-service:exportSbomOntology`
 */
object ExportSbomOntology {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isNotEmpty()) { "Usage: ExportSbomOntology <output-yaml-path>" }
        val out = Path.of(args[0])
        Files.createDirectories(out.parent)

        val schemas = InMemorySchemaCatalog()
        val edges = InMemoryAllowedEdgeCatalog()
        SbomRegistry.pack().registerInto(schemas, edges)

        val schemaHandler = ObjectSchemaSeedHandler(schemas)
        val edgeHandler = AllowedEdgeRuleSeedHandler(edges)
        val documents = mutableListOf<Map<String, Any?>>()
        schemas.all()
            .sortedWith(compareBy({ it.type }, { it.version }))
            .forEach { documents += schemaHandler.serialize(it) }
        edges.all()
            .sortedWith(compareBy({ it.sourceType }, { it.role }, { it.targetType }))
            .forEach { documents += edgeHandler.serialize(it) }

        Files.writeString(out, SeedYaml.writeDocuments(documents))
        println("Wrote $out (${documents.size} documents)")
    }
}
