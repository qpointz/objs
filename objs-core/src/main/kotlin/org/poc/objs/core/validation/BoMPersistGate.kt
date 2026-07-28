package org.poc.objs.core.validation

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import java.util.UUID

/**
 * Persist-boundary gate: create/update/delete with two-stage batch validation (G-9, G-19, G-20).
 *
 * Id rules (batch-friendly):
 * - no id → create (assign [UUID.randomUUID] in [prepareIds])
 * - id present and not in store → create with client-supplied id (edges can reference new entities)
 * - id present and in store → update
 */
class BoMPersistGate(
    private val validator: BoMValidator,
    private val storeLookup: BoMEntityTypeLookup,
    private val existsEntity: (UUID) -> Boolean,
    private val existsEdge: (UUID) -> Boolean,
) {
    /**
     * Two-stage validation for a subgraph write payload.
     * Assigns missing ids before edge validation so new entities are addressable.
     */
    fun validateWrite(graph: BoMGraph): BoMValidationResult {
        val stage1 = validator.validateEntities(graph.entities)
        if (!stage1.isValid) {
            return stage1
        }
        prepareIds(graph)
        val lookup = validator.combinedLookup(graph.entities, storeLookup)
        return validator.validateEdges(graph.edges, lookup)
    }

    fun validateDeleteEntity(id: UUID): BoMValidationResult {
        if (!existsEntity(id)) {
            return BoMValidationResult.of(
                BoMValidationIssue(code = "ENTITY_NOT_FOUND", message = "Entity $id not found"),
            )
        }
        return BoMValidationResult.ok()
    }

    fun validateDeleteEdge(id: UUID): BoMValidationResult {
        if (!existsEdge(id)) {
            return BoMValidationResult.of(
                BoMValidationIssue(code = "EDGE_NOT_FOUND", message = "Edge $id not found"),
            )
        }
        return BoMValidationResult.ok()
    }

    /** Assign [UUID.randomUUID] to items without id. */
    fun prepareIds(graph: BoMGraph) {
        graph.entities.forEach { e ->
            if (e.id == null) {
                e.id = UUID.randomUUID()
            }
        }
        graph.edges.forEach { e ->
            if (e.id == null) {
                e.id = UUID.randomUUID()
            }
        }
    }

    fun isCreateEntity(entity: BoMEntity): Boolean {
        val id = entity.id ?: return true
        return !existsEntity(id)
    }

    fun isCreateEdge(edge: BoMEdge): Boolean {
        val id = edge.id ?: return true
        return !existsEdge(id)
    }
}
