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

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val type = requireText(document.raw, "type", document.index)
        val version = requireText(document.raw, "version", document.index)
        val usages = parseUsages(document.raw["usages"], document.index)
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
                    usages = usages,
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
        if (schema.usages != setOf(BoMSchemaUsage.ENTITY)) {
            document["usages"] = schema.usages.map { it.name }.sorted()
        }
        document["contentSchema"] = PayloadMapper.toMap(schema.contentSchema)
        return document
    }

    private fun parseUsages(raw: Any?, index: Int): Set<BoMSchemaUsage> {
        if (raw == null) return setOf(BoMSchemaUsage.ENTITY)
        val values = when (raw) {
            is Collection<*> -> raw.map { it.toString() }
            is Array<*> -> raw.map { it.toString() }
            else -> throw SeedDocumentParseException(index, "usages must be a list")
        }
        if (values.isEmpty()) {
            throw SeedDocumentParseException(index, "usages must not be empty")
        }
        return try {
            values.map { BoMSchemaUsage.valueOf(it) }.toSet()
        } catch (ex: IllegalArgumentException) {
            throw SeedDocumentParseException(index, "Unknown schema usage in usages", ex)
        }
    }
}
