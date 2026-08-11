package org.poc.objs.core.domain

/**
 * Builds the flat identity map for an entity payload or edge-properties document
 * from [contentSchema] field `identifier` flags (dotted paths; arrays skipped).
 *
 * Missing, null, and blank-string values are omitted (not yet set — may be filled on update).
 */
object BoMIdentityProjection {
    fun project(contentSchema: BoMSchemaNode, payload: Map<String, Any?>): Map<String, Any?> {
        require(contentSchema.type == BoMSchemaType.OBJECT) {
            "identity projection requires an OBJECT contentSchema"
        }
        val out = linkedMapOf<String, Any?>()
        walkObject(contentSchema, payload, prefix = "", out = out)
        return out
    }

    /**
     * Dotted paths marked `identifier` on [contentSchema] (structure only; arrays skipped).
     * Used when comparing updates across schema versions: a stored identity path is frozen
     * only while the **incoming** schema still marks that path as identifier.
     */
    fun identifierPaths(contentSchema: BoMSchemaNode): Set<String> {
        require(contentSchema.type == BoMSchemaType.OBJECT) {
            "identity projection requires an OBJECT contentSchema"
        }
        val out = linkedSetOf<String>()
        walkIdentifierPaths(contentSchema, prefix = "", out = out)
        return out
    }

    /** True when an identity leaf is unset and may still be written on update. */
    fun isUnset(value: Any?): Boolean = when (value) {
        null -> true
        is String -> value.isBlank()
        else -> false
    }

    private fun walkIdentifierPaths(
        schema: BoMSchemaNode,
        prefix: String,
        out: MutableSet<String>,
    ) {
        for (field in schema.fields.orEmpty()) {
            val path = if (prefix.isEmpty()) field.name else "$prefix.${field.name}"
            when (field.schema.type) {
                BoMSchemaType.OBJECT -> walkIdentifierPaths(field.schema, path, out)
                BoMSchemaType.ARRAY -> Unit
                else -> if (field.identifier) out.add(path)
            }
        }
    }

    private fun walkObject(
        schema: BoMSchemaNode,
        payload: Map<String, Any?>?,
        prefix: String,
        out: MutableMap<String, Any?>,
    ) {
        for (field in schema.fields.orEmpty()) {
            val path = if (prefix.isEmpty()) field.name else "$prefix.${field.name}"
            val value = payload?.get(field.name)
            when (field.schema.type) {
                BoMSchemaType.OBJECT -> {
                    @Suppress("UNCHECKED_CAST")
                    val nested = value as? Map<String, Any?>
                    walkObject(field.schema, nested, path, out)
                }
                BoMSchemaType.ARRAY -> {
                    // Identity flags under arrays are illegal in the DSL; skip arrays entirely.
                }
                else -> {
                    if (field.identifier && !isUnset(value)) {
                        out[path] = value
                    }
                }
            }
        }
    }
}
