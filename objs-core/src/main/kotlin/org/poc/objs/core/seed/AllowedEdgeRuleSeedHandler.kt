package org.poc.objs.core.seed

import org.poc.objs.core.domain.CatalogMetadata
import org.poc.objs.core.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.EdgeCardinality
import org.poc.objs.api.domain.PropertiesPolicy
import org.springframework.stereotype.Component

@Component
class AllowedEdgeRuleSeedHandler(
    private val edgeRules: AllowedEdgeCatalog,
) : SeedDocumentHandler {
    override val kind: String = SEED_KIND_ALLOWED_EDGE_RULE
    override val applyOrder: Int = 10

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val sourceType = requireText(document.raw, "sourceType", document.index)
        val role = requireText(document.raw, "role", document.index)
        val targetType = requireText(document.raw, "targetType", document.index)
        val policy = when (val raw = document.raw["propertiesPolicy"]?.toString()) {
            null -> PropertiesPolicy.NONE
            else -> try {
                PropertiesPolicy.valueOf(raw)
            } catch (_: IllegalArgumentException) {
                throw SeedDocumentParseException(
                    document.index,
                    "Unknown propertiesPolicy: $raw",
                )
            }
        }
        val emptyAllowed = when (val raw = document.raw["emptyPropertiesAllowed"]) {
            null -> true
            is Boolean -> raw
            else -> throw SeedDocumentParseException(
                document.index,
                "emptyPropertiesAllowed must be a boolean",
            )
        }
        val propertiesSchemaType = document.raw["propertiesSchemaType"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val propertiesSchemaVersion =
            document.raw["propertiesSchemaVersion"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        if (policy == PropertiesPolicy.SCHEMA) {
            if (propertiesSchemaType == null || propertiesSchemaVersion == null) {
                throw SeedDocumentParseException(
                    document.index,
                    "SCHEMA propertiesPolicy requires propertiesSchemaType and propertiesSchemaVersion",
                )
            }
        }
        val cardinality = when (val raw = document.raw["cardinality"]?.toString()?.trim()) {
            null, "" -> EdgeCardinality.UNSPECIFIED
            else -> try {
                EdgeCardinality.fromWire(raw)
            } catch (_: IllegalArgumentException) {
                throw SeedDocumentParseException(
                    document.index,
                    "Unknown cardinality: $raw",
                )
            }
        }
        val rule = AllowedEdgeRule(
            sourceType = sourceType,
            role = role,
            targetType = targetType,
            propertiesPolicy = policy,
            emptyPropertiesAllowed = emptyAllowed,
            propertiesSchemaType = propertiesSchemaType,
            propertiesSchemaVersion = propertiesSchemaVersion,
            cardinality = cardinality,
            description = CatalogMetadata.optionalText(document.raw["description"]?.toString()),
            sourceVerb = CatalogMetadata.optionalText(document.raw["sourceVerb"]?.toString()),
            targetVerb = CatalogMetadata.optionalText(document.raw["targetVerb"]?.toString()),
            tags = parseSeedTags(document.raw["tags"], document.index),
            attributes = parseSeedAttributes(document.raw["attributes"], document.index),
        )
        return ParsedSeedDocument(
            document = document,
            identity = "($sourceType,$role,$targetType)",
            payload = rule,
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val rule = parsed.payload as AllowedEdgeRule
        edgeRules.register(rule)
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }

    fun serialize(rule: AllowedEdgeRule): Map<String, Any?> {
        val document = linkedMapOf<String, Any?>(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to kind,
            "sourceType" to rule.sourceType,
            "role" to rule.role,
            "targetType" to rule.targetType,
            "propertiesPolicy" to rule.propertiesPolicy.name,
            "emptyPropertiesAllowed" to rule.emptyPropertiesAllowed,
            "cardinality" to rule.cardinality.wire,
        )
        if (rule.propertiesSchemaType != null) {
            document["propertiesSchemaType"] = rule.propertiesSchemaType
        }
        if (rule.propertiesSchemaVersion != null) {
            document["propertiesSchemaVersion"] = rule.propertiesSchemaVersion
        }
        emitSeedText(document, "description", rule.description)
        emitSeedText(document, "sourceVerb", rule.sourceVerb)
        emitSeedText(document, "targetVerb", rule.targetVerb)
        emitSeedTags(document, rule.tags)
        emitSeedAttributes(document, rule.attributes)
        return document
    }
}
