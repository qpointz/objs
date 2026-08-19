package org.poc.objs.core.domain

import java.time.Instant
import java.util.UUID

/** Soft-link graph header (id + free-form annotations). */
data class BoMGraphHeader @JvmOverloads constructor(
    val id: UUID,
    val annotations: Map<String, String>,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

/** Create/replace membership spec for a soft-link subgraph. */
data class BoMGraphSpec(
    val id: UUID? = null,
    val annotations: Map<String, String> = emptyMap(),
    val entityIds: Set<UUID> = emptySet(),
    val edgeIds: Set<UUID> = emptySet(),
)

/** List row with membership counts. */
data class BoMGraphListItem @JvmOverloads constructor(
    val id: UUID,
    val annotations: Map<String, String>,
    val entityCount: Long,
    val edgeCount: Long,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

/** One deep graph version (Explorer list). Newest-first from [org.poc.objs.core.persistence.BoMNamedGraphStore.listGraphVersions]. */
data class BoMGraphVersionSummary(
    val graphId: UUID,
    val version: Long,
    val createdAt: Instant,
    val annotations: Map<String, String>,
)

/** Header + resolved live [BoMGraphContents] (member ids unchanged). */
data class BoMResolvedGraph @JvmOverloads constructor(
    val id: UUID,
    val annotations: Map<String, String>,
    val contents: BoMGraphContents,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

/**
 * Soft-link subgraph membership validation / not-found failures.
 */
class BoMGraphException(
    val code: String,
    message: String,
) : RuntimeException(message)
