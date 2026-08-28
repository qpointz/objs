package org.poc.objs.core.domain

/**
 * Options for full-catalog JSON Schema export (codegen-oriented projections).
 *
 * Defaults match the historical exporter: draft 2020-12, outbound-only edges,
 * and edge-property schemas included in `$defs` when edges are present.
 */
data class JsonSchemaExportOptions(
    val dialect: JsonSchemaDialect = JsonSchemaDialect.DRAFT_2020_12,
    val includeEdges: JsonSchemaEdgeInclusion = JsonSchemaEdgeInclusion.OUTBOUND,
    val includeEdgePropertySchemas: Boolean = true,
) {
    fun toWireMap(): Map<String, Any?> = linkedMapOf(
        "dialect" to dialect.wire,
        "includeEdges" to includeEdges.wire,
        "includeEdgePropertySchemas" to includeEdgePropertySchemas,
    )

    companion object {
        val DEFAULT: JsonSchemaExportOptions = JsonSchemaExportOptions()

        /**
         * Build options from optional wire strings (REST query params).
         * Blank / null values use defaults. Throws [JsonSchemaExportOptionsException] on bad input.
         */
        fun fromWire(
            dialect: String? = null,
            includeEdges: String? = null,
            includeEdgePropertySchemas: Boolean? = null,
        ): JsonSchemaExportOptions = JsonSchemaExportOptions(
            dialect = dialect?.takeIf { it.isNotBlank() }?.let { JsonSchemaDialect.fromWire(it) }
                ?: JsonSchemaDialect.DRAFT_2020_12,
            includeEdges = includeEdges?.takeIf { it.isNotBlank() }?.let { JsonSchemaEdgeInclusion.fromWire(it) }
                ?: JsonSchemaEdgeInclusion.OUTBOUND,
            includeEdgePropertySchemas = includeEdgePropertySchemas ?: true,
        )
    }
}

enum class JsonSchemaDialect(val wire: String, val schemaUri: String) {
    DRAFT_2020_12("2020-12", JsonSchema.DIALECT),
    DRAFT_07("draft-07", "http://json-schema.org/draft-07/schema#"),
    ;

    /** Catalog reusable-schemas keyword: `$defs` (2020-12) or `definitions` (draft-07). */
    val defsKeyword: String
        get() = when (this) {
            DRAFT_2020_12 -> "\$defs"
            DRAFT_07 -> "definitions"
        }

    /** `$ref` prefix into [defsKeyword], e.g. `#/$defs/` or `#/definitions/`. */
    val defsRefPrefix: String
        get() = "#/$defsKeyword/"

    /**
     * In draft-07, `$ref` ignores sibling keywords. Wrap metadata + `$ref` in a map that keeps
     * siblings valid: put `$ref` alone under `allOf` when [exclusiveRef] is true.
     */
    val exclusiveRef: Boolean
        get() = this == DRAFT_07

    companion object {
        fun fromWire(raw: String): JsonSchemaDialect {
            val key = raw.trim()
            return entries.find { it.wire.equals(key, ignoreCase = true) }
                ?: throw JsonSchemaExportOptionsException(
                    "Unknown JSON Schema dialect='$raw' (supported: ${entries.joinToString { it.wire }})",
                )
        }
    }
}

enum class JsonSchemaEdgeInclusion(val wire: String) {
    NONE("none"),
    OUTBOUND("outbound"),
    LINKED("linked"),
    ;

    companion object {
        fun fromWire(raw: String): JsonSchemaEdgeInclusion {
            val key = raw.trim().lowercase()
            return entries.find { it.wire == key }
                ?: throw JsonSchemaExportOptionsException(
                    "Unknown includeEdges='$raw' (supported: ${entries.joinToString { it.wire }})",
                )
        }
    }
}

class JsonSchemaExportOptionsException(message: String) : IllegalArgumentException(message)
