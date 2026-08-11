package org.poc.objs.gremlin.core

import org.apache.tinkerpop.gremlin.process.traversal.Path
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Property
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.gremlin.core.materialize.EnvelopeMaterializationStrategy
import java.util.UUID

/**
 * Projects raw Gremlin results into [BoMGremlinItem]s and optional subgraph2.
 */
object BoMGremlinResultProjector {

    fun projectItems(raw: List<Any?>): List<BoMGremlinItem> =
        raw.map { projectValue(it) }

    fun projectValue(value: Any?): BoMGremlinItem =
        when (value) {
            null -> BoMGremlinItem.Scalar(null)
            is Vertex -> BoMGremlinItem.Vertex(vertexToMap(value))
            is Edge -> BoMGremlinItem.Edge(edgeToMap(value))
            is Path -> BoMGremlinItem.Path(pathToValue(value))
            is Map<*, *> -> BoMGremlinItem.MapValue(normalizeMap(value))
            is Iterable<*> -> {
                if (value is Collection<*> && value.size == 1) {
                    projectValue(value.first())
                } else {
                    BoMGremlinItem.ListValue(value.map { projectValue(it) })
                }
            }
            is Array<*> -> BoMGremlinItem.ListValue(value.map { projectValue(it) })
            is Number, is String, is Boolean -> BoMGremlinItem.Scalar(value)
            else -> BoMGremlinItem.Scalar(value.toString())
        }

    fun buildSubgraph2(items: List<BoMGremlinItem>, subgraph1: BoMGraphContents): BoMGraphContents? {
        val entitiesById = LinkedHashMap<UUID, BoMEntity>()
        val edgesById = LinkedHashMap<UUID, BoMEdge>()
        var sawEdgeElement = false

        fun absorb(item: BoMGremlinItem) {
            when (item) {
                is BoMGremlinItem.Vertex -> {
                    val entity = mapToEntity(item.value) ?: return
                    val id = entity.id ?: return
                    entitiesById.putIfAbsent(id, entity)
                }
                is BoMGremlinItem.Edge -> {
                    sawEdgeElement = true
                    val edge = mapToEdge(item.value) ?: return
                    val id = edge.id ?: return
                    edgesById.putIfAbsent(id, edge)
                }
                is BoMGremlinItem.Path -> item.value.objects.forEach(::absorb)
                is BoMGremlinItem.ListValue -> item.value.forEach(::absorb)
                is BoMGremlinItem.MapValue, is BoMGremlinItem.Scalar -> Unit
            }
        }

        items.forEach(::absorb)
        if (entitiesById.isEmpty() && edgesById.isEmpty()) {
            return null
        }

        if (!sawEdgeElement && entitiesById.isNotEmpty()) {
            val ids = entitiesById.keys
            for (edge in subgraph1.edges) {
                if (edge.source in ids && edge.target in ids) {
                    val id = edge.id ?: continue
                    edgesById.putIfAbsent(id, edge)
                }
            }
        }

        // Drop dangling edges
        val entityIds = entitiesById.keys
        val edges = edgesById.values.filter { it.source in entityIds && it.target in entityIds }

        // Ensure endpoints from edge-only results are present when possible from subgraph1
        if (entitiesById.isEmpty() && edges.isNotEmpty()) {
            val needed = edges.flatMap { listOf(it.source, it.target) }.toSet()
            for (entity in subgraph1.entities) {
                val id = entity.id ?: continue
                if (id in needed) {
                    entitiesById[id] = entity
                }
            }
        }

        if (entitiesById.isEmpty()) {
            return null
        }

        return BoMGraphContents(
            entities = entitiesById.values.toList(),
            edges = edges,
        )
    }

    fun inferPrimary(items: List<BoMGremlinItem>, subgraph: BoMGraphContents?): String {
        if (subgraph != null) return "graph"
        if (items.isEmpty()) return "list"
        if (items.size == 1) {
            return when (items[0]) {
                is BoMGremlinItem.Scalar -> "scalar"
                is BoMGremlinItem.MapValue -> "table"
                is BoMGremlinItem.ListValue -> "list"
                else -> "mixed"
            }
        }
        val kinds = items.map { it.kind }.toSet()
        return when {
            kinds == setOf("map") -> "table"
            kinds == setOf("scalar") -> "list"
            else -> "mixed"
        }
    }

    fun buildTable(items: List<BoMGremlinItem>): BoMGremlinTable? {
        val maps = items.mapNotNull { (it as? BoMGremlinItem.MapValue)?.value }
        if (maps.isEmpty() || maps.size != items.size) return null
        val columns = maps.flatMap { it.keys }.distinct()
        val rows = maps.map { row -> columns.map { col -> row[col] } }
        return BoMGremlinTable(columns = columns, rows = rows)
    }

    fun vertexToMap(vertex: Vertex): Map<String, Any?> {
        val props = propertyMap(vertex.properties<Any?>())
        val id = vertex.id() as UUID
        return linkedMapOf(
            "id" to id,
            "type" to vertex.label(),
            "schemaVersion" to (props[EnvelopeMaterializationStrategy.PROP_SCHEMA_VERSION] as? String ?: ""),
            "payload" to ((props[EnvelopeMaterializationStrategy.PROP_PAYLOAD] as? Map<*, *>)?.let { normalizeMap(it) }
                ?: emptyMap()),
            "annotations" to ((props[EnvelopeMaterializationStrategy.PROP_ANNOTATIONS] as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { it.value?.toString() ?: "" }
                ?: emptyMap()),
        )
    }

    fun edgeToMap(edge: Edge): Map<String, Any?> {
        val props = propertyMap(edge.properties<Any?>())
        return linkedMapOf(
            "id" to (edge.id() as UUID),
            "source" to (edge.outVertex().id() as UUID),
            "target" to (edge.inVertex().id() as UUID),
            "role" to edge.label(),
            "type" to props[EnvelopeMaterializationStrategy.PROP_TYPE],
            "schemaVersion" to props[EnvelopeMaterializationStrategy.PROP_SCHEMA_VERSION],
            "properties" to ((props[EnvelopeMaterializationStrategy.PROP_PROPERTIES] as? Map<*, *>)
                ?.let { normalizeMap(it) }),
        )
    }

    private fun pathToValue(path: Path): BoMGremlinPathValue {
        val objects = path.objects().map { projectValue(it) }
        val labels = path.labels().map { it.toSet() }
        return BoMGremlinPathValue(labels = labels, objects = objects)
    }

    private fun propertyMap(properties: Iterator<out Property<Any?>>): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        while (properties.hasNext()) {
            val p = properties.next()
            if (p is VertexProperty<*> && p.key() != null) {
                map[p.key()] = p.value()
            } else if (p.isPresent && p.key() != null) {
                map[p.key()] = p.value()
            }
        }
        return map
    }

    @Suppress("UNCHECKED_CAST")
    private fun normalizeMap(map: Map<*, *>): Map<String, Any?> =
        map.entries.associate { (k, v) ->
            k.toString() to when (v) {
                is Map<*, *> -> normalizeMap(v)
                is Iterable<*> -> v.map { if (it is Map<*, *>) normalizeMap(it) else it }
                else -> v
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun mapToEntity(value: Map<String, Any?>): BoMEntity? {
        val id = value["id"] as? UUID ?: return null
        val type = value["type"] as? String ?: return null
        val schemaVersion = value["schemaVersion"] as? String ?: ""
        val payload = (value["payload"] as? Map<String, Any?>)?.toMutableMap() ?: mutableMapOf()
        val annotations = (value["annotations"] as? Map<*, *>)
            ?.mapKeys { it.key.toString() }
            ?.mapValues { it.value?.toString() ?: "" }
            ?.toMutableMap()
            ?: mutableMapOf()
        return BoMEntity(
            id = id,
            type = type,
            schemaVersion = schemaVersion,
            payload = payload,
            annotations = annotations,
        )
    }

    private fun mapToEdge(value: Map<String, Any?>): BoMEdge? {
        val id = value["id"] as? UUID ?: return null
        val source = value["source"] as? UUID ?: return null
        val target = value["target"] as? UUID ?: return null
        val role = value["role"] as? String ?: return null
        @Suppress("UNCHECKED_CAST")
        val properties = value["properties"] as? MutableMap<String, Any?>
            ?: (value["properties"] as? Map<String, Any?>)?.toMutableMap()
        return BoMEdge(
            id = id,
            source = source,
            target = target,
            role = role,
            type = value["type"] as? String,
            schemaVersion = value["schemaVersion"] as? String,
            properties = properties,
        )
    }
}
