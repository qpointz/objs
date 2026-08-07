package org.poc.objs.core.domain

/**
 * Options for full-catalog JSON Schema export (codegen-oriented projections).
 *
 * Defaults match the historical exporter: draft 2020-12, outbound-only edges,
 * and edge-property schemas included in `$defs` when edges are present.
 */
data class BoMJsonSchemaExportOptions(
    val dialect: BoMJsonSchemaDialect = BoMJsonSchemaDialect.DRAFT_2020_12,
    val includeEdges: BoMJsonSchemaEdgeInclusion = BoMJsonSchemaEdgeInclusion.OUTBOUND,
    val includeEdgePropertySchemas: Boolean = true,
) {
    fun toWireMap(): Map<String, Any?> = linkedMapOf(
        "dialect" to dialect.wire,
        "includeEdges" to includeEdges.wire,
        "includeEdgePropertySchemas" to includeEdgePropertySchemas,
    )

    companion object {
        val DEFAULT: BoMJsonSchemaExportOptions = BoMJsonSchemaExportOptions()

        /**
         * Build options from optional wire strings (REST query params).
         * Blank / null values use defaults. Throws [BoMJsonSchemaExportOptionsException] on bad input.
         */
        fun fromWire(
            dialect: String? = null,
            includeEdges: String? = null,
            includeEdgePropertySchemas: Boolean? = null,
        ): BoMJsonSchemaExportOptions = BoMJsonSchemaExportOptions(
            dialect = dialect?.takeIf { it.isNotBlank() }?.let { BoMJsonSchemaDialect.fromWire(it) }
                ?: BoMJsonSchemaDialect.DRAFT_2020_12,
            includeEdges = includeEdges?.takeIf { it.isNotBlank() }?.let { BoMJsonSchemaEdgeInclusion.fromWire(it) }
                ?: BoMJsonSchemaEdgeInclusion.OUTBOUND,
            includeEdgePropertySchemas = includeEdgePropertySchemas ?: true,
        )
    }
}

enum class BoMJsonSchemaDialect(val wire: String, val schemaUri: String) {
    DRAFT_2020_12("2020-12", BoMJsonSchema.DIALECT),
    ;

    companion object {
        fun fromWire(raw: String): BoMJsonSchemaDialect {
            val key = raw.trim()
            return entries.find { it.wire.equals(key, ignoreCase = true) }
                ?: throw BoMJsonSchemaExportOptionsException(
                    "Unknown JSON Schema dialect='$raw' (supported: ${entries.joinToString { it.wire }})",
                )
        }
    }
}

enum class BoMJsonSchemaEdgeInclusion(val wire: String) {
    OUTBOUND("outbound"),
    LINKED("linked"),
    ;

    companion object {
        fun fromWire(raw: String): BoMJsonSchemaEdgeInclusion {
            val key = raw.trim().lowercase()
            return entries.find { it.wire == key }
                ?: throw BoMJsonSchemaExportOptionsException(
                    "Unknown includeEdges='$raw' (supported: ${entries.joinToString { it.wire }})",
                )
        }
    }
}

class BoMJsonSchemaExportOptionsException(message: String) : IllegalArgumentException(message)
