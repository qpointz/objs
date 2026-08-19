package org.poc.objs.core.domain

/**
 * Find-only duplicate cluster: pool entities of [type] that share the same
 * [BoMIdentityProjection] map (size > 1). Empty identity is never grouped.
 */
data class BoMDuplicateGroup(
    val type: String,
    val identity: Map<String, Any?>,
    val entities: List<BoMEntity>,
)
