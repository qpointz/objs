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

/** Result of annotation-based subgraph selection (induced edges). */
data class BoMSubgraph(
    val entities: List<BoMEntity>,
    val edges: List<BoMEdge>,
)
