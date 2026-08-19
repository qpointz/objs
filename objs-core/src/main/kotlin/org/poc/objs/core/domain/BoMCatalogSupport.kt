package org.poc.objs.core.domain

import org.springframework.stereotype.Component

data class BoMFieldHint(
    val path: String,
    val title: String,
    val fieldType: String,
    val searchable: Boolean,
    val identifier: Boolean,
)

data class BoMTypeAllowedEdges(
    val incoming: List<BoMAllowedEdgeRule>,
    val outgoing: List<BoMAllowedEdgeRule>,
)

/**
 * Shared catalog façade for latest schema, field hints, allow-list-for-type, display label,
 * and equality filter → `obj-expr` (C-17 WI-002).
 */
@Component
class BoMCatalogSupport(
    private val schemas: BoMSchemaCatalog,
    private val edges: BoMAllowedEdgeCatalog,
) {
    fun latestEntitySchema(type: String): BoMSchema? =
        BoMSchemaVersion.maxByVersion(
            schemas.listByType(type).filter { it.usage == BoMSchemaUsage.ENTITY },
        ) { it.version }

    fun latestSchema(type: String): BoMSchema? =
        BoMSchemaVersion.maxByVersion(schemas.listByType(type)) { it.version }

    fun latestEntitySchemas(): List<BoMSchema> =
        schemas.types()
            .sorted()
            .mapNotNull { latestEntitySchema(it) }

    fun fieldHints(schema: BoMSchema): List<BoMFieldHint> {
        val root = schema.contentSchema
        require(root.type == BoMSchemaType.OBJECT) { "entity contentSchema must be OBJECT" }
        val out = mutableListOf<BoMFieldHint>()
        walk(root, prefix = "", out = out, searchableOrIdentifierOnly = true)
        return out
    }

    fun firstLevelScalarFields(schema: BoMSchema): List<BoMFieldHint> {
        val root = schema.contentSchema
        require(root.type == BoMSchemaType.OBJECT) { "entity contentSchema must be OBJECT" }
        return root.fields.orEmpty().mapNotNull { field ->
            when (field.schema.type) {
                BoMSchemaType.OBJECT, BoMSchemaType.ARRAY -> null
                else ->
                    BoMFieldHint(
                        path = field.name,
                        title = fieldDisplayTitle(field),
                        fieldType = field.schema.type.name,
                        searchable = field.searchable,
                        identifier = field.identifier,
                    )
            }
        }
    }

    fun allowedEdgesForType(type: String): BoMTypeAllowedEdges {
        val incoming = mutableListOf<BoMAllowedEdgeRule>()
        val outgoing = mutableListOf<BoMAllowedEdgeRule>()
        for (rule in edges.all()) {
            if (matchesType(rule.targetType, type)) incoming += rule
            if (matchesType(rule.sourceType, type)) outgoing += rule
        }
        return BoMTypeAllowedEdges(incoming = incoming, outgoing = outgoing)
    }

    /**
     * G-A10: `payload["name"]` if non-blank; else first identifier path value; else [type].
     */
    fun displayLabel(payload: Map<String, Any?>, type: String, schema: BoMSchema? = null): String {
        payload["name"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val hints = schema?.let { fieldHints(it) }.orEmpty().filter { it.identifier }
        for (hint in hints) {
            nestedValue(payload, hint.path)?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return type
    }

    /**
     * Equality-only `obj-expr`, `&&` joined. Keys are payload paths, or `type` / `id` / `schemaVersion`.
     */
    fun filterMapToObjExpr(filters: Map<String, String>): String {
        val parts = mutableListOf<String>()
        for ((rawKey, rawValue) in filters) {
            val key = rawKey.trim()
            val value = rawValue.trim()
            if (key.isEmpty() || value.isEmpty()) continue
            val lhs =
                when (key) {
                    "type", "id", "schemaVersion" -> key
                    else -> {
                        val path = key.removePrefix("p.").removePrefix("p['").removeSuffix("']")
                        "p['${escape(path)}']"
                    }
                }
            parts += "$lhs == '${escape(value)}'"
        }
        return parts.joinToString(" && ")
    }

    private fun walk(
        node: BoMSchemaNode,
        prefix: String,
        out: MutableList<BoMFieldHint>,
        searchableOrIdentifierOnly: Boolean,
    ) {
        if (node.type != BoMSchemaType.OBJECT) return
        for (field in node.fields.orEmpty()) {
            val path = if (prefix.isEmpty()) field.name else "$prefix.${field.name}"
            when (field.schema.type) {
                BoMSchemaType.OBJECT -> walk(field.schema, path, out, searchableOrIdentifierOnly)
                BoMSchemaType.ARRAY -> Unit
                else -> {
                    if (!searchableOrIdentifierOnly || field.searchable || field.identifier) {
                        out +=
                            BoMFieldHint(
                                path = path,
                                title = fieldDisplayTitle(field),
                                fieldType = field.schema.type.name,
                                searchable = field.searchable,
                                identifier = field.identifier,
                            )
                    }
                }
            }
        }
    }

    companion object {
        private val GENERIC_SCALAR_TITLES =
            setOf("Text", "URI", "Date and time", "Number", "Integer", "Boolean")

        fun fieldDisplayTitle(field: BoMSchemaField): String {
            val title = field.schema.title.trim()
            if (title.isNotEmpty() && title !in GENERIC_SCALAR_TITLES) {
                return title
            }
            return field.name
        }

        fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

        fun matchesType(pattern: String, type: String): Boolean =
            pattern == BoMAllowedEdgeRule.ANY || pattern == type

        fun nestedValue(payload: Map<String, Any?>, path: String): Any? {
            var current: Any? = payload
            for (part in path.split('.')) {
                current = (current as? Map<*, *>)?.get(part)
            }
            return current
        }
    }
}
