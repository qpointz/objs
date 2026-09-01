package org.poc.objs.api.domain

import java.util.UUID

/** Structural input boundary for graph normalization and native materialization. */
interface GraphFragment {
    val entities: List<Entity>
    val edges: List<Edge>
}

/** Severity of a graph-fragment resolution diagnostic. */
enum class GraphFragmentDiagnosticSeverity {
    INFO,
    WARNING,
    ERROR,
}

/**
 * Structured information emitted while resolving a graph fragment.
 *
 * Diagnostic entry ordering is intentionally unspecified.
 */
data class GraphFragmentDiagnostic(
    val severity: GraphFragmentDiagnosticSeverity,
    val message: String,
    val nodes: List<UUID> = emptyList(),
    val edges: List<UUID> = emptyList(),
)

/** Normalized graph input and diagnostics produced by a [GraphFragmentPolicy]. */
data class ResolvedGraphFragment(
    override val entities: List<Entity>,
    override val edges: List<Edge>,
    val diagnostics: List<GraphFragmentDiagnostic> = emptyList(),
) : GraphFragment {
    fun hasErrors(): Boolean = diagnostics.any { it.severity == GraphFragmentDiagnosticSeverity.ERROR }

    fun asGraphContents(): GraphContents = GraphContents(entities = entities, edges = edges)
}

/** Single extension point for identity, duplicate, conflict, and dangling-endpoint resolution. */
fun interface GraphFragmentPolicy {
    fun resolve(fragment: GraphFragment): ResolvedGraphFragment
}

/**
 * Default UUID-based fragment policy.
 *
 * Records with an ID are compared by semantic graph content. Equivalent records are deduplicated;
 * conflicting records produce an error and the lexicographically canonical candidate is retained
 * so the returned candidate set is deterministic. Records without IDs remain candidates and are
 * not deduplicated.
 */
object DefaultGraphFragmentPolicy : GraphFragmentPolicy {
    override fun resolve(fragment: GraphFragment): ResolvedGraphFragment {
        val diagnostics = mutableListOf<GraphFragmentDiagnostic>()
        val entitiesById = linkedMapOf<UUID, Entity>()
        val entitiesWithoutId = mutableListOf<Entity>()

        fragment.entities.forEach { entity ->
            val id = entity.id
            if (id == null) {
                entitiesWithoutId += entity
                return@forEach
            }

            val existing = entitiesById[id]
            if (existing == null) {
                entitiesById[id] = entity
            } else if (sameEntity(existing, entity)) {
                entitiesById[id] = canonicalEntity(existing, entity)
            } else {
                diagnostics += GraphFragmentDiagnostic(
                    severity = GraphFragmentDiagnosticSeverity.ERROR,
                    message = "Conflicting entities share UUID $id",
                    nodes = listOf(id),
                )
                entitiesById[id] = listOf(existing, entity).minBy(::entityKey)
            }
        }

        val edgesById = linkedMapOf<UUID, Edge>()
        val edgesWithoutId = mutableListOf<Edge>()

        fragment.edges.forEach { edge ->
            val id = edge.id
            if (id == null) {
                edgesWithoutId += edge
                return@forEach
            }

            val existing = edgesById[id]
            if (existing == null) {
                edgesById[id] = edge
            } else if (sameEdge(existing, edge)) {
                edgesById[id] = canonicalEdge(existing, edge)
            } else {
                diagnostics += GraphFragmentDiagnostic(
                    severity = GraphFragmentDiagnosticSeverity.ERROR,
                    message = "Conflicting edges share UUID $id",
                    edges = listOf(id),
                    nodes = listOf(edge.source, edge.target).distinct(),
                )
                edgesById[id] = listOf(existing, edge).minBy(::edgeKey)
            }
        }

        val resolvedEntities = (entitiesById.values + entitiesWithoutId).sortedWith(entityComparator)
        val resolvedEdges = (edgesById.values + edgesWithoutId).sortedWith(edgeComparator)
        val knownEntityIds = resolvedEntities.mapNotNullTo(hashSetOf()) { it.id }

        resolvedEdges.forEach { edge ->
            val missing = buildList {
                if (edge.source !in knownEntityIds) add(edge.source)
                if (edge.target !in knownEntityIds && edge.target != edge.source) add(edge.target)
            }
            if (missing.isNotEmpty()) {
                diagnostics += GraphFragmentDiagnostic(
                    severity = GraphFragmentDiagnosticSeverity.ERROR,
                    message = "Edge ${edge.id ?: "<without UUID>"} has dangling endpoint(s)",
                    nodes = missing,
                    edges = listOfNotNull(edge.id),
                )
            }
        }

        return ResolvedGraphFragment(resolvedEntities, resolvedEdges, diagnostics)
    }

    private fun sameEntity(left: Entity, right: Entity): Boolean =
        left.id == right.id &&
            left.type == right.type &&
            left.schemaVersion == right.schemaVersion &&
            canonicalValue(left.payload) == canonicalValue(right.payload) &&
            canonicalValue(left.annotations) == canonicalValue(right.annotations)

    private fun sameEdge(left: Edge, right: Edge): Boolean =
        left.id == right.id &&
            left.source == right.source &&
            left.target == right.target &&
            left.role == right.role &&
            left.type == right.type &&
            left.schemaVersion == right.schemaVersion &&
            canonicalValue(left.properties) == canonicalValue(right.properties)

    private fun canonicalEntity(left: Entity, right: Entity): Entity =
        if (left.graphKey() <= right.graphKey()) left else right

    private fun canonicalEdge(left: Edge, right: Edge): Edge =
        if (left.graphKey() == right.graphKey()) {
            left
        } else {
            left.copy(graphId = null)
        }

    private fun entityKey(entity: Entity): String = entity.graphKey()

    private fun edgeKey(edge: Edge): String = edge.graphKey()

    private fun Entity.graphKey(): String =
        listOf(
            id?.toString().orEmpty(),
            type,
            schemaVersion,
            canonicalValue(payload),
            canonicalValue(annotations),
        ).joinToString("\u0000")

    private fun Edge.graphKey(): String =
        listOf(
            id?.toString().orEmpty(),
            source.toString(),
            target.toString(),
            role,
            type.orEmpty(),
            schemaVersion.orEmpty(),
            canonicalValue(properties),
            graphId?.toString().orEmpty(),
        ).joinToString("\u0000")

    private fun canonicalValue(value: Any?): String =
        when (value) {
            null -> "null"
            is Map<*, *> -> value.entries
                .sortedBy { it.key?.toString().orEmpty() }
                .joinToString(prefix = "{", postfix = "}") {
                    "${it.key?.toString().orEmpty()}:${canonicalValue(it.value)}"
                }
            is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]", transform = ::canonicalValue)
            is Array<*> -> value.joinToString(prefix = "[", postfix = "]", transform = ::canonicalValue)
            else -> value.toString()
        }

    private val entityComparator = compareBy<Entity>(
        { it.id == null },
        { it.id?.toString().orEmpty() },
        ::entityKey,
    )

    private val edgeComparator = compareBy<Edge>(
        { it.id == null },
        { it.id?.toString().orEmpty() },
        ::edgeKey,
    )
}
