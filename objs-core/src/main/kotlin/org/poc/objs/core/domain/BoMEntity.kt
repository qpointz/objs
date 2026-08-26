package org.poc.objs.core.domain

import java.time.Instant
import java.util.UUID

/**
 * Domain entity (`BoMEntity`): typed JSON payload + caller annotations.
 *
 * [id] absent means create on persist; present means update (G-20).
 * [type] + [schemaVersion] select the JSON Schema from the central catalog.
 */
data class BoMEntity @JvmOverloads constructor(
    var id: UUID? = null,
    var type: String,
    var schemaVersion: String,
    var payload: MutableMap<String, Any?> = mutableMapOf(),
    var annotations: MutableMap<String, String> = mutableMapOf(),
    var createdAt: Instant? = null,
    var updatedAt: Instant? = null,
    /** Last deep-capture version for this entity; null until first Snapshot/capture → UI LATEST. */
    var headVersion: Long? = null,
)

/**
 * Directed edge (`BoMEdge`) with optional property schema.
 *
 * [source] / [target] reference entity ids. [type]/[schemaVersion]/[properties] used when
 * the allow-list properties policy is [BoMPropertiesPolicy.SCHEMA].
 */
data class BoMEdge @JvmOverloads constructor(
    var id: UUID? = null,
    var graphId: UUID? = null,
    var source: UUID,
    var target: UUID,
    var role: String,
    var type: String? = null,
    var schemaVersion: String? = null,
    var properties: MutableMap<String, Any?>? = null,
    var createdAt: Instant? = null,
    var updatedAt: Instant? = null,
    /** Last deep-capture version for this edge; null until first Snapshot/capture → UI LATEST. */
    var headVersion: Long? = null,
)

/** In-memory bag of entities and edges (SDK / write payload). */
data class BoMGraph(
    val entities: MutableList<BoMEntity> = mutableListOf(),
    val edges: MutableList<BoMEdge> = mutableListOf(),
) {
    fun entityById(id: UUID): BoMEntity? = entities.find { it.id == id }
}

/** Named-graph mutate semantic (pool mutate stays MERGE-only). */
enum class BoMMutateMode {
    /** Patch: set + unset; omission keeps. */
    MERGE,

    /** Overwrite membership + graph-local edges from set; unset not allowed. */
    REPLACE,
}

/**
 * Entity half of a [BoMGraphMutation]: [set] payloads and [unset] ids.
 */
data class BoMEntityMutation @JvmOverloads constructor(
    val set: MutableList<BoMEntity> = mutableListOf(),
    val unset: MutableList<UUID> = mutableListOf(),
)

/**
 * Edge half of a [BoMGraphMutation]: [set] payloads and [unset] ids.
 */
data class BoMEdgeMutation @JvmOverloads constructor(
    val set: MutableList<BoMEdge> = mutableListOf(),
    val unset: MutableList<UUID> = mutableListOf(),
)

/**
 * Transactional graph mutation — kind-first: [entities] / [edges], each with set/unset.
 *
 * Distinct from [BoMGraph] so MERGE callers can omit unchanged members (omission never deletes).
 * Empty [unset] = set-only. Prefer [bomMutation] for construction.
 *
 * JSON shape:
 * ```
 * { "entities": { "set": [], "unset": [] }, "edges": { "set": [], "unset": [] } }
 * ```
 */
data class BoMGraphMutation @JvmOverloads constructor(
    val entities: BoMEntityMutation = BoMEntityMutation(),
    val edges: BoMEdgeMutation = BoMEdgeMutation(),
    val mode: BoMMutateMode = BoMMutateMode.MERGE,
) {
    fun graph(): BoMGraph = BoMGraph(entities.set, edges.set)

    fun hasUnsets(): Boolean = entities.unset.isNotEmpty() || edges.unset.isNotEmpty()

    fun hasSets(): Boolean = entities.set.isNotEmpty() || edges.set.isNotEmpty()

    companion object {
        /** Set-only mutation from a bag (seeds / REPLACE helpers). */
        fun of(graph: BoMGraph, mode: BoMMutateMode = BoMMutateMode.MERGE): BoMGraphMutation =
            BoMGraphMutation(
                entities = BoMEntityMutation(set = graph.entities),
                edges = BoMEdgeMutation(set = graph.edges),
                mode = mode,
            )
    }
}

/** Result of annotation-based subgraph selection (induced edges). */
data class BoMGraphContents(
    val entities: List<BoMEntity>,
    val edges: List<BoMEdge>,
)
