package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.typed.mergeAnnotations
import org.poc.objs.core.validation.BoMValidationResult
import org.poc.objs.sbom.annotations.SbomAnnotationKeys
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.model.SbomApplicationCatalog
import org.poc.objs.sbom.model.SbomApplicationVersions
import org.poc.objs.sbom.registry.SbomRegistry
import org.springframework.stereotype.Service

@Service
class SbomService(
    private val store: BoMGraphStore,
    private val schemas: BoMSchemaCatalog,
    private val edgeRules: BoMAllowedEdgeCatalog,
) {
    private var packRegistered = false

    fun ensureRegistry() {
        if (!packRegistered) {
            SbomRegistry.pack().registerInto(schemas, edgeRules)
            packRegistered = true
        }
    }

    fun save(graph: BoMGraph): BoMValidationResult {
        ensureRegistry()
        return store.write(graph)
    }

    /**
     * Upsert [graph] into [context], applying [requestAnnotations] as defaults
     * (body entity annotations override).
     */
    fun save(
        context: SbomContext,
        graph: BoMGraph,
        requestAnnotations: Map<String, String> = emptyMap(),
    ): BoMValidationResult {
        ensureRegistry()
        val defaults = context.toAnnotations() + requestAnnotations
        for (entity in graph.entities) {
            entity.annotations = mergeAnnotations(defaults, entity.annotations)
            // Path identity always wins for app / appVersion
            entity.annotations[SbomAnnotationKeys.APP] = context.app
            entity.annotations[SbomAnnotationKeys.APP_VERSION] = context.appVersion
        }
        return store.write(graph)
    }

    fun getSbom(app: String, appVersion: String? = null, extra: Map<String, String> = emptyMap()): BoMSubgraph {
        val filter = buildMap {
            put(SbomAnnotationKeys.APP, app)
            if (appVersion != null) {
                put(SbomAnnotationKeys.APP_VERSION, appVersion)
            }
            putAll(extra)
        }
        return store.selectSubgraphMatchAll(filter)
    }

    fun getSbom(context: SbomContext, extra: Map<String, String> = emptyMap()): BoMSubgraph =
        getSbom(context.app, context.appVersion, extra)

    /**
     * Distinct `(app, appVersion)` partitions present in the store.
     * Apps and versions are sorted lexicographically; entities missing either key are skipped.
     */
    fun listApplications(): SbomApplicationCatalog {
        val versionsByApp = linkedMapOf<String, MutableSet<String>>()
        for (entity in store.loadAll().entities) {
            val app = entity.annotations[SbomAnnotationKeys.APP]?.takeIf { it.isNotBlank() } ?: continue
            val version = entity.annotations[SbomAnnotationKeys.APP_VERSION]?.takeIf { it.isNotBlank() } ?: continue
            versionsByApp.getOrPut(app) { linkedSetOf() }.add(version)
        }
        val applications = versionsByApp.entries
            .sortedBy { it.key }
            .map { (app, versions) ->
                SbomApplicationVersions(app = app, versions = versions.sorted())
            }
        return SbomApplicationCatalog(applications = applications)
    }
}
