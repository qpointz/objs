package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.match.BoMChainedMatcher
import org.poc.objs.core.match.BoMGraphExprMatcher
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.match.BoMObjExprMatcher
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.typed.mergeAnnotations
import org.poc.objs.core.validation.BoMValidationResult
import org.poc.objs.sbom.annotations.SbomAnnotationKeys
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.model.SbomApplicationCatalog
import org.poc.objs.sbom.model.SbomApplicationVersions
import org.poc.objs.sbom.registry.SbomRegistry
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * SBOM persistence facade: **one `bom_graph` per `(app, appVersion)` snapshot** (WI-006).
 *
 * Each app-version graph id is derived deterministically from the context so repeated saves land
 * in the same graph; the header is created lazily on first write. This is an *application-level*
 * partitioning choice (graph-per-snapshot), not a foundation feature — objs core keeps no
 * parent/lineage columns on `bom_graph` (any snapshot genealogy an app wants stays in its own
 * annotations/tables, never in objs foundation columns).
 */
@Service
class SbomService(
    private val graphs: BoMNamedGraphStore,
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

    /**
     * Upsert [graph] into the `(app, appVersion)` graph inferred from its entities' annotations
     * (every entity in [graph] must already carry both — see [SbomGraphBuilder]).
     */
    fun save(graph: BoMGraph): BoMValidationResult {
        ensureRegistry()
        return save(contextOf(graph), graph)
    }

    /**
     * Upsert [graph] into the graph for [context], applying [requestAnnotations] as defaults
     * (body entity annotations override). Creates the `(app, appVersion)` graph header on first
     * write for this context; later writes reuse the same graph id.
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
        val graphId = ensureGraph(context)
        return graphs.mutate(
            graphId,
            BoMGraphMutation.of(graph),
        )
    }

    /**
     * Fetch the SBOM for [app] (all versions when [appVersion] is `null`).
     *
     * Selection is always via `graph-expr` on graph **headers** (optionally chained with
     * `obj-expr` for provenance filters) — never a whole-pool scan. Versioned fetch matches
     * `a.app` + `a.appVersion`; all-versions matches `a.app` only.
     */
    fun getSbom(app: String, appVersion: String? = null, extra: Map<String, String> = emptyMap()): BoMGraphContents {
        val graphExpr = if (appVersion != null) {
            BoMGraphExprMatcher(
                "a.${SbomAnnotationKeys.APP} == '${escape(app)}' && " +
                    "a.${SbomAnnotationKeys.APP_VERSION} == '${escape(appVersion)}'",
            )
        } else {
            BoMGraphExprMatcher("a.${SbomAnnotationKeys.APP} == '${escape(app)}'")
        }
        val objFilter = objExprMatcher(extra)
        val matcher: BoMMatcher = objFilter?.let { BoMChainedMatcher(listOf(graphExpr, it)) } ?: graphExpr
        return store.select(matcher)
    }

    fun getSbom(context: SbomContext, extra: Map<String, String> = emptyMap()): BoMGraphContents =
        getSbom(context.app, context.appVersion, extra)

    /**
     * Distinct `(app, appVersion)` partitions from **graph headers** (one graph per snapshot).
     * Apps and versions are sorted lexicographically; headers missing either key are skipped.
     */
    fun listApplications(): SbomApplicationCatalog {
        val versionsByApp = linkedMapOf<String, MutableSet<String>>()
        for (item in graphs.list()) {
            val app = item.annotations[SbomAnnotationKeys.APP]?.takeIf { it.isNotBlank() } ?: continue
            val version = item.annotations[SbomAnnotationKeys.APP_VERSION]?.takeIf { it.isNotBlank() } ?: continue
            versionsByApp.getOrPut(app) { linkedSetOf() }.add(version)
        }
        val applications = versionsByApp.entries
            .sortedBy { it.key }
            .map { (app, versions) ->
                SbomApplicationVersions(app = app, versions = versions.sorted())
            }
        return SbomApplicationCatalog(applications = applications)
    }

    /** Deterministic graph id for an `(app, appVersion)` snapshot; stable across saves. */
    private fun graphIdFor(context: SbomContext): UUID =
        UUID.nameUUIDFromBytes("sbom-graph:${context.app}:${context.appVersion}".toByteArray())

    private fun ensureGraph(context: SbomContext): UUID {
        val id = graphIdFor(context)
        if (graphs.get(id) == null) {
            graphs.create(BoMGraphSpec(id = id, annotations = context.toAnnotations()))
        }
        return id
    }

    private fun contextOf(graph: BoMGraph): SbomContext {
        val annotations = graph.entities.firstOrNull()?.annotations
            ?: error("Cannot infer SbomContext: graph has no entities")
        val app = annotations[SbomAnnotationKeys.APP]
            ?: error("Cannot infer SbomContext: entities missing '${SbomAnnotationKeys.APP}' annotation")
        val appVersion = annotations[SbomAnnotationKeys.APP_VERSION]
            ?: error("Cannot infer SbomContext: entities missing '${SbomAnnotationKeys.APP_VERSION}' annotation")
        return SbomContext(app, appVersion)
    }

    private fun objExprMatcher(extra: Map<String, String>): BoMObjExprMatcher? {
        if (extra.isEmpty()) return null
        val clauses = extra.entries.joinToString(" && ") { (k, v) -> "a.$k == '${escape(v)}'" }
        return BoMObjExprMatcher(clauses)
    }

    private fun escape(value: String): String = value.replace("'", "\\'")
}
