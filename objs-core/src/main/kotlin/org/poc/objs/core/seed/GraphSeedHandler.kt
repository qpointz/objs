package org.poc.objs.core.seed

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.persistence.BoMGraphStore
import org.springframework.stereotype.Component
import java.util.UUID

data class SeedGraphPayload(
    val name: String,
    val graph: BoMGraph,
    val entityKeys: Map<String, UUID>,
    val edgeKeys: Map<String, UUID>,
)

@Component
class GraphSeedHandler(
    private val graphStore: BoMGraphStore,
) : SeedDocumentHandler {
    override val kind: String = SEED_KIND_GRAPH

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val name = requireText(document.metadata, "name", document.index)
        val entitiesRaw = document.spec["entities"] as? List<*>
            ?: throw SeedDocumentParseException(document.index, "Graph requires spec.entities list")
        val edgesRaw = document.spec["edges"] as? List<*> ?: emptyList<Any?>()

        val entityKeys = linkedMapOf<String, UUID>()
        val entities = mutableListOf<BoMEntity>()
        entitiesRaw.forEachIndexed { i, raw ->
            val map = asObject(raw, document.index, "spec.entities[$i]")
            val key = requireText(map, "key", document.index, "spec.entities[$i].key")
            if (key in entityKeys) {
                throw SeedDocumentParseException(document.index, "Duplicate entity key: $key")
            }
            val id = parseOptionalUuid(map["id"], document.index, "spec.entities[$i].id")
                ?: UuidV5.entityId(name, key)
            entityKeys[key] = id
            entities += BoMEntity(
                id = id,
                type = requireText(map, "type", document.index, "spec.entities[$i].type"),
                schemaVersion = requireText(map, "schemaVersion", document.index, "spec.entities[$i].schemaVersion"),
                payload = stringKeyedMap(map["payload"]).toMutableMap(),
                annotations = stringStringMap(map["annotations"], document.index, "spec.entities[$i].annotations"),
            )
        }

        val edgeKeys = linkedMapOf<String, UUID>()
        val edges = mutableListOf<BoMEdge>()
        edgesRaw.forEachIndexed { i, raw ->
            val map = asObject(raw, document.index, "spec.edges[$i]")
            val key = requireText(map, "key", document.index, "spec.edges[$i].key")
            if (key in edgeKeys) {
                throw SeedDocumentParseException(document.index, "Duplicate edge key: $key")
            }
            val sourceKey = requireText(map, "source", document.index, "spec.edges[$i].source")
            val targetKey = requireText(map, "target", document.index, "spec.edges[$i].target")
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
            val id = parseOptionalUuid(map["id"], document.index, "spec.edges[$i].id")
                ?: UuidV5.edgeId(name, key)
            edgeKeys[key] = id
            val properties = map["properties"]?.let { stringKeyedMap(it).toMutableMap() }
            edges += BoMEdge(
                id = id,
                source = sourceId,
                target = targetId,
                role = requireText(map, "role", document.index, "spec.edges[$i].role"),
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
                graph = BoMGraph(entities = entities, edges = edges),
                entityKeys = entityKeys,
                edgeKeys = edgeKeys,
            ),
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val payload = parsed.payload as SeedGraphPayload
        val result = graphStore.write(payload.graph)
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
        return linkedMapOf(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to kind,
            "metadata" to linkedMapOf("name" to name),
            "spec" to linkedMapOf(
                "entities" to entityDocs,
                "edges" to edgeDocs,
            ),
        )
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
