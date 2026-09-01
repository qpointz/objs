package org.poc.objs.sbom.service

import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphFragment
import org.poc.objs.api.domain.ResolvedGraphFragment
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.sbom.domain.BomUnion
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.persistence.SbomApplicationSbomRepository
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.poc.objs.sbom.registry.SbomRoles
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

/**
 * Weak CycloneDX JSON export (G-P11 / R16) — demo of same BOM, different format.
 * Not a certified exporter; omits non-Component types freely.
 */
@Service
class CycloneDxExportService(
    private val applications: SbomApplicationRepository,
    private val versions: SbomApplicationVersionRepository,
    private val boms: SbomApplicationSbomRepository,
    private val namedGraphs: NamedGraphStore,
) {
    fun exportDraft(applicationId: UUID): Map<String, Any?> {
        val app = requireApp(applicationId)
        val draft =
            versions.findByApplicationIdAndStatus(applicationId, "DRAFT")
                .maxWithOrNull(
                    compareBy<org.poc.objs.sbom.persistence.SbomApplicationVersionRecord> { it.capturedAt }
                        .thenBy { it.id },
                )
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No draft for application: $applicationId")
        return exportContents(
            contents = unionOf(draft.id),
            applicationName = app.name,
            versionLabel = "draft",
        )
    }

    fun exportVersion(applicationId: UUID, versionId: UUID): Map<String, Any?> {
        val app = requireApp(applicationId)
        val version =
            versions.findByIdAndApplicationId(versionId, applicationId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found: $versionId")
        return exportContents(
            contents = unionOf(version.id),
            applicationName = app.name,
            versionLabel = version.version.ifBlank { null } ?: version.label ?: version.capturedAt.toString(),
        )
    }

    private fun exportContents(
        contents: GraphFragment,
        applicationName: String,
        versionLabel: String,
    ): Map<String, Any?> {
        val components =
            contents.entities
                .filter { it.type == "Component" }
                .mapNotNull { it.toCdxComponent() }
        val componentIds = components.mapNotNull { it["bom-ref"] as? String }.toSet()

        val dependsOnByRef = linkedMapOf<String, MutableList<String>>()
        for (edge in contents.edges) {
            if (edge.role != SbomRoles.DEPENDS_ON) continue
            val from = edge.source.toString()
            val to = edge.target.toString()
            if (from !in componentIds || to !in componentIds) continue
            dependsOnByRef.getOrPut(from) { mutableListOf() }.add(to)
        }

        val dependencies = mutableListOf<Map<String, Any?>>()
        // Include every component ref so tools can walk the tree; empty dependsOn when none.
        for (ref in componentIds) {
            dependencies +=
                mapOf(
                    "ref" to ref,
                    "dependsOn" to (dependsOnByRef[ref]?.distinct()?.sorted() ?: emptyList()),
                )
        }

        val serial = UUID.randomUUID()
        return linkedMapOf(
            "bomFormat" to "CycloneDX",
            "specVersion" to "1.6",
            "serialNumber" to "urn:uuid:$serial",
            "version" to 1,
            "metadata" to
                linkedMapOf(
                    "timestamp" to Instant.now().toString(),
                    "component" to
                        linkedMapOf(
                            "type" to "application",
                            "name" to applicationName,
                            "version" to versionLabel,
                            "bom-ref" to "app:$applicationName@$versionLabel",
                        ),
                    "tools" to
                        listOf(
                            linkedMapOf(
                                "vendor" to "objs-poc",
                                "name" to "sbom-inventory-weak-export",
                                "version" to "demo",
                            ),
                        ),
                ),
            "components" to components,
            "dependencies" to dependencies,
        )
    }

    private fun Entity.toCdxComponent(): Map<String, Any?>? {
        val id = id ?: return null
        val name = payload["name"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
        val version = payload["version"]?.toString()?.takeIf { it.isNotBlank() } ?: "0.0.0"
        val kind = payload["kind"]?.toString()?.lowercase()
        val type =
            when (kind) {
                "framework", "library", "sdk" -> if (kind == "framework") "framework" else "library"
                else -> "library"
            }
        val out =
            linkedMapOf<String, Any?>(
                "type" to type,
                "bom-ref" to id.toString(),
                "name" to name,
                "version" to version,
            )
        payload["coordinates"]?.toString()?.takeIf { it.isNotBlank() }?.let { out["purl"] = it }
        payload["ecosystem"]?.toString()?.takeIf { it.isNotBlank() }?.let { eco ->
            out["properties"] =
                listOf(
                    mapOf("name" to "ecosystem", "value" to eco),
                )
        }
        return out
    }

    private fun unionOf(versionId: UUID): ResolvedGraphFragment {
        val graphIds = boms.findByVersionIdOrderBySortOrderAscIdAsc(versionId).map { it.graphId }
        if (graphIds.isEmpty()) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Version has no BOM")
        }
        val resolved = BomUnion.of(graphIds.mapNotNull { namedGraphs.get(it) })
        if (resolved.hasErrors()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                resolved.diagnostics.joinToString("; ") { it.message },
            )
        }
        return resolved
    }

    private fun requireApp(id: UUID) =
        applications.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: $id")
        }
}
