package org.poc.objs.api.validation

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity

/**
 * Persist-gate validation port. Implemented by the networknt-backed [org.poc.objs.core.validation.Validator]
 * in `:objs-persistence` (G-A20 hybrid C / G-X8).
 */
interface PersistValidator {
    fun validateEntities(entities: Collection<Entity>): ValidationResult

    fun validateEdges(
        edges: Collection<Edge>,
        typeLookup: EntityTypeLookup,
    ): ValidationResult

    fun combinedLookup(
        payloadEntities: Collection<Entity>,
        storeLookup: EntityTypeLookup,
    ): EntityTypeLookup
}
