package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMResolvedSubgraph
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.domain.BoMSubgraphException
import org.poc.objs.core.domain.BoMSubgraphListItem
import org.poc.objs.core.domain.BoMSubgraphSpec
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Soft-link subgraph store: membership by entity/edge id; resolve loads latest graph rows (G-S12).
 */
@Service
class BoMSubgraphStore(
    private val subgraphRepository: BoMSubgraphRepository,
    private val subgraphEntityRepository: BoMSubgraphEntityRepository,
    private val subgraphEdgeRepository: BoMSubgraphEdgeRepository,
    private val entityRepository: BoMEntityRepository,
    private val edgeRepository: BoMEdgeRepository,
    @Lazy private val graphStore: BoMGraphStore,
) {

    @Transactional
    fun create(spec: BoMSubgraphSpec): BoMResolvedSubgraph {
        val id = spec.id ?: UUID.randomUUID()
        if (subgraphRepository.existsById(id)) {
            throw BoMSubgraphException(
                code = "SUBGRAPH_ID_CONFLICT",
                message = "Subgraph already exists: $id",
            )
        }
        validateMembership(spec.entityIds, spec.edgeIds)
        subgraphRepository.save(
            BoMSubgraphRecord(
                id = id,
                annotations = spec.annotations.toMutableMap(),
            ),
        )
        replaceMembership(id, spec.entityIds, spec.edgeIds)
        return requireNotNull(get(id))
    }

    @Transactional
    fun replace(id: UUID, spec: BoMSubgraphSpec): BoMResolvedSubgraph {
        val existing = subgraphRepository.findById(id).orElse(null)
            ?: throw BoMSubgraphException(
                code = "SUBGRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        validateMembership(spec.entityIds, spec.edgeIds)
        existing.annotations = spec.annotations.toMutableMap()
        subgraphRepository.save(existing)
        subgraphEntityRepository.deleteBySubgraphId(id)
        subgraphEdgeRepository.deleteBySubgraphId(id)
        replaceMembership(id, spec.entityIds, spec.edgeIds)
        return requireNotNull(get(id))
    }

    @Transactional
    fun delete(id: UUID) {
        if (!subgraphRepository.existsById(id)) {
            throw BoMSubgraphException(
                code = "SUBGRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        }
        subgraphRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): BoMResolvedSubgraph? {
        val header = subgraphRepository.findById(id).orElse(null) ?: return null
        return resolve(header)
    }

    @Transactional(readOnly = true)
    fun list(): List<BoMSubgraphListItem> =
        subgraphRepository.findAll().map { header ->
            BoMSubgraphListItem(
                id = header.id,
                annotations = header.annotations.toMap(),
                entityCount = subgraphEntityRepository.countBySubgraphId(header.id),
                edgeCount = subgraphEdgeRepository.countBySubgraphId(header.id),
            )
        }

    /**
     * Hard materialization: clone members (new ids), stamp [annotations] on clones and new header,
     * create a new soft-link pack over the clones (G-S13–G-S15).
     */
    @Transactional
    fun snapshot(sourceId: UUID, annotations: Map<String, String>): BoMResolvedSubgraph {
        val source = get(sourceId)
            ?: throw BoMSubgraphException(
                code = "SUBGRAPH_NOT_FOUND",
                message = "Subgraph not found: $sourceId",
            )
        val idMap = linkedMapOf<UUID, UUID>()
        val newEntities = source.subgraph.entities.map { entity ->
            val oldId = requireNotNull(entity.id) { "member entity missing id" }
            val newId = UUID.randomUUID()
            idMap[oldId] = newId
            val merged = LinkedHashMap(entity.annotations)
            annotations.forEach { (k, v) -> merged[k] = v }
            BoMEntity(
                id = newId,
                type = entity.type,
                schemaVersion = entity.schemaVersion,
                payload = deepCopyMap(entity.payload),
                annotations = merged,
            )
        }
        val newEdges = source.subgraph.edges.map { edge ->
            val oldEdgeId = requireNotNull(edge.id) { "member edge missing id" }
            BoMEdge(
                id = UUID.randomUUID(),
                source = requireNotNull(idMap[edge.source]) { "edge $oldEdgeId source not in pack" },
                target = requireNotNull(idMap[edge.target]) { "edge $oldEdgeId target not in pack" },
                role = edge.role,
                type = edge.type,
                schemaVersion = edge.schemaVersion,
                properties = edge.properties?.let { deepCopyMap(it) },
            )
        }
        val writeResult = graphStore.write(
            BoMGraph(
                entities = newEntities.toMutableList(),
                edges = newEdges.toMutableList(),
            ),
        )
        if (!writeResult.isValid) {
            throw BoMSubgraphException(
                code = "SUBGRAPH_SNAPSHOT_VALIDATE",
                message = writeResult.issues.joinToString("; ") { it.message ?: it.code },
            )
        }
        return create(
            BoMSubgraphSpec(
                annotations = annotations,
                entityIds = newEntities.mapNotNull { it.id }.toSet(),
                edgeIds = newEdges.mapNotNull { it.id }.toSet(),
            ),
        )
    }

    private fun deepCopyMap(source: Map<String, Any?>): MutableMap<String, Any?> {
        val copy = LinkedHashMap<String, Any?>()
        for ((k, v) in source) {
            copy[k] = when (v) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    deepCopyMap(v as Map<String, Any?>)
                }
                is List<*> -> v.map { item ->
                    if (item is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        deepCopyMap(item as Map<String, Any?>)
                    } else {
                        item
                    }
                }.toMutableList()
                else -> v
            }
        }
        return copy
    }

    private fun replaceMembership(subgraphId: UUID, entityIds: Set<UUID>, edgeIds: Set<UUID>) {
        if (entityIds.isNotEmpty()) {
            subgraphEntityRepository.saveAll(
                entityIds.map { BoMSubgraphEntityRecord(subgraphId = subgraphId, entityId = it) },
            )
        }
        if (edgeIds.isNotEmpty()) {
            subgraphEdgeRepository.saveAll(
                edgeIds.map { BoMSubgraphEdgeRecord(subgraphId = subgraphId, edgeId = it) },
            )
        }
    }

    private fun validateMembership(entityIds: Set<UUID>, edgeIds: Set<UUID>) {
        for (entityId in entityIds) {
            if (!entityRepository.existsById(entityId)) {
                throw BoMSubgraphException(
                    code = "SUBGRAPH_ENTITY_MISSING",
                    message = "Entity not found: $entityId",
                )
            }
        }
        for (edgeId in edgeIds) {
            val edge = edgeRepository.findById(edgeId).orElse(null)
                ?: throw BoMSubgraphException(
                    code = "SUBGRAPH_EDGE_MISSING",
                    message = "Edge not found: $edgeId",
                )
            if (edge.sourceId !in entityIds || edge.targetId !in entityIds) {
                throw BoMSubgraphException(
                    code = "SUBGRAPH_EDGE_ENDPOINTS",
                    message = "Edge $edgeId endpoints must both be subgraph entity members",
                )
            }
        }
    }

    private fun resolve(header: BoMSubgraphRecord): BoMResolvedSubgraph {
        val entityIds = subgraphEntityRepository.findBySubgraphId(header.id).map { it.entityId }
        val edgeIds = subgraphEdgeRepository.findBySubgraphId(header.id).map { it.edgeId }
        val entities = if (entityIds.isEmpty()) {
            emptyList()
        } else {
            entityRepository.findAllById(entityIds).map { it.toDomain() }
        }
        val edges = if (edgeIds.isEmpty()) {
            emptyList()
        } else {
            edgeRepository.findAllById(edgeIds).map { it.toDomain() }
        }
        return BoMResolvedSubgraph(
            id = header.id,
            annotations = header.annotations.toMap(),
            subgraph = BoMSubgraph(entities = entities, edges = edges),
        )
    }
}
