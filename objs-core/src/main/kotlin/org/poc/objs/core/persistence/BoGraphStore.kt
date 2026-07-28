package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoEdge
import org.poc.objs.core.domain.BoEntity
import org.poc.objs.core.domain.BoGraph
import org.poc.objs.core.domain.BoSubgraph
import org.poc.objs.core.match.BoAnnotationMatcher
import org.poc.objs.core.match.MatchAllAnnotationMatcher
import org.poc.objs.core.subgraph.BoSubgraphSelector
import org.poc.objs.core.validation.BoEntityTypeLookup
import org.poc.objs.core.validation.BoPersistGate
import org.poc.objs.core.validation.BoValidationResult
import org.poc.objs.core.validation.BoValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Persistence facade: batch subgraph write with two-stage gate; load and subgraph select.
 */
@Service
class BoGraphStore(
    private val entityRepository: BoEntityRepository,
    private val edgeRepository: BoEdgeRepository,
    private val validator: BoValidator,
) {
    private fun gate(): BoPersistGate = BoPersistGate(
        validator = validator,
        storeLookup = BoEntityTypeLookup { id -> entityRepository.findById(id).map { it.type }.orElse(null) },
        existsEntity = { id -> entityRepository.existsById(id) },
        existsEdge = { id -> edgeRepository.existsById(id) },
    )

    @Transactional
    fun write(graph: BoGraph): BoValidationResult {
        val g = gate()
        val result = g.validateWrite(graph)
        if (!result.isValid) {
            return result
        }
        // ids already prepared inside validateWrite
        for (entity in graph.entities) {
            val id = requireNotNull(entity.id)
            val record = entityRepository.findById(id).orElseGet { BoEntityRecord(id = id) }
            record.type = entity.type
            record.schemaVersion = entity.schemaVersion
            record.payload = entity.payload.toMutableMap()
            record.annotations = entity.annotations.toMutableMap()
            entityRepository.save(record)
        }
        for (edge in graph.edges) {
            val id = requireNotNull(edge.id)
            val record = edgeRepository.findById(id).orElseGet { BoEdgeRecord(id = id) }
            record.sourceId = edge.source
            record.targetId = edge.target
            record.role = edge.role
            record.type = edge.type
            record.schemaVersion = edge.schemaVersion
            record.properties = edge.properties?.toMutableMap()
            edgeRepository.save(record)
        }
        return BoValidationResult.ok()
    }

    @Transactional
    fun deleteEntity(id: UUID): BoValidationResult {
        val result = gate().validateDeleteEntity(id)
        if (!result.isValid) return result
        edgeRepository.findAll().filter { it.sourceId == id || it.targetId == id }
            .forEach { edgeRepository.delete(it) }
        entityRepository.deleteById(id)
        return BoValidationResult.ok()
    }

    @Transactional
    fun deleteEdge(id: UUID): BoValidationResult {
        val result = gate().validateDeleteEdge(id)
        if (!result.isValid) return result
        edgeRepository.deleteById(id)
        return BoValidationResult.ok()
    }

    @Transactional(readOnly = true)
    fun loadAll(): BoGraph {
        val entities = entityRepository.findAll().map { it.toDomain() }.toMutableList()
        val edges = edgeRepository.findAll().map { it.toDomain() }.toMutableList()
        return BoGraph(entities, edges)
    }

    @Transactional(readOnly = true)
    fun selectSubgraph(matcher: BoAnnotationMatcher): BoSubgraph =
        BoSubgraphSelector.select(loadAll(), matcher)

    @Transactional(readOnly = true)
    fun selectSubgraphMatchAll(filter: Map<String, String>): BoSubgraph =
        selectSubgraph(MatchAllAnnotationMatcher(filter))
}

fun BoEntityRecord.toDomain() = BoEntity(
    id = id,
    type = type,
    schemaVersion = schemaVersion,
    payload = payload.toMutableMap(),
    annotations = annotations.toMutableMap(),
)

fun BoEdgeRecord.toDomain() = BoEdge(
    id = id,
    source = sourceId,
    target = targetId,
    role = role,
    type = type,
    schemaVersion = schemaVersion,
    properties = properties?.toMutableMap(),
)
