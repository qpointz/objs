package org.poc.objs.core.versioning

import java.util.UUID

/** Kind of HEAD row being persisted. */
enum class VersionedKind {
    ENTITY,
    EDGE,
    GRAPH,
}

/** Persist operation that may trigger a capture. */
enum class VersioningOp {
    CREATE,
    UPDATE,
    DELETE,
}

/**
 * Asked by the store after a HEAD write. C-18 default ([ExplicitOnlyVersioningStrategy]) never
 * captures; deep graph freeze captures regardless (WI-004).
 */
fun interface BomVersioningStrategy {
    fun shouldCapture(ctx: VersioningContext): Boolean
}

data class VersioningContext(
    val graphId: UUID?,
    val kind: VersionedKind,
    val op: VersioningOp,
    val parentId: UUID,
    val headVersion: Long?,
)

/** C-18 default: ordinary persist never writes `*_version`. */
class ExplicitOnlyVersioningStrategy : BomVersioningStrategy {
    override fun shouldCapture(ctx: VersioningContext): Boolean = false
}
