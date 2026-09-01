package org.poc.objs.gremlin.core.materialize

import org.apache.tinkerpop.gremlin.structure.Graph
import org.poc.objs.api.domain.ResolvedGraphFragment

/**
 * Resolves a [GremlinMaterializationStrategy] by name and materializes a subgraph.
 * Default strategy is [EnvelopeMaterializationStrategy.NAME].
 */
class GremlinMaterializer(
    strategies: List<GremlinMaterializationStrategy> = listOf(EnvelopeMaterializationStrategy()),
) {
    private val byName: Map<String, GremlinMaterializationStrategy> =
        strategies.associateBy { it.name }

    fun materialize(
        fragment: ResolvedGraphFragment,
        strategy: String = EnvelopeMaterializationStrategy.NAME,
    ): Graph {
        val impl = byName[strategy]
            ?: throw IllegalArgumentException(
                "Unknown materialization strategy '$strategy'; known=${byName.keys.sorted()}",
            )
        return impl.materialize(fragment)
    }
}
