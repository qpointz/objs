package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMResolvedGraph
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.BoMGraphException
import org.poc.objs.core.domain.BoMGraphListItem
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.validation.BoMEntityTypeLookup
import org.poc.objs.core.validation.BoMPersistGate
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.poc.objs.core.validation.BoMValidator
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Graph store: entity membership is M2M (`bom_graph_entity`); edges are graph-owned via
 * `bom_graph_edge.graph_id` (C-13 — no more `bom_subgraph_edges` M2M).
 */
@Service
class BoMNamedGraphStore(
    private val graphRepository: BoMGraphRepository,
    private val membershipRepository: BoMGraphMembershipRepository,
    private val entityRepository: BoMEntityRepository,
    private val edgeRepository: BoMEdgeRepository,
    private val validator: BoMValidator,
    @Lazy private val graphStore: BoMGraphStore,
) {
    private fun gate(): BoMPersistGate = BoMPersistGate(
        validator = validator,
        storeLookup = BoMEntityTypeLookup { id -> entityRepository.findById(id).map { it.type }.orElse(null) },
        existsEntity = { id -> entityRepository.existsById(id) },
        existsEdge = { id -> edgeRepository.existsById(id) },
    )

    @Transactional
    fun create(spec: BoMGraphSpec): BoMResolvedGraph {
        val id = spec.id ?: UUID.randomUUID()
        if (graphRepository.existsById(id)) {
            throw BoMGraphException(
                code = "GRAPH_ID_CONFLICT",
                message = "Subgraph already exists: $id",
            )
        }
        validateMembership(spec.entityIds, spec.edgeIds)
        graphRepository.save(
            BoMGraphRecord(
                id = id,
                annotations = spec.annotations.toMutableMap(),
            ),
        )
        replaceMembership(id, spec.entityIds, spec.edgeIds)
        return requireNotNull(get(id))
    }

    @Transactional
    fun replace(id: UUID, spec: BoMGraphSpec): BoMResolvedGraph {
        val existing = graphRepository.findById(id).orElse(null)
            ?: throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        validateMembership(spec.entityIds, spec.edgeIds)
        existing.annotations = spec.annotations.toMutableMap()
        graphRepository.save(existing)
        membershipRepository.deleteByGraphId(id)
        replaceMembership(id, spec.entityIds, spec.edgeIds)
        return requireNotNull(get(id))
    }

    @Transactional
    fun delete(id: UUID) {
        if (!graphRepository.existsById(id)) {
            throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        }
        graphRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): BoMResolvedGraph? {
        val header = graphRepository.findById(id).orElse(null) ?: return null
        return resolve(header)
    }

    @Transactional(readOnly = true)
    fun list(): List<BoMGraphListItem> =
        graphRepository.findAll().map { header ->
            BoMGraphListItem(
                id = header.id,
                annotations = header.annotations.toMap(),
                entityCount = membershipRepository.countByGraphId(header.id),
                edgeCount = edgeRepository.countByGraphId(header.id),
            )
        }

    /**
     * Hard materialization: clone members (new ids) into a brand-new graph, stamp [annotations]
     * on clones and the new header. Source graph is unchanged (G-S13–G-S15).
     */
    @Transactional
    private fun snapshot(sourceId: UUID, annotations: Map<String, String>): BoMResolvedGraph {
        val source = get(sourceId)
            ?: throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $sourceId",
            )
        val newGraphId = UUID.randomUUID()
        val idMap = linkedMapOf<UUID, UUID>()
        val newEntities = source.contents.entities.map { entity ->
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
        val newEdges = source.contents.edges.map { edge ->
            val oldEdgeId = requireNotNull(edge.id) { "member edge missing id" }
            BoMEdge(
                id = UUID.randomUUID(),
                graphId = newGraphId,
                source = requireNotNull(idMap[edge.source]) { "edge $oldEdgeId source not in pack" },
                target = requireNotNull(idMap[edge.target]) { "edge $oldEdgeId target not in pack" },
                role = edge.role,
                type = edge.type,
                schemaVersion = edge.schemaVersion,
                properties = edge.properties?.let { deepCopyMap(it) },
            )
        }
        // Header must exist before edges referencing it via graph_id (NOT NULL FK) are written.
        graphRepository.save(BoMGraphRecord(id = newGraphId, annotations = annotations.toMutableMap()))
        val writeResult = graphStore.write(
            BoMGraph(
                entities = newEntities.toMutableList(),
                edges = newEdges.toMutableList(),
            ),
        )
        if (!writeResult.isValid) {
            throw BoMGraphException(
                code = "GRAPH_CLONE_VALIDATE",
                message = writeResult.issues.joinToString("; ") { it.message ?: it.code },
            )
        }
        if (newEntities.isNotEmpty()) {
            membershipRepository.saveAll(
                newEntities.map { BoMGraphMembershipRecord(graphId = newGraphId, entityId = requireNotNull(it.id)) },
            )
        }
        return requireNotNull(get(newGraphId))
    }

    /**
     * Story vocabulary alias for [snapshot]: clone [sourceId] into a brand-new, independent graph
     * (new entity/edge ids, no parent FK — snapshot hierarchy is an application concern, not objs).
     */
    @Transactional
    fun clone(sourceId: UUID, annotations: Map<String, String> = emptyMap()): BoMResolvedGraph =
        snapshot(sourceId, annotations)

    /**
     * Attach an existing pool entity to [graphId] (membership row only; idempotent).
     */
    @Transactional
    fun attach(graphId: UUID, entityId: UUID) {
        requireGraphExists(graphId)
        if (!entityRepository.existsById(entityId)) {
            throw BoMGraphException(code = "GRAPH_ENTITY_MISSING", message = "Entity not found: $entityId")
        }
        membershipRepository.save(BoMGraphMembershipRecord(graphId = graphId, entityId = entityId))
    }

    /**
     * Detach [entityId] from [graphId] (membership row only; pool entity kept) and drop this
     * graph's edges incident to it (edges cannot survive without a member endpoint).
     */
    @Transactional
    fun detach(graphId: UUID, entityId: UUID) {
        requireGraphExists(graphId)
        membershipRepository.deleteByGraphIdAndEntityId(graphId, entityId)
        edgeRepository.findByGraphId(graphId)
            .filter { it.sourceId == entityId || it.targetId == entityId }
            .forEach { edgeRepository.delete(it) }
    }

    /**
     * Transactional graph-scoped mutation (WI-004): validate, then explicit edge deletes
     * (only this graph's edges), entity **detach** (membership + incident edges; pool entities
     * kept), then upserts — entity upsert lands in the pool + this graph's membership; edge
     * upsert is stamped with [graphId] and requires both endpoints to be projected members.
     *
     * Same id in delete and upsert: upsert wins (mirrors [BoMGraphStore.mutate]).
     */
    @Transactional
    fun mutate(graphId: UUID, mutation: BoMGraphMutation): BoMValidationResult {
        val result = validateMutate(graphId, mutation)
        if (!result.isValid) {
            return result
        }
        applyGraphDeletes(graphId, mutation)
        if (mutation.upsert.entities.isNotEmpty()) {
            graphStore.upsertEntities(mutation.upsert.entities)
            mutation.upsert.entities.forEach { entity ->
                membershipRepository.save(
                    BoMGraphMembershipRecord(graphId = graphId, entityId = requireNotNull(entity.id)),
                )
            }
        }
        applyGraphEdgeUpserts(graphId, mutation.upsert.edges)
        return BoMValidationResult.ok()
    }

    /**
     * Dry-run validation for [mutate]: same checks, no persistence. May assign ids to upsert
     * entities/edges (via [BoMPersistGate.prepareIds]) and stamps [graphId] onto upsert edges.
     */
    @Transactional(readOnly = true)
    fun validateMutate(graphId: UUID, mutation: BoMGraphMutation): BoMValidationResult {
        requireGraphExists(graphId)
        mutation.upsert.edges.forEach { it.graphId = graphId }

        val g = gate()
        val issues = mutableListOf<BoMValidationIssue>()
        for (id in mutation.delete.edges.distinct()) {
            issues.addAll(g.validateDeleteEdge(id).issues)
        }
        for (id in mutation.delete.entities.distinct()) {
            issues.addAll(g.validateDeleteEntity(id).issues)
        }
        if (issues.isNotEmpty()) {
            return BoMValidationResult.of(issues)
        }
        if (!mutation.hasUpserts()) {
            return BoMValidationResult.ok()
        }

        val graph = mutation.graph()
        val stage1 = validator.validateEntities(graph.entities)
        if (!stage1.isValid) {
            return stage1
        }
        g.prepareIds(graph)

        val deletedEntityIds = mutation.delete.entities.toSet()
        val projectedStore = BoMEntityTypeLookup { id ->
            if (id in deletedEntityIds) null else entityRepository.findById(id).map { it.type }.orElse(null)
        }
        val lookup = validator.combinedLookup(graph.entities, projectedStore)
        val edgeIssues = validator.validateEdges(graph.edges, lookup).issues.toMutableList()

        val currentMembers = membershipRepository.findByGraphId(graphId).mapTo(hashSetOf()) { it.entityId }
        val projectedMembers = (currentMembers - deletedEntityIds) + graph.entities.mapNotNull { it.id }
        graph.edges.forEachIndexed { index, edge ->
            if (edge.source !in projectedMembers) {
                edgeIssues += BoMValidationIssue(
                    code = "EDGE_ENDPOINT_NOT_MEMBER",
                    message = "Edge source ${edge.source} is not a member of graph $graphId",
                    path = "edges[$index].source",
                )
            }
            if (edge.target !in projectedMembers) {
                edgeIssues += BoMValidationIssue(
                    code = "EDGE_ENDPOINT_NOT_MEMBER",
                    message = "Edge target ${edge.target} is not a member of graph $graphId",
                    path = "edges[$index].target",
                )
            }
        }
        return BoMValidationResult(edgeIssues)
    }

    private fun applyGraphDeletes(graphId: UUID, mutation: BoMGraphMutation) {
        val graphEdgeIds = edgeRepository.findByGraphId(graphId).mapNotNullTo(hashSetOf()) { it.id }
        for (id in mutation.delete.edges.distinct()) {
            if (id in graphEdgeIds) {
                edgeRepository.deleteById(id)
            }
        }
        val entityIds = mutation.delete.entities.distinct()
        if (entityIds.isEmpty()) {
            return
        }
        val idSet = entityIds.toSet()
        edgeRepository.findByGraphId(graphId)
            .filter { it.sourceId in idSet || it.targetId in idSet }
            .forEach { edgeRepository.delete(it) }
        entityIds.forEach { id -> membershipRepository.deleteByGraphIdAndEntityId(graphId, id) }
    }

    private fun applyGraphEdgeUpserts(graphId: UUID, edges: List<BoMEdge>) {
        for (edge in edges) {
            val id = requireNotNull(edge.id)
            val existing = edgeRepository.findById(id).orElse(null)
            val record = existing ?: BoMEdgeRecord(id = id)
            record.graphId = graphId
            record.sourceId = edge.source
            record.targetId = edge.target
            record.role = edge.role
            record.type = edge.type
            record.schemaVersion = edge.schemaVersion
            record.properties = edge.properties?.toMutableMap()
            edgeRepository.save(record)
        }
    }

    private fun requireGraphExists(graphId: UUID) {
        if (!graphRepository.existsById(graphId)) {
            throw BoMGraphException(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId")
        }
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

    /**
     * Entity membership is stored as M2M rows. Edges are owned via `graph_id`: any edge listed
     * in [edgeIds] is (re)assigned to [graphId]; edges previously owned by [graphId] but no
     * longer listed are removed (they cannot exist without a graph).
     */
    private fun replaceMembership(graphId: UUID, entityIds: Set<UUID>, edgeIds: Set<UUID>) {
        if (entityIds.isNotEmpty()) {
            membershipRepository.saveAll(
                entityIds.map { BoMGraphMembershipRecord(graphId = graphId, entityId = it) },
            )
        }
        val currentEdgeIds = edgeRepository.findByGraphId(graphId).mapNotNullTo(linkedSetOf()) { it.id }
        val toRemove = currentEdgeIds - edgeIds
        if (toRemove.isNotEmpty()) {
            edgeRepository.deleteAllById(toRemove)
        }
        for (edgeId in edgeIds) {
            val edge = edgeRepository.findById(edgeId).orElseThrow {
                BoMGraphException(code = "GRAPH_EDGE_MISSING", message = "Edge not found: $edgeId")
            }
            edge.graphId = graphId
            edgeRepository.save(edge)
        }
    }

    private fun validateMembership(entityIds: Set<UUID>, edgeIds: Set<UUID>) {
        for (entityId in entityIds) {
            if (!entityRepository.existsById(entityId)) {
                throw BoMGraphException(
                    code = "GRAPH_ENTITY_MISSING",
                    message = "Entity not found: $entityId",
                )
            }
        }
        for (edgeId in edgeIds) {
            val edge = edgeRepository.findById(edgeId).orElse(null)
                ?: throw BoMGraphException(
                    code = "GRAPH_EDGE_MISSING",
                    message = "Edge not found: $edgeId",
                )
            if (edge.sourceId !in entityIds || edge.targetId !in entityIds) {
                throw BoMGraphException(
                    code = "GRAPH_EDGE_ENDPOINTS",
                    message = "Edge $edgeId endpoints must both be subgraph entity members",
                )
            }
        }
    }

    private fun resolve(header: BoMGraphRecord): BoMResolvedGraph {
        val entityIds = membershipRepository.findByGraphId(header.id).map { it.entityId }
        val entities = if (entityIds.isEmpty()) {
            emptyList()
        } else {
            entityRepository.findAllById(entityIds).map { it.toDomain() }
        }
        val edges = edgeRepository.findByGraphId(header.id).map { it.toDomain() }
        return BoMResolvedGraph(
            id = header.id,
            annotations = header.annotations.toMap(),
            contents = BoMGraphContents(entities = entities, edges = edges),
        )
    }
}
