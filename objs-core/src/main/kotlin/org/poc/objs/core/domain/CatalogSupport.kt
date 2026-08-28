package org.poc.objs.core.domain

import org.poc.objs.api.domain.*

import org.springframework.stereotype.Component

data class FieldHint(
    val path: String,
    val title: String,
    val fieldType: String,
    val searchable: Boolean,
    val identifier: Boolean,
)

data class TypeAllowedEdges(
    val incoming: List<AllowedEdgeRule>,
    val outgoing: List<AllowedEdgeRule>,
)

/**
 * Shared catalog façade for latest schema, field hints, allow-list-for-type, display label,
 * and equality filter → `obj-expr` (C-17 WI-002).
 */
@Component
class CatalogSupport(
    private val schemas: SchemaCatalog,
    private val edges: AllowedEdgeCatalog,
) {
    fun latestEntitySchema(type: String): Schema? =
        SchemaVersion.maxByVersion(
            schemas.listByType(type).filter { it.usage == SchemaUsage.ENTITY },
        ) { it.version }

    fun latestSchema(type: String): Schema? =
        SchemaVersion.maxByVersion(schemas.listByType(type)) { it.version }

    fun latestEntitySchemas(): List<Schema> =
        schemas.types()
            .sorted()
            .mapNotNull { latestEntitySchema(it) }

    fun fieldHints(schema: Schema): List<FieldHint> {
        val root = schema.contentSchema
        require(root.type == SchemaType.OBJECT) { "entity contentSchema must be OBJECT" }
        val out = mutableListOf<FieldHint>()
        walk(root, prefix = "", out = out, searchableOrIdentifierOnly = true)
        return out
    }

    fun firstLevelScalarFields(schema: Schema): List<FieldHint> {
        val root = schema.contentSchema
        require(root.type == SchemaType.OBJECT) { "entity contentSchema must be OBJECT" }
        return root.fields.orEmpty().mapNotNull { field ->
            when (field.schema.type) {
                SchemaType.OBJECT, SchemaType.ARRAY -> null
                else ->
                    FieldHint(
                        path = field.name,
                        title = fieldDisplayTitle(field),
                        fieldType = field.schema.type.name,
                        searchable = field.searchable,
                        identifier = field.identifier,
                    )
            }
        }
    }

    fun allowedEdgesForType(type: String): TypeAllowedEdges {
        val incoming = mutableListOf<AllowedEdgeRule>()
        val outgoing = mutableListOf<AllowedEdgeRule>()
        for (rule in edges.all()) {
            if (matchesType(rule.targetType, type)) incoming += rule
            if (matchesType(rule.sourceType, type)) outgoing += rule
        }
        return TypeAllowedEdges(incoming = incoming, outgoing = outgoing)
    }

    /**
     * G-A10: `payload["name"]` if non-blank; else first identifier path value; else [type].
     */
    fun displayLabel(payload: Map<String, Any?>, type: String, schema: Schema? = null): String {
        payload["name"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val hints = schema?.let { fieldHints(it) }.orEmpty().filter { it.identifier }
        for (hint in hints) {
            nestedValue(payload, hint.path)?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return type
    }

    /**
     * Equality and simple operator `obj-expr`, `&&` joined. Keys are payload paths, or `type` / `id` /
     * `schemaVersion`. Filter values may use trailing `*` (prefix), or leading `>`, `>=`, `<`, `<=`.
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
            parts +=
                when {
                    key !in setOf("type", "id", "schemaVersion") && value.endsWith("*") && value.length > 1 ->
                        "$lhs =~ '^${escape(value.dropLast(1))}'"
                    value.startsWith(">=") ->
                        "$lhs >= '${escape(value.removePrefix(">="))}'"
                    value.startsWith("<=") ->
                        "$lhs <= '${escape(value.removePrefix("<="))}'"
                    value.startsWith(">") ->
                        "$lhs > '${escape(value.removePrefix(">"))}'"
                    value.startsWith("<") ->
                        "$lhs < '${escape(value.removePrefix("<"))}'"
                    else -> "$lhs == '${escape(value)}'"
                }
        }
        return parts.joinToString(" && ")
    }

    private fun walk(
        node: SchemaNode,
        prefix: String,
        out: MutableList<FieldHint>,
        searchableOrIdentifierOnly: Boolean,
    ) {
        if (node.type != SchemaType.OBJECT) return
        for (field in node.fields.orEmpty()) {
            val path = if (prefix.isEmpty()) field.name else "$prefix.${field.name}"
            when (field.schema.type) {
                SchemaType.OBJECT -> walk(field.schema, path, out, searchableOrIdentifierOnly)
                SchemaType.ARRAY -> Unit
                else -> {
                    if (!searchableOrIdentifierOnly || field.searchable || field.identifier) {
                        out +=
                            FieldHint(
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

        fun fieldDisplayTitle(field: SchemaField): String {
            val title = field.schema.title.trim()
            if (title.isNotEmpty() && title !in GENERIC_SCALAR_TITLES) {
                return title
            }
            return field.name
        }

        fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

        fun matchesType(pattern: String, type: String): Boolean =
            pattern == AllowedEdgeRule.ANY || pattern == type

        fun nestedValue(payload: Map<String, Any?>, path: String): Any? {
            var current: Any? = payload
            for (part in path.split('.')) {
                current = (current as? Map<*, *>)?.get(part)
            }
            return current
        }
    }
}
