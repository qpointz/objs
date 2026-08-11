package org.poc.objs.core.seed

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMGraphUpsert
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.springframework.stereotype.Component
import java.util.UUID

data class SeedGraphPayload(
    val name: String,
    val graphId: UUID,
    val annotations: Map<String, String>,
    val graph: BoMGraph,
    val entityKeys: Map<String, UUID>,
    val edgeKeys: Map<String, UUID>,
)

/**
 * Seed handler for `kind: Graph`.
 *
 * Each Graph document becomes one `bom_graph` (header + membership + graph-local edges). Optional
 * document fields: `id` (graph UUID), `annotations` (header annotations). When `id` is omitted,
 * a stable UUIDv3 is derived from `graph-seed:<name>`.
 */
@Component
class GraphSeedHandler(
    private val namedGraphs: BoMNamedGraphStore,
) : SeedDocumentHandler {
    override val kind: String = SEED_KIND_GRAPH

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val name = requireText(document.raw, "name", document.index)
        val annotations = stringStringMap(document.raw["annotations"], document.index, "annotations")
        val graphId = parseOptionalUuid(document.raw["id"], document.index, "id")
            ?: UUID.nameUUIDFromBytes("graph-seed:$name".toByteArray())
        val entitiesRaw = document.raw["entities"] as? List<*>
            ?: throw SeedDocumentParseException(document.index, "Graph requires entities list")
        val edgesRaw = document.raw["edges"] as? List<*> ?: emptyList<Any?>()

        val entityKeys = linkedMapOf<String, UUID>()
        val entities = mutableListOf<BoMEntity>()
        entitiesRaw.forEachIndexed { i, raw ->
            val map = asObject(raw, document.index, "entities[$i]")
            val key = requireText(map, "key", document.index, "entities[$i].key")
            if (key in entityKeys) {
                throw SeedDocumentParseException(document.index, "Duplicate entity key: $key")
            }
            val id = parseOptionalUuid(map["id"], document.index, "entities[$i].id")
                ?: UuidV5.entityId(name, key)
            entityKeys[key] = id
            entities += BoMEntity(
                id = id,
                type = requireText(map, "type", document.index, "entities[$i].type"),
                schemaVersion = requireText(map, "schemaVersion", document.index, "entities[$i].schemaVersion"),
                payload = stringKeyedMap(map["payload"]).toMutableMap(),
                annotations = stringStringMap(map["annotations"], document.index, "entities[$i].annotations"),
            )
        }

        val edgeKeys = linkedMapOf<String, UUID>()
        val edges = mutableListOf<BoMEdge>()
        edgesRaw.forEachIndexed { i, raw ->
            val map = asObject(raw, document.index, "edges[$i]")
            val key = requireText(map, "key", document.index, "edges[$i].key")
            if (key in edgeKeys) {
                throw SeedDocumentParseException(document.index, "Duplicate edge key: $key")
            }
            val sourceKey = requireText(map, "source", document.index, "edges[$i].source")
            val targetKey = requireText(map, "target", document.index, "edges[$i].target")
            val sourceId = entityKeys[sourceKey]
                ?: throw SeedDocumentParseException(
                    document.index,
                    "Edge '$key' source '$sourceKey' is not defined in this Graph",
                )
            val targetId = entityKeys[targetKey]
                ?: throw SeedDocumentParseException(
                    document.index,
                    "Edge '$key' target '$targetKey' is not defined in this Graph",
                )
            val id = parseOptionalUuid(map["id"], document.index, "edges[$i].id")
                ?: UuidV5.edgeId(name, key)
            edgeKeys[key] = id
            val properties = map["properties"]?.let { stringKeyedMap(it).toMutableMap() }
            edges += BoMEdge(
                id = id,
                graphId = graphId,
                source = sourceId,
                target = targetId,
                role = requireText(map, "role", document.index, "edges[$i].role"),
                type = map["type"]?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                schemaVersion = map["schemaVersion"]?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                properties = properties,
            )
        }

        return ParsedSeedDocument(
            document = document,
            identity = name,
            payload = SeedGraphPayload(
                name = name,
                graphId = graphId,
                annotations = annotations,
                graph = BoMGraph(entities = entities, edges = edges),
                entityKeys = entityKeys,
                edgeKeys = edgeKeys,
            ),
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val payload = parsed.payload as SeedGraphPayload
        if (namedGraphs.get(payload.graphId) == null) {
            namedGraphs.create(
                BoMGraphSpec(
                    id = payload.graphId,
                    annotations = payload.annotations,
                ),
            )
        }
        val result = namedGraphs.mutate(
            payload.graphId,
            BoMGraphMutation(
                upsert = BoMGraphUpsert(
                    entities = payload.graph.entities,
                    edges = payload.graph.edges,
                ),
            ),
        )
        if (!result.isValid) {
            throw SeedDocumentValidationException(
                parsed.document.index,
                result.issues,
                "Graph validation failed: ${result.issues.joinToString("; ") { it.message }}",
            )
        }
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }

    fun serialize(
        name: String,
        entities: List<BoMEntity>,
        edges: List<BoMEdge>,
        entityKeys: Map<UUID, String>,
        edgeKeys: Map<UUID, String>,
        graphId: UUID? = null,
        annotations: Map<String, String> = emptyMap(),
    ): Map<String, Any?> {
        val entityDocs = entities.map { entity ->
            val key = entityKeys[entity.id]
                ?: throw IllegalArgumentException("Missing export key for entity ${entity.id}")
            linkedMapOf<String, Any?>(
                "key" to key,
                "id" to entity.id.toString(),
                "type" to entity.type,
                "schemaVersion" to entity.schemaVersion,
                "annotations" to entity.annotations.toSortedMap(),
                "payload" to entity.payload.toSortedMap(compareBy { it }),
            )
        }
        val edgeDocs = edges.map { edge ->
            val key = edgeKeys[edge.id]
                ?: throw IllegalArgumentException("Missing export key for edge ${edge.id}")
            val sourceKey = entityKeys[edge.source]
                ?: throw IllegalArgumentException("Missing export key for edge source ${edge.source}")
            val targetKey = entityKeys[edge.target]
                ?: throw IllegalArgumentException("Missing export key for edge target ${edge.target}")
            val doc = linkedMapOf<String, Any?>(
                "key" to key,
                "id" to edge.id.toString(),
                "source" to sourceKey,
                "target" to targetKey,
                "role" to edge.role,
            )
            if (edge.type != null) doc["type"] = edge.type
            if (edge.schemaVersion != null) doc["schemaVersion"] = edge.schemaVersion
            if (edge.properties != null) {
                doc["properties"] = edge.properties!!.toSortedMap(compareBy { it })
            }
            doc
        }
        val doc = linkedMapOf<String, Any?>(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to kind,
            "name" to name,
        )
        if (graphId != null) {
            doc["id"] = graphId.toString()
        }
        if (annotations.isNotEmpty()) {
            doc["annotations"] = annotations.toSortedMap()
        }
        doc["entities"] = entityDocs
        doc["edges"] = edgeDocs
        return doc
    }

    private fun parseOptionalUuid(raw: Any?, index: Int, path: String): UUID? {
        if (raw == null) return null
        val text = raw.toString().trim()
        if (text.isEmpty()) return null
        return try {
            UUID.fromString(text)
        } catch (_: IllegalArgumentException) {
            throw SeedDocumentParseException(index, "$path must be a UUID")
        }
    }

    private fun asObject(raw: Any?, index: Int, path: String): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return raw as? Map<String, Any?>
            ?: throw SeedDocumentParseException(index, "$path must be an object")
    }

    private fun stringKeyedMap(raw: Any?): Map<String, Any?> {
        if (raw == null) return emptyMap()
        @Suppress("UNCHECKED_CAST")
        val map = raw as? Map<*, *> ?: return emptyMap()
        return map.entries.associate { it.key.toString() to it.value }
    }

    private fun stringStringMap(raw: Any?, index: Int, path: String): MutableMap<String, String> {
        if (raw == null) return mutableMapOf()
        @Suppress("UNCHECKED_CAST")
        val map = raw as? Map<*, *>
            ?: throw SeedDocumentParseException(index, "$path must be an object")
        return map.entries.associate { (k, v) ->
            k.toString() to (v?.toString() ?: "")
        }.toMutableMap()
    }
}
