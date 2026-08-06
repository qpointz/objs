package org.poc.objs.gremlin.core.materialize

import org.apache.commons.configuration2.BaseConfiguration
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.AbstractTinkerGraph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.poc.objs.core.domain.BoMSubgraph
import java.util.UUID

/**
 * One vertex per entity, one edge per BoM edge. Hierarchical `payload` /
 * `annotations` / edge `properties` stay as nested map property values
 * (not exploded into child vertices).
 */
class EnvelopeMaterializationStrategy : BoMGremlinMaterializationStrategy {
    override val name: String = NAME

    override fun materialize(subgraph: BoMSubgraph): Graph {
        val graph = TinkerGraph.open(uuidConfig())
        val vertices = LinkedHashMap<UUID, Vertex>()

        for (entity in subgraph.entities) {
            val id = entity.id
                ?: throw IllegalArgumentException("Entity type=${entity.type} has null id; cannot materialize")
            val vertex = graph.addVertex(
                T.id, id,
                T.label, entity.type,
                PROP_SCHEMA_VERSION, entity.schemaVersion,
                PROP_PAYLOAD, deepCopyMap(entity.payload),
                PROP_ANNOTATIONS, LinkedHashMap(entity.annotations),
            )
            vertices[id] = vertex
        }

        for (edge in subgraph.edges) {
            val id = edge.id
                ?: throw IllegalArgumentException("Edge role=${edge.role} has null id; cannot materialize")
            val out = vertices[edge.source]
                ?: throw IllegalArgumentException("Edge $id source ${edge.source} not in subgraph entities")
            val `in` = vertices[edge.target]
                ?: throw IllegalArgumentException("Edge $id target ${edge.target} not in subgraph entities")

            val props = mutableListOf<Any?>(T.id, id)
            edge.type?.let {
                props += PROP_TYPE
                props += it
            }
            edge.schemaVersion?.let {
                props += PROP_SCHEMA_VERSION
                props += it
            }
            edge.properties?.let {
                props += PROP_PROPERTIES
                props += deepCopyMap(it)
            }
            out.addEdge(edge.role, `in`, *props.toTypedArray())
        }

        return graph
    }

    companion object {
        const val NAME = "envelope"
        const val PROP_SCHEMA_VERSION = "schemaVersion"
        const val PROP_PAYLOAD = "payload"
        const val PROP_ANNOTATIONS = "annotations"
        const val PROP_TYPE = "type"
        const val PROP_PROPERTIES = "properties"

        private fun uuidConfig() = BaseConfiguration().apply {
            setProperty(AbstractTinkerGraph.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER, "UUID")
            setProperty(AbstractTinkerGraph.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, "UUID")
        }

        @Suppress("UNCHECKED_CAST")
        private fun deepCopyMap(source: Map<String, Any?>): MutableMap<String, Any?> {
            val copy = LinkedHashMap<String, Any?>()
            for ((k, v) in source) {
                copy[k] = when (v) {
                    is Map<*, *> -> deepCopyMap(v as Map<String, Any?>)
                    is List<*> -> v.map { item ->
                        if (item is Map<*, *>) deepCopyMap(item as Map<String, Any?>) else item
                    }.toMutableList()
                    else -> v
                }
            }
            return copy
        }
    }
}
