package org.poc.objs.core.persistence

import jakarta.persistence.EntityManager
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphDelete
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMGraphUpsert
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.match.BoMAnnotationMatcher
import org.poc.objs.core.match.BoMChainedMatcher
import org.poc.objs.core.match.BoMEntityDomainCandidate
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.match.BoMSubgExprMatcher
import org.poc.objs.core.match.BoMSubgraphIdMatcher
import org.poc.objs.core.match.MatchAllAnnotationMatcher
import org.poc.objs.core.match.asBoMMatcher
import org.poc.objs.core.validation.BoMEntityTypeLookup
import org.poc.objs.core.validation.BoMPersistGate
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.poc.objs.core.validation.BoMValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Persistence facade: batch subgraph write with two-stage gate; load and subgraph select.
 */
@Service
class BoMGraphStore(
    private val entityRepository: BoMEntityRepository,
    private val edgeRepository: BoMEdgeRepository,
    private val validator: BoMValidator,
    private val rawGraphReader: BoMRawGraphReader,
    private val entityManager: EntityManager,
    private val subgraphStore: BoMSubgraphStore,
) {
    private fun gate(): BoMPersistGate = BoMPersistGate(
        validator = validator,
        storeLookup = BoMEntityTypeLookup { id -> entityRepository.findById(id).map { it.type }.orElse(null) },
        existsEntity = { id -> entityRepository.existsById(id) },
        existsEdge = { id -> edgeRepository.existsById(id) },
    )

    /**
     * Batch upsert. On success, [graph] is mutated in place with all entity/edge ids assigned (G-R5).
     */
    @Transactional
    fun write(graph: BoMGraph): BoMValidationResult =
        mutate(BoMGraphMutation(upsert = BoMGraphUpsert(entities = graph.entities, edges = graph.edges)))

    /** Dry-run write validation (may assign temporary ids on [graph] via the persist gate). */
    @Transactional(readOnly = true)
    fun validate(graph: BoMGraph): BoMValidationResult =
        validateMutation(BoMGraphMutation(upsert = BoMGraphUpsert(entities = graph.entities, edges = graph.edges)))

    /**
     * Transactional mutate: validate projected state, then explicit edge deletes,
     * entity deletes (cascade incident edges), then upserts.
     *
     * Same id in delete and upsert: upsert wins in the final store state.
     */
    @Transactional
    fun mutate(mutation: BoMGraphMutation): BoMValidationResult {
        val result = validateMutation(mutation)
        if (!result.isValid) {
            return result
        }
        applyDeletes(mutation)
        applyUpserts(mutation.graph())
        return BoMValidationResult.ok()
    }

    /**
     * Dry-run mutation validation (may assign temporary ids on upsert entities/edges).
     * Edge endpoint lookup ignores entities scheduled for delete unless also upserted.
     */
    @Transactional(readOnly = true)
    fun validateMutation(mutation: BoMGraphMutation): BoMValidationResult {
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
        val deleted = mutation.delete.entities.toSet()
        val projectedStore = BoMEntityTypeLookup { id ->
            if (id in deleted) {
                null
            } else {
                entityRepository.findById(id).map { it.type }.orElse(null)
            }
        }
        val lookup = validator.combinedLookup(graph.entities, projectedStore)
        return validator.validateEdges(graph.edges, lookup)
    }

    @Transactional
    fun deleteEntity(id: UUID): BoMValidationResult =
        mutate(BoMGraphMutation(delete = BoMGraphDelete(entities = mutableListOf(id))))

    @Transactional
    fun deleteEdge(id: UUID): BoMValidationResult =
        mutate(BoMGraphMutation(delete = BoMGraphDelete(edges = mutableListOf(id))))

    /**
     * All-or-nothing batch delete (G-R3/G-R4). Thin shim over [mutate].
     */
    @Transactional
    fun delete(
        entityIds: Collection<UUID> = emptyList(),
        edgeIds: Collection<UUID> = emptyList(),
    ): BoMValidationResult {
        if (entityIds.isEmpty() && edgeIds.isEmpty()) {
            return BoMValidationResult.of(
                BoMValidationIssue(
                    code = "DELETE_EMPTY",
                    message = "At least one entityId or edgeId is required",
                ),
            )
        }
        return mutate(
            BoMGraphMutation(
                delete = BoMGraphDelete(
                    entities = entityIds.toMutableList(),
                    edges = edgeIds.toMutableList(),
                ),
            ),
        )
    }

    private fun applyDeletes(mutation: BoMGraphMutation) {
        for (id in mutation.delete.edges.distinct()) {
            if (edgeRepository.existsById(id)) {
                edgeRepository.deleteById(id)
            }
        }
        val entityIds = mutation.delete.entities.distinct().filter { entityRepository.existsById(it) }
        if (entityIds.isEmpty()) {
            return
        }
        edgeRepository.findBySourceIdInOrTargetIdIn(entityIds, entityIds)
            .forEach { edgeRepository.delete(it) }
        entityIds.forEach { entityRepository.deleteById(it) }
    }

    private fun applyUpserts(graph: BoMGraph) {
        for (entity in graph.entities) {
            val id = requireNotNull(entity.id)
            val record = entityRepository.findById(id).orElseGet { BoMEntityRecord(id = id) }
            record.type = entity.type
            record.schemaVersion = entity.schemaVersion
            record.payload = entity.payload.toMutableMap()
            record.annotations = entity.annotations.toMutableMap()
            entityRepository.save(record)
        }
        for (edge in graph.edges) {
            val id = requireNotNull(edge.id)
            val record = edgeRepository.findById(id).orElseGet { BoMEdgeRecord(id = id) }
            record.sourceId = edge.source
            record.targetId = edge.target
            record.role = edge.role
            record.type = edge.type
            record.schemaVersion = edge.schemaVersion
            record.properties = edge.properties?.toMutableMap()
            edgeRepository.save(record)
        }
    }

    @Transactional(readOnly = true)
    fun loadAll(): BoMGraph {
        val entities = entityRepository.findAll().map { it.toDomain() }.toMutableList()
        val edges = edgeRepository.findAll().map { it.toDomain() }.toMutableList()
        return BoMGraph(entities, edges)
    }

    @Transactional(readOnly = true)
    fun selectSubgraph(matcher: BoMMatcher): BoMSubgraph {
        // JDBC reads share the Spring transaction connection; flush pending JPA writes first.
        entityManager.flush()
        val stages = flattenStages(matcher)
        val first = stages.first()
        if (first is BoMSubgraphIdMatcher || first is BoMSubgExprMatcher) {
            return selectSoftLinkSubgraph(stages)
        }
        val (entities, edges) = rawGraphReader.select(matcher)
        return BoMSubgraph(
            entities = entities.map { it.toDomain() },
            edges = edges.map { it.toDomain() },
        )
    }

    private fun selectSoftLinkSubgraph(stages: List<BoMMatcher>): BoMSubgraph {
        val first = stages.first()
        val packs = when (first) {
            is BoMSubgraphIdMatcher -> {
                val resolved = subgraphStore.get(first.id)
                    ?: throw BoMValidationException(
                        "subgraph",
                        BoMValidationResult.of(
                            BoMValidationIssue(
                                code = "MATCHER_SUBGRAPH_NOT_FOUND",
                                message = "Subgraph not found: ${first.id}",
                                path = "subgraph.id",
                            ),
                        ),
                    )
                listOf(resolved)
            }
            is BoMSubgExprMatcher ->
                subgraphStore.list().mapNotNull { item ->
                    if (first.matchesHeader(item.id, item.annotations)) {
                        subgraphStore.get(item.id)
                    } else {
                        null
                    }
                }
            else -> error("expected soft-link matcher stage")
        }
        val entityById = linkedMapOf<UUID, BoMEntity>()
        val edgeById = linkedMapOf<UUID, BoMEdge>()
        for (pack in packs) {
            for (entity in pack.subgraph.entities) {
                val id = entity.id ?: continue
                entityById[id] = entity
            }
            for (edge in pack.subgraph.edges) {
                val id = edge.id ?: continue
                edgeById[id] = edge
            }
        }
        var entities = entityById.values.toList()
        val filters = stages.drop(1)
        if (filters.isNotEmpty()) {
            entities = entities.filter { entity ->
                val candidate = BoMEntityDomainCandidate(entity)
                filters.all { it.matches(candidate) }
            }
        }
        val selectedIds = entities.mapNotNullTo(linkedSetOf()) { it.id }
        val edges = edgeById.values.filter { it.source in selectedIds && it.target in selectedIds }
        return BoMSubgraph(entities = entities, edges = edges)
    }

    private fun flattenStages(matcher: BoMMatcher): List<BoMMatcher> =
        when (matcher) {
            is BoMChainedMatcher -> matcher.matchers.flatMap(::flattenStages)
            else -> listOf(matcher)
        }

    @Transactional(readOnly = true)
    fun selectSubgraph(matcher: BoMAnnotationMatcher): BoMSubgraph =
        selectSubgraph(matcher.asBoMMatcher())

    @Transactional(readOnly = true)
    fun selectSubgraphMatchAll(filter: Map<String, String>): BoMSubgraph =
        selectSubgraph(MatchAllAnnotationMatcher(filter))
}

fun BoMEntityRecord.toDomain() = BoMEntity(
    id = id,
    type = type,
    schemaVersion = schemaVersion,
    payload = payload.toMutableMap(),
    annotations = annotations.toMutableMap(),
)

fun BoMEdgeRecord.toDomain() = BoMEdge(
    id = id,
    source = sourceId,
    target = targetId,
    role = role,
    type = type,
    schemaVersion = schemaVersion,
    properties = properties?.toMutableMap(),
)
