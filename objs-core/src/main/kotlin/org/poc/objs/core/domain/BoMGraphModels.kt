package org.poc.objs.core.domain

import java.util.UUID

/** Soft-link graph header (id + free-form annotations). */
data class BoMGraphHeader(
    val id: UUID,
    val annotations: Map<String, String>,
)

/** Create/replace membership spec for a soft-link subgraph. */
data class BoMGraphSpec(
    val id: UUID? = null,
    val annotations: Map<String, String> = emptyMap(),
    val entityIds: Set<UUID> = emptySet(),
    val edgeIds: Set<UUID> = emptySet(),
)

/** List row with membership counts. */
data class BoMGraphListItem(
    val id: UUID,
    val annotations: Map<String, String>,
    val entityCount: Long,
    val edgeCount: Long,
)

/** Header + resolved live [BoMGraphContents] (member ids unchanged). */
data class BoMResolvedGraph(
    val id: UUID,
    val annotations: Map<String, String>,
    val contents: BoMGraphContents,
)

/**
 * Soft-link subgraph membership validation / not-found failures.
 */
class BoMGraphException(
    val code: String,
    message: String,
) : RuntimeException(message)
