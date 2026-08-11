package org.poc.objs.gremlin.core.materialize

import org.apache.tinkerpop.gremlin.structure.Graph
import org.poc.objs.core.domain.BoMGraphContents

/**
 * Resolves a [BoMGremlinMaterializationStrategy] by name and materializes a subgraph.
 * Default strategy is [EnvelopeMaterializationStrategy.NAME].
 */
class BoMGremlinMaterializer(
    strategies: List<BoMGremlinMaterializationStrategy> = listOf(EnvelopeMaterializationStrategy()),
) {
    private val byName: Map<String, BoMGremlinMaterializationStrategy> =
        strategies.associateBy { it.name }

    fun materialize(
        subgraph: BoMGraphContents,
        strategy: String = EnvelopeMaterializationStrategy.NAME,
    ): Graph {
        val impl = byName[strategy]
            ?: throw IllegalArgumentException(
                "Unknown materialization strategy '$strategy'; known=${byName.keys.sorted()}",
            )
        return impl.materialize(subgraph)
    }
}
