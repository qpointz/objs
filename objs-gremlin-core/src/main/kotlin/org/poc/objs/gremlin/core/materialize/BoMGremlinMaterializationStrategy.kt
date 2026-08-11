package org.poc.objs.gremlin.core.materialize

import org.apache.tinkerpop.gremlin.structure.Graph
import org.poc.objs.core.domain.BoMGraphContents

/**
 * Pluggable mapping from a BoM subgraph to an in-memory TinkerPop [Graph].
 *
 * v1 implements [EnvelopeMaterializationStrategy] only. Future strategies
 * (documented, not implemented): `flatten`, `nested-vertices`.
 */
interface BoMGremlinMaterializationStrategy {
    val name: String

    fun materialize(subgraph: BoMGraphContents): Graph
}
