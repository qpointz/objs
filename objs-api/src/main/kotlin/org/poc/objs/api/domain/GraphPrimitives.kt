package org.poc.objs.api.domain

import java.time.Instant
import java.util.UUID

/**
 * Domain entity: typed JSON payload plus caller annotations.
 *
 * [id] absent means create on persist; present means update.
 * [type] plus [schemaVersion] identify the payload schema.
 */
data class Entity @JvmOverloads constructor(
    var id: UUID? = null,
    var type: String,
    var schemaVersion: String,
    var payload: MutableMap<String, Any?> = mutableMapOf(),
    var annotations: MutableMap<String, String> = mutableMapOf(),
    var createdAt: Instant? = null,
    var updatedAt: Instant? = null,
    var headVersion: Long? = null,
)

/**
 * Directed edge with optional property payload.
 *
 * [source] and [target] reference entity ids. [type], [schemaVersion], and [properties] are
 * supplied when the relation's property policy permits them.
 */
data class Edge @JvmOverloads constructor(
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
    var headVersion: Long? = null,
)

/** In-memory bag of entities and edges used by the SDK and write API. */
data class Graph(
    val entities: MutableList<Entity> = mutableListOf(),
    val edges: MutableList<Edge> = mutableListOf(),
) {
    fun entityById(id: UUID): Entity? = entities.find { it.id == id }
}

/** Graph mutation mode. */
enum class MutationMode {
    /** Patch: set plus unset; omission keeps existing values. */
    MERGE,

    /** Overwrite membership and graph-local edges from set; unset is not allowed. */
    REPLACE,
}

/** Entity half of a [GraphMutation]. */
data class EntityMutation @JvmOverloads constructor(
    val set: MutableList<Entity> = mutableListOf(),
    val unset: MutableList<UUID> = mutableListOf(),
)

/** Edge half of a [GraphMutation]. */
data class EdgeMutation @JvmOverloads constructor(
    val set: MutableList<Edge> = mutableListOf(),
    val unset: MutableList<UUID> = mutableListOf(),
)

/**
 * Transactional graph mutation: entities and edges, each with set and unset operations.
 *
 * JSON shape:
 * ```
 * { "entities": { "set": [], "unset": [] }, "edges": { "set": [], "unset": [] } }
 * ```
 */
data class GraphMutation @JvmOverloads constructor(
    val entities: EntityMutation = EntityMutation(),
    val edges: EdgeMutation = EdgeMutation(),
    val mode: MutationMode = MutationMode.MERGE,
) {
    fun graph(): Graph = Graph(entities.set, edges.set)

    fun hasUnsets(): Boolean = entities.unset.isNotEmpty() || edges.unset.isNotEmpty()

    fun hasSets(): Boolean = entities.set.isNotEmpty() || edges.set.isNotEmpty()

    companion object {
        /** Set-only mutation from a bag, useful for seeds and REPLACE helpers. */
        fun of(graph: Graph, mode: MutationMode = MutationMode.MERGE): GraphMutation =
            GraphMutation(
                entities = EntityMutation(set = graph.entities),
                edges = EdgeMutation(set = graph.edges),
                mode = mode,
            )
    }
}

/** Result of annotation-based subgraph selection. */
data class GraphContents(
    val entities: List<Entity>,
    val edges: List<Edge>,
)
