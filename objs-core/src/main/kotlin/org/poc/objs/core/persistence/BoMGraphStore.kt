package org.poc.objs.core.persistence

import jakarta.persistence.EntityManager
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.match.BoMAnnotationMatcher
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.match.MatchAllAnnotationMatcher
import org.poc.objs.core.match.asBoMMatcher
import org.poc.objs.core.validation.BoMEntityTypeLookup
import org.poc.objs.core.validation.BoMPersistGate
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
    fun write(graph: BoMGraph): BoMValidationResult {
        val g = gate()
        val result = g.validateWrite(graph)
        if (!result.isValid) {
            return result
        }
        // ids already prepared inside validateWrite
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
        return BoMValidationResult.ok()
    }

    /** Dry-run write validation (may assign temporary ids on [graph] via the persist gate). */
    @Transactional(readOnly = true)
    fun validate(graph: BoMGraph): BoMValidationResult = gate().validateWrite(graph)

    @Transactional
    fun deleteEntity(id: UUID): BoMValidationResult {
        val result = gate().validateDeleteEntity(id)
        if (!result.isValid) return result
        edgeRepository.findAll().filter { it.sourceId == id || it.targetId == id }
            .forEach { edgeRepository.delete(it) }
        entityRepository.deleteById(id)
        return BoMValidationResult.ok()
    }

    @Transactional
    fun deleteEdge(id: UUID): BoMValidationResult {
        val result = gate().validateDeleteEdge(id)
        if (!result.isValid) return result
        edgeRepository.deleteById(id)
        return BoMValidationResult.ok()
    }

    /**
     * All-or-nothing batch delete (G-R3/G-R4). Validates every id first; then deletes
     * requested edges, then entities (entity delete also removes incident edges).
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
        val issues = mutableListOf<BoMValidationIssue>()
        val g = gate()
        for (id in edgeIds) {
            issues.addAll(g.validateDeleteEdge(id).issues)
        }
        for (id in entityIds) {
            issues.addAll(g.validateDeleteEntity(id).issues)
        }
        if (issues.isNotEmpty()) {
            return BoMValidationResult.of(issues)
        }
        for (id in edgeIds) {
            edgeRepository.deleteById(id)
        }
        for (id in entityIds) {
            edgeRepository.findAll().filter { it.sourceId == id || it.targetId == id }
                .forEach { edgeRepository.delete(it) }
            entityRepository.deleteById(id)
        }
        return BoMValidationResult.ok()
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
        val (entities, edges) = rawGraphReader.select(matcher)
        return BoMSubgraph(
            entities = entities.map { it.toDomain() },
            edges = edges.map { it.toDomain() },
        )
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
