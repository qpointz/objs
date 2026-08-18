package org.poc.objs.core.seed

import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDefinitionException
import org.poc.objs.core.domain.BoMSchemaNode
import org.poc.objs.core.domain.BoMSchemaNormalizer
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.typed.PayloadMapper
import org.springframework.stereotype.Component

@Component
class ObjectSchemaSeedHandler(
    private val schemas: BoMSchemaCatalog,
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
            PayloadMapper.fromMap(contentMap, BoMSchemaNode::class.java)
        } catch (ex: Exception) {
            throw SeedDocumentParseException(
                document.index,
                "Failed to parse contentSchema: ${ex.message}",
                ex,
            )
        }
        val schema = try {
            BoMSchemaNormalizer.normalizeStrict(
                BoMSchema(
                    type = type,
                    version = version,
                    contentSchema = contentSchema,
                    usage = usage,
                    tags = parseSeedTags(document.raw["tags"], document.index),
                    attributes = parseSeedAttributes(document.raw["attributes"], document.index),
                ),
            )
        } catch (ex: BoMSchemaDefinitionException) {
            throw SeedDocumentParseException(document.index, ex.message ?: "Invalid schema", ex)
        }
        return ParsedSeedDocument(
            document = document,
            identity = "$type@$version",
            payload = schema,
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val schema = parsed.payload as BoMSchema
        schemas.register(schema)
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }

    fun serialize(schema: BoMSchema): Map<String, Any?> {
        val document = linkedMapOf<String, Any?>(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to kind,
            "type" to schema.type,
            "version" to schema.version,
        )
        if (schema.usage != BoMSchemaUsage.ENTITY) {
            document["usage"] = schema.usage.name
        }
        emitSeedTags(document, schema.tags)
        emitSeedAttributes(document, schema.attributes)
        document["contentSchema"] = PayloadMapper.toMap(schema.contentSchema)
        return document
    }

    private fun parseUsage(raw: Any?, index: Int): BoMSchemaUsage {
        if (raw == null) return BoMSchemaUsage.ENTITY
        if (raw is Collection<*> || raw is Array<*>) {
            throw SeedDocumentParseException(index, "usage must be a single value (ENTITY or EDGE_PROPERTIES), not a list")
        }
        return try {
            BoMSchemaUsage.valueOf(raw.toString().trim())
        } catch (ex: IllegalArgumentException) {
            throw SeedDocumentParseException(index, "Unknown schema usage: $raw", ex)
        }
    }
}
