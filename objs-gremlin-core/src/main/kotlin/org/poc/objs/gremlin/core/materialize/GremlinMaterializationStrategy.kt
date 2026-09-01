package org.poc.objs.gremlin.core.materialize

import org.apache.tinkerpop.gremlin.structure.Graph
import org.poc.objs.api.domain.ResolvedGraphFragment

/**
 * Pluggable mapping from a resolved graph fragment to an in-memory TinkerPop [Graph].
 *
 * v1 implements [EnvelopeMaterializationStrategy] only. Future strategies
 * (documented, not implemented): `flatten`, `nested-vertices`.
 */
interface GremlinMaterializationStrategy {
    val name: String

    fun materialize(fragment: ResolvedGraphFragment): Graph
}
