package org.poc.objs.core.domain

import java.util.UUID

/**
 * Domain entity (`BoEntity`): typed JSON payload + caller annotations.
 *
 * [id] absent means create on persist; present means update (G-20).
 * [type] + [version] select the JSON Schema from the central catalog.
 */
data class BoEntity(
    var id: UUID? = null,
    var type: String,
    var version: String,
    var payload: MutableMap<String, Any?> = mutableMapOf(),
    var annotations: MutableMap<String, String> = mutableMapOf(),
)

/**
 * Directed edge (`BoEdge`) with optional property schema.
 *
 * [source] / [target] reference entity ids. [type]/[version]/[properties] used when
 * the allow-list properties policy is [BoPropertiesPolicy.SCHEMA].
 */
data class BoEdge(
    var id: UUID? = null,
    var source: UUID,
    var target: UUID,
    var role: String,
    var type: String? = null,
    var version: String? = null,
    var properties: MutableMap<String, Any?>? = null,
)

/** In-memory bag of entities and edges (SDK / write payload). */
data class BoGraph(
    val entities: MutableList<BoEntity> = mutableListOf(),
    val edges: MutableList<BoEdge> = mutableListOf(),
) {
    fun entityById(id: UUID): BoEntity? = entities.find { it.id == id }
}

/** Result of annotation-based subgraph selection (induced edges). */
data class BoSubgraph(
    val entities: List<BoEntity>,
    val edges: List<BoEdge>,
)
