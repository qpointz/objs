package org.poc.objs.api.validation

import java.util.UUID

/**
 * Resolves entity types for edge endpoints from the write payload and/or an existing store.
 */
fun interface EntityTypeLookup {
    /**
     * @return entity type string, or null if the id is unknown
     */
    fun typeOf(id: UUID): String?
}
