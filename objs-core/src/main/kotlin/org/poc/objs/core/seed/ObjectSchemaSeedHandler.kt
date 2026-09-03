package org.poc.objs.core.seed

import org.poc.objs.api.seed.*

import org.poc.objs.api.domain.Schema
import org.poc.objs.api.domain.SchemaCatalog
import org.poc.objs.api.domain.SchemaDefinitionException
import org.poc.objs.api.domain.SchemaNode
import org.poc.objs.api.domain.SchemaNormalizer
import org.poc.objs.api.domain.SchemaUsage
import org.poc.objs.core.typed.DefaultPayloadMapper

class ObjectSchemaSeedHandler(
    private val schemas: SchemaCatalog,
) : SeedDocumentHandler {
    override val kind: String = SEED_KIND_OBJECT_SCHEMA
    override val applyOrder: Int = 0

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val type = requireText(document.raw, "type", document.index)
        val version = requireText(document.raw, "version", document.index)
        val usage = parseUsage(document.raw["usage"], document.index)
        val contentRaw = document.raw["contentSchema"]
            ?: throw SeedDocumentParseException(
                document.index,
                "ObjectSchema requires contentSchema",
            )
        @Suppress("UNCHECKED_CAST")
        val contentMap = contentRaw as? Map<String, Any?>
            ?: throw SeedDocumentParseException(
                document.index,
                "contentSchema must be an object",
            )
        val contentSchema = try {
            DefaultPayloadMapper.fromMap(contentMap, SchemaNode::class.java)
        } catch (ex: Exception) {
            throw SeedDocumentParseException(
                document.index,
                "Failed to parse contentSchema: ${ex.message}",
                ex,
            )
        }
        val schema = try {
            SchemaNormalizer.normalizeStrict(
                Schema(
                    type = type,
                    version = version,
                    contentSchema = contentSchema,
                    usage = usage,
                    tags = parseSeedTags(document.raw["tags"], document.index),
                    attributes = parseSeedAttributes(document.raw["attributes"], document.index),
                ),
            )
        } catch (ex: SchemaDefinitionException) {
            throw SeedDocumentParseException(document.index, ex.message ?: "Invalid schema", ex)
        }
        return ParsedSeedDocument(
            document = document,
            identity = "$type@$version",
            payload = schema,
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val schema = parsed.payload as Schema
        schemas.register(schema)
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }

    fun serialize(schema: Schema): Map<String, Any?> {
        val document = linkedMapOf<String, Any?>(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to kind,
            "type" to schema.type,
            "version" to schema.version,
        )
        if (schema.usage != SchemaUsage.ENTITY) {
            document["usage"] = schema.usage.name
        }
        emitSeedTags(document, schema.tags)
        emitSeedAttributes(document, schema.attributes)
        document["contentSchema"] = DefaultPayloadMapper.toMap(schema.contentSchema)
        return document
    }

    private fun parseUsage(raw: Any?, index: Int): SchemaUsage {
        if (raw == null) return SchemaUsage.ENTITY
        if (raw is Collection<*> || raw is Array<*>) {
            throw SeedDocumentParseException(index, "usage must be a single value (ENTITY or EDGE_PROPERTIES), not a list")
        }
        return try {
            SchemaUsage.valueOf(raw.toString().trim())
        } catch (ex: IllegalArgumentException) {
            throw SeedDocumentParseException(index, "Unknown schema usage: $raw", ex)
        }
    }
}
