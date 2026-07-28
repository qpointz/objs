package org.poc.objs.core.validation

import org.poc.objs.core.domain.BoEdge
import org.poc.objs.core.domain.BoEntity
import org.poc.objs.core.domain.BoGraph
import org.poc.objs.core.domain.UuidV7
import java.util.UUID

/**
 * Persist-boundary gate: create/update/delete with two-stage batch validation (G-9, G-19, G-20).
 *
 * Id rules (batch-friendly):
 * - no id → create (assign UUID v7 in [prepareIds])
 * - id present and not in store → create with client-supplied id (edges can reference new entities)
 * - id present and in store → update
 */
class BoPersistGate(
    private val validator: BoValidator,
    private val storeLookup: BoEntityTypeLookup,
    private val existsEntity: (UUID) -> Boolean,
    private val existsEdge: (UUID) -> Boolean,
) {
    /**
     * Two-stage validation for a subgraph write payload.
     * Assigns missing ids before edge validation so new entities are addressable.
     */
    fun validateWrite(graph: BoGraph): BoValidationResult {
        val stage1 = validator.validateEntities(graph.entities)
        if (!stage1.isValid) {
            return stage1
        }
        prepareIds(graph)
        val lookup = validator.combinedLookup(graph.entities, storeLookup)
        return validator.validateEdges(graph.edges, lookup)
    }

    fun validateDeleteEntity(id: UUID): BoValidationResult {
        if (!existsEntity(id)) {
            return BoValidationResult.of(
                BoValidationIssue(code = "ENTITY_NOT_FOUND", message = "Entity $id not found"),
            )
        }
        return BoValidationResult.ok()
    }

    fun validateDeleteEdge(id: UUID): BoValidationResult {
        if (!existsEdge(id)) {
            return BoValidationResult.of(
                BoValidationIssue(code = "EDGE_NOT_FOUND", message = "Edge $id not found"),
            )
        }
        return BoValidationResult.ok()
    }

    /** Assign UUID v7 to items without id. */
    fun prepareIds(graph: BoGraph) {
        graph.entities.forEach { e ->
            if (e.id == null) {
                e.id = UuidV7.generate()
            }
        }
        graph.edges.forEach { e ->
            if (e.id == null) {
                e.id = UuidV7.generate()
            }
        }
    }

    fun isCreateEntity(entity: BoEntity): Boolean {
        val id = entity.id ?: return true
        return !existsEntity(id)
    }

    fun isCreateEdge(edge: BoEdge): Boolean {
        val id = edge.id ?: return true
        return !existsEdge(id)
    }
}
