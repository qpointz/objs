package org.poc.objs.core.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Recursive, ordered DSL used to define the shape of an Objs entity payload or edge properties.
 *
 * The DSL is authoritative. JSON Schema is a generated projection used by validators and clients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SchemaNode(
    val type: SchemaType,
    val title: String,
    val description: String,
    val fields: List<SchemaField>? = null,
    val items: SchemaNode? = null,
    val values: List<EnumValue>? = null,
    val format: String? = null,
    val default: Any? = null,
)

/** Ordered field entry for an [SchemaType.OBJECT] node. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SchemaField(
    val name: String,
    val schema: SchemaNode,
    val required: Boolean = true,
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT)
    val identifier: Boolean = false,
    @get:JsonInclude(JsonInclude.Include.NON_DEFAULT)
    val searchable: Boolean = false,
    /** Presentation hints only; ignored by validation. */
    val stereotype: List<String>? = null,
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val tags: List<String> = emptyList(),
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val attributes: Map<String, String> = emptyMap(),
)

/**
 * String enum entry: persisted [value], required long [description], optional UI [caption].
 * Object editors show [caption] when non-blank, otherwise [value].
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnumValue(
    val value: String,
    val description: String,
    val caption: String? = null,
)

/** DSL node types. INTEGER is an Objs extension required by the SBOM domain. */
enum class SchemaType {
    OBJECT,
    ARRAY,
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    ENUM,
}

class SchemaDefinitionException(message: String) : IllegalArgumentException(message)

/** Strict normalization and structural validation for object-schema definitions. */
object SchemaNormalizer {
    fun normalizeStrict(input: Schema): Schema {
        if (input.type.isBlank()) invalid("type must not be blank")
        if (input.version.isBlank()) invalid("version must not be blank")
        val content = normalizeNode("$.contentSchema", input.contentSchema)
        if (content.type != SchemaType.OBJECT) {
            invalid("$.contentSchema.type must be OBJECT")
        }
        return input.copy(
            type = input.type.trim(),
            version = input.version.trim(),
            contentSchema = content,
            usage = input.usage,
            tags = CatalogMetadata.tags(input.tags),
            attributes = CatalogMetadata.attributes(input.attributes),
        )
    }

    private fun normalizeNode(
        path: String,
        node: SchemaNode,
        underArray: Boolean = false,
    ): SchemaNode {
        if (node.title.isBlank()) invalid("$path.title must not be blank")
        if (node.description.isBlank()) invalid("$path.description must not be blank")

        return when (node.type) {
            SchemaType.OBJECT -> {
                reject(path, node, items = true, values = true, format = true)
                val fields = node.fields ?: invalid("$path.fields is required for OBJECT")
                val names = mutableSetOf<String>()
                val normalizedFields = fields.mapIndexed { index, field ->
                    val fieldPath = "$path.fields[$index]"
                    val name = field.name.trim()
                    if (name.isBlank()) invalid("$fieldPath.name must not be blank")
                    if (!names.add(name)) invalid("$path has duplicate field name '$name'")
                    val nested = normalizeNode("$fieldPath.schema", field.schema, underArray)
                    validateFieldFlags(fieldPath, field, nested, underArray)
                    field.copy(
                        name = name,
                        schema = nested,
                        stereotype = field.stereotype
                            ?.map(String::trim)
                            ?.filter(String::isNotEmpty)
                            ?.distinct()
                            ?.takeIf { it.isNotEmpty() },
                        tags = CatalogMetadata.tags(field.tags),
                        attributes = CatalogMetadata.attributes(field.attributes),
                    )
                }
                node.copy(
                    title = node.title.trim(),
                    description = node.description.trim(),
                    fields = normalizedFields,
                )
            }

            SchemaType.ARRAY -> {
                reject(path, node, fields = true, values = true, format = true)
                val items = node.items ?: invalid("$path.items is required for ARRAY")
                node.copy(
                    title = node.title.trim(),
                    description = node.description.trim(),
                    items = normalizeNode("$path.items", items, underArray = true),
                )
            }

            SchemaType.ENUM -> {
                reject(path, node, fields = true, items = true, format = true)
                val values = node.values ?: invalid("$path.values is required for ENUM")
                if (values.isEmpty()) invalid("$path.values must not be empty")
                val seen = mutableSetOf<String>()
                val normalizedValues = values.mapIndexed { index, entry ->
                    val valuePath = "$path.values[$index]"
                    val value = entry.value.trim()
                    val description = entry.description.trim()
                    if (value.isBlank()) invalid("$valuePath.value must not be blank")
                    if (description.isBlank()) invalid("$valuePath.description must not be blank")
                    if (!seen.add(value)) invalid("$path has duplicate enum value '$value'")
                    val caption = entry.caption?.trim()?.takeIf { it.isNotEmpty() }
                    entry.copy(value = value, description = description, caption = caption)
                }
                node.copy(
                    title = node.title.trim(),
                    description = node.description.trim(),
                    values = normalizedValues,
                )
            }

            SchemaType.STRING -> {
                reject(path, node, fields = true, items = true, values = true)
                node.copy(
                    title = node.title.trim(),
                    description = node.description.trim(),
                    format = CatalogMetadata.optionalText(node.format),
                )
            }

            SchemaType.NUMBER,
            SchemaType.INTEGER,
            SchemaType.BOOLEAN,
            -> {
                reject(path, node, fields = true, items = true, values = true, format = true)
                node.copy(title = node.title.trim(), description = node.description.trim())
            }
        }
    }

    private fun validateFieldFlags(
        fieldPath: String,
        field: SchemaField,
        nested: SchemaNode,
        underArray: Boolean,
    ) {
        if (!field.identifier && !field.searchable) return
        if (underArray) {
            invalid("$fieldPath identifier/searchable is not allowed under ARRAY")
        }
        val scalar = nested.type in SCALAR_FLAG_TYPES
        if (field.identifier && !scalar) {
            invalid("$fieldPath.identifier is only allowed on scalar field schemas")
        }
        if (field.searchable && !scalar) {
            invalid("$fieldPath.searchable is only allowed on scalar field schemas")
        }
    }

    private val SCALAR_FLAG_TYPES = setOf(
        SchemaType.STRING,
        SchemaType.NUMBER,
        SchemaType.INTEGER,
        SchemaType.BOOLEAN,
        SchemaType.ENUM,
    )

    private fun reject(
        path: String,
        node: SchemaNode,
        fields: Boolean = false,
        items: Boolean = false,
        values: Boolean = false,
        format: Boolean = false,
    ) {
        if (fields && node.fields != null) invalid("$path.fields is only allowed for OBJECT")
        if (items && node.items != null) invalid("$path.items is only allowed for ARRAY")
        if (values && node.values != null) invalid("$path.values is only allowed for ENUM")
        if (format && node.format != null) invalid("$path.format is only allowed for STRING")
    }

    private fun invalid(message: String): Nothing = throw SchemaDefinitionException(message)
}

/** Deterministic JSON Schema 2020-12 projection of the authoritative Objs DSL. */
object JsonSchema {
    const val DIALECT: String = "https://json-schema.org/draft/2020-12/schema"

    fun from(definition: Schema): Map<String, Any?> {
        val normalized = SchemaNormalizer.normalizeStrict(definition)
        return linkedMapOf<String, Any?>(
            "\$schema" to DIALECT,
            "x-objs-type" to normalized.type,
            "x-objs-version" to normalized.version,
        ) + fromNode(normalized.contentSchema)
    }

    fun fromNode(schema: SchemaNode): Map<String, Any?> {
        val out = linkedMapOf<String, Any?>(
            "title" to schema.title,
            "description" to schema.description,
        )
        schema.default?.let { out["default"] = it }

        when (schema.type) {
            SchemaType.OBJECT -> {
                out["type"] = "object"
                val properties = linkedMapOf<String, Any?>()
                val required = mutableListOf<String>()
                for (field in schema.fields.orEmpty()) {
                    val projected = fromNode(field.schema).toMutableMap()
                    field.stereotype?.takeIf { it.isNotEmpty() }?.let {
                        projected["x-objs-stereotype"] = it
                    }
                    if (field.identifier) projected["x-objs-identifier"] = true
                    if (field.searchable) projected["x-objs-searchable"] = true
                    properties[field.name] = projected
                    if (field.required) required += field.name
                }
                out["properties"] = properties
                if (required.isNotEmpty()) out["required"] = required
                out["additionalProperties"] = true
            }

            SchemaType.ARRAY -> {
                out["type"] = "array"
                out["items"] = requireNotNull(schema.items).let(::fromNode)
            }

            SchemaType.STRING -> {
                out["type"] = "string"
                schema.format?.let { out["format"] = it }
            }

            SchemaType.NUMBER -> out["type"] = "number"
            SchemaType.INTEGER -> out["type"] = "integer"
            SchemaType.BOOLEAN -> out["type"] = "boolean"
            SchemaType.ENUM -> {
                out["type"] = "string"
                val values = schema.values.orEmpty()
                out["enum"] = values.map { it.value }
                out["x-objs-enumDescriptions"] = values.associate { it.value to it.description }
                val captions = values.mapNotNull { v -> v.caption?.let { v.value to it } }.toMap()
                if (captions.isNotEmpty()) out["x-objs-enumCaptions"] = captions
            }
        }
        return out
    }
}

/** Kotlin authoring helpers for the object-schema DSL. */
object SchemaDsl {
    fun obj(
        title: String,
        description: String,
        fields: List<SchemaField> = emptyList(),
    ) = SchemaNode(
        type = SchemaType.OBJECT,
        title = title,
        description = description,
        fields = fields,
    )

    fun array(title: String, description: String, items: SchemaNode) = SchemaNode(
        type = SchemaType.ARRAY,
        title = title,
        description = description,
        items = items,
    )

    fun string(title: String, description: String, format: String? = null, default: String? = null) =
        SchemaNode(
            type = SchemaType.STRING,
            title = title,
            description = description,
            format = format,
            default = default,
        )

    fun number(title: String, description: String, default: Number? = null) = SchemaNode(
        type = SchemaType.NUMBER,
        title = title,
        description = description,
        default = default,
    )

    fun integer(title: String, description: String, default: Long? = null) = SchemaNode(
        type = SchemaType.INTEGER,
        title = title,
        description = description,
        default = default,
    )

    fun boolean(title: String, description: String, default: Boolean? = null) = SchemaNode(
        type = SchemaType.BOOLEAN,
        title = title,
        description = description,
        default = default,
    )

    fun enum(
        title: String,
        description: String,
        values: List<EnumValue>,
        default: String? = null,
    ) = SchemaNode(
        type = SchemaType.ENUM,
        title = title,
        description = description,
        values = values,
        default = default,
    )

    fun field(
        name: String,
        schema: SchemaNode,
        required: Boolean = true,
        identifier: Boolean = false,
        searchable: Boolean = false,
        stereotype: List<String>? = null,
        tags: List<String> = emptyList(),
        attributes: Map<String, String> = emptyMap(),
    ) = SchemaField(name, schema, required, identifier, searchable, stereotype, tags, attributes)
}
