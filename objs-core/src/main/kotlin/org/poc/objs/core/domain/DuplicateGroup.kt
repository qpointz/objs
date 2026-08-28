package org.poc.objs.core.domain

import org.poc.objs.api.domain.*

/**
 * Find-only duplicate cluster: pool entities of [type] that share the same
 * [IdentityProjection] map (size > 1). Empty identity is never grouped.
 */
data class DuplicateGroup(
    val type: String,
    val identity: Map<String, Any?>,
    val entities: List<Entity>,
)
