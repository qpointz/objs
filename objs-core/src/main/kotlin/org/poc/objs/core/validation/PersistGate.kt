package org.poc.objs.core.validation

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import java.util.UUID

/**
 * Persist-boundary gate: create/update/delete with two-stage batch validation (G-9, G-19, G-20).
 *
 * Id rules (batch-friendly):
 * - no id → create (assign [UUID.randomUUID] in [prepareIds])
 * - id present and not in store → create with client-supplied id (edges can reference new entities)
 * - id present and in store → update
 */
class PersistGate(
    private val validator: Validator,
    private val storeLookup: EntityTypeLookup,
    private val existsEntity: (UUID) -> Boolean,
    private val existsEdge: (UUID) -> Boolean,
) {
    /**
     * Two-stage validation for a subgraph write payload.
     * Assigns missing ids before edge validation so new entities are addressable.
     */
    fun validateWrite(graph: Graph): ValidationResult {
        val stage1 = validator.validateEntities(graph.entities)
        if (!stage1.isValid) {
            return stage1
        }
        prepareIds(graph)
        val lookup = validator.combinedLookup(graph.entities, storeLookup)
        return validator.validateEdges(graph.edges, lookup)
    }

    fun validateDeleteEntity(id: UUID): ValidationResult {
        if (!existsEntity(id)) {
            return ValidationResult.of(
                ValidationIssue(code = "ENTITY_NOT_FOUND", message = "Entity $id not found"),
            )
        }
        return ValidationResult.ok()
    }

    fun validateDeleteEdge(id: UUID): ValidationResult {
        if (!existsEdge(id)) {
            return ValidationResult.of(
                ValidationIssue(code = "EDGE_NOT_FOUND", message = "Edge $id not found"),
            )
        }
        return ValidationResult.ok()
    }

    /** Assign [UUID.randomUUID] to items without id. */
    fun prepareIds(graph: Graph) {
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

    fun isCreateEntity(entity: Entity): Boolean {
        val id = entity.id ?: return true
        return !existsEntity(id)
    }

    fun isCreateEdge(edge: Edge): Boolean {
        val id = edge.id ?: return true
        return !existsEdge(id)
    }
}
