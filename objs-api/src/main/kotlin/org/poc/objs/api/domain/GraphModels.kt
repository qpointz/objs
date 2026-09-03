package org.poc.objs.api.domain

import org.poc.objs.api.domain.*

import java.time.Instant
import java.util.UUID

/** Soft-link graph header (id + free-form annotations). */
data class GraphHeader @JvmOverloads constructor(
    val id: UUID,
    val annotations: Map<String, String>,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

/**
 * Live (HEAD) graphs containing an entity. [total] is the unfiltered live membership count;
 * [items] may be filtered (`q`) and/or limited.
 */
data class EntityLiveGraphs(
    val items: List<GraphHeader>,
    val total: Int,
)

/** Create/replace membership spec for a soft-link subgraph. */
data class GraphSpec(
    val id: UUID? = null,
    val annotations: Map<String, String> = emptyMap(),
    val entityIds: Set<UUID> = emptySet(),
    val edgeIds: Set<UUID> = emptySet(),
)

/** List row with membership counts. */
data class GraphListItem @JvmOverloads constructor(
    val id: UUID,
    val annotations: Map<String, String>,
    val entityCount: Long,
    val edgeCount: Long,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

/** One deep graph version (Explorer list). Newest-first from [org.poc.objs.core.persistence.NamedGraphStore.listGraphVersions]. */
data class GraphVersionSummary(
    val graphId: UUID,
    val version: Long,
    val createdAt: Instant,
    val annotations: Map<String, String>,
)

/** Entity or edge history row (newest-first list). */
data class InstanceVersionSummary(
    val id: UUID,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val annotations: Map<String, String> = emptyMap(),
)

/** Total + newest-first recent versions for object-viewer Versions section. */
data class InstanceVersionStats(
    val total: Long,
    val recent: List<InstanceVersionSummary>,
)

/** Header + resolved live [GraphContents] (member ids unchanged). */
data class ResolvedGraph @JvmOverloads constructor(
    val id: UUID,
    val annotations: Map<String, String>,
    val contents: GraphContents,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

/**
 * Soft-link subgraph membership validation / not-found failures.
 */
class GraphException(
    val code: String,
    message: String,
) : RuntimeException(message)
