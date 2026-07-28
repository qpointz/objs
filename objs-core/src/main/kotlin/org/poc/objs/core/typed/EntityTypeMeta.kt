package org.poc.objs.core.typed

/**
 * Catalog identity for a typed entity or edge-property schema.
 */
data class EntityTypeMeta(
    val type: String,
    val schemaVersion: String,
    /** Optional classpath resource path to a JSON Schema document. */
    val schemaResource: String? = null,
)
