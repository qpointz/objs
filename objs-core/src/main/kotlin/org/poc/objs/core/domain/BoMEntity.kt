package org.poc.objs.core.domain

import java.util.UUID

/**
 * Domain entity (`BoMEntity`): typed JSON payload + caller annotations.
 *
 * [id] absent means create on persist; present means update (G-20).
 * [type] + [schemaVersion] select the JSON Schema from the central catalog.
 */
data class BoMEntity(
    var id: UUID? = null,
    var type: String,
    var schemaVersion: String,
    var payload: MutableMap<String, Any?> = mutableMapOf(),
    var annotations: MutableMap<String, String> = mutableMapOf(),
)

/**
 * Directed edge (`BoMEdge`) with optional property schema.
 *
 * [source] / [target] reference entity ids. [type]/[schemaVersion]/[properties] used when
 * the allow-list properties policy is [BoMPropertiesPolicy.SCHEMA].
 */
data class BoMEdge(
    var id: UUID? = null,
    var source: UUID,
    var target: UUID,
    var role: String,
    var type: String? = null,
    var schemaVersion: String? = null,
    var properties: MutableMap<String, Any?>? = null,
)

/** In-memory bag of entities and edges (SDK / write payload). */
data class BoMGraph(
    val entities: MutableList<BoMEntity> = mutableListOf(),
    val edges: MutableList<BoMEdge> = mutableListOf(),
) {
    fun entityById(id: UUID): BoMEntity? = entities.find { it.id == id }
}

/**
 * Upsert half of a [BoMGraphMutation].
 */
data class BoMGraphUpsert(
    val entities: MutableList<BoMEntity> = mutableListOf(),
    val edges: MutableList<BoMEdge> = mutableListOf(),
)

/**
 * Delete half of a [BoMGraphMutation] — entity/edge **ids** only.
 */
data class BoMGraphDelete(
    val entities: MutableList<UUID> = mutableListOf(),
    val edges: MutableList<UUID> = mutableListOf(),
)

/**
 * Transactional graph mutation: [upsert] entities/edges and/or [delete] by id.
 *
 * Distinct from [BoMGraph] so seeds and upsert-only callers stay MERGE-shaped
 * (omission never deletes). Empty [delete] lists = upsert-only.
 *
 * JSON shape:
 * ```
 * { "upsert": { "entities": [], "edges": [] }, "delete": { "entities": [], "edges": [] } }
 * ```
 */
data class BoMGraphMutation(
    val upsert: BoMGraphUpsert = BoMGraphUpsert(),
    val delete: BoMGraphDelete = BoMGraphDelete(),
) {
    fun graph(): BoMGraph = BoMGraph(upsert.entities, upsert.edges)

    fun hasDeletes(): Boolean = delete.entities.isNotEmpty() || delete.edges.isNotEmpty()

    fun hasUpserts(): Boolean = upsert.entities.isNotEmpty() || upsert.edges.isNotEmpty()
}

/** Result of annotation-based subgraph selection (induced edges). */
data class BoMSubgraph(
    val entities: List<BoMEntity>,
    val edges: List<BoMEdge>,
)
