package org.poc.objs.api.store

import java.util.UUID

/**
 * App-level guard so purge/destroy/delete fail closed when external rows reference a graph
 * or graph versions (e.g. SBOM BOM rows / fingerprints). Default: allow all.
 */
fun interface GraphVersionReferenceGuard {
    /**
     * @param versions empty means “any/all versions of [graphId]” (destroy / purge-all / soft delete)
     * @throws org.poc.objs.api.GraphOperationException with code `GRAPH_VERSION_IN_USE` or `GRAPH_IN_USE` when blocked
     */
    fun assertRemovable(graphId: UUID, versions: Collection<Long>)
}

/** No-op guard for foundation tests and apps without version references. */
object AllowAllGraphVersionReferenceGuard : GraphVersionReferenceGuard {
    override fun assertRemovable(graphId: UUID, versions: Collection<Long>) = Unit
}
