package org.poc.objs.core.domain

import java.util.UUID

/** Soft-link subgraph header (id + free-form annotations). */
data class BoMSubgraphHeader(
    val id: UUID,
    val annotations: Map<String, String>,
)

/** Create/replace membership spec for a soft-link subgraph. */
data class BoMSubgraphSpec(
    val id: UUID? = null,
    val annotations: Map<String, String> = emptyMap(),
    val entityIds: Set<UUID> = emptySet(),
    val edgeIds: Set<UUID> = emptySet(),
)

/** List row with membership counts. */
data class BoMSubgraphListItem(
    val id: UUID,
    val annotations: Map<String, String>,
    val entityCount: Long,
    val edgeCount: Long,
)

/** Header + resolved live [BoMSubgraph] (member ids unchanged). */
data class BoMResolvedSubgraph(
    val id: UUID,
    val annotations: Map<String, String>,
    val subgraph: BoMSubgraph,
)

/**
 * Soft-link subgraph membership validation / not-found failures.
 */
class BoMSubgraphException(
    val code: String,
    message: String,
) : RuntimeException(message)
