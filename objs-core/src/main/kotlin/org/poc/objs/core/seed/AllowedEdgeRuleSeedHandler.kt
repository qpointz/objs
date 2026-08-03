package org.poc.objs.core.seed

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMEdgeCardinality
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.springframework.stereotype.Component

@Component
class AllowedEdgeRuleSeedHandler(
    private val edgeRules: BoMAllowedEdgeCatalog,
) : SeedDocumentHandler {
    override val kind: String = SEED_KIND_ALLOWED_EDGE_RULE

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val sourceType = requireText(document.raw, "sourceType", document.index)
        val role = requireText(document.raw, "role", document.index)
        val targetType = requireText(document.raw, "targetType", document.index)
        val policy = when (val raw = document.raw["propertiesPolicy"]?.toString()) {
            null -> BoMPropertiesPolicy.NONE
            else -> try {
                BoMPropertiesPolicy.valueOf(raw)
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
        if (policy == BoMPropertiesPolicy.SCHEMA) {
            if (propertiesSchemaType == null || propertiesSchemaVersion == null) {
                throw SeedDocumentParseException(
                    document.index,
                    "SCHEMA propertiesPolicy requires propertiesSchemaType and propertiesSchemaVersion",
                )
            }
        }
        val cardinality = when (val raw = document.raw["cardinality"]?.toString()?.trim()) {
            null, "" -> BoMEdgeCardinality.UNSPECIFIED
            else -> try {
                BoMEdgeCardinality.fromWire(raw)
            } catch (_: IllegalArgumentException) {
                throw SeedDocumentParseException(
                    document.index,
                    "Unknown cardinality: $raw",
                )
            }
        }
        val rule = BoMAllowedEdgeRule(
            sourceType = sourceType,
            role = role,
            targetType = targetType,
            propertiesPolicy = policy,
            emptyPropertiesAllowed = emptyAllowed,
            propertiesSchemaType = propertiesSchemaType,
            propertiesSchemaVersion = propertiesSchemaVersion,
            cardinality = cardinality,
        )
        return ParsedSeedDocument(
            document = document,
            identity = "($sourceType,$role,$targetType)",
            payload = rule,
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val rule = parsed.payload as BoMAllowedEdgeRule
        edgeRules.register(rule)
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }

    fun serialize(rule: BoMAllowedEdgeRule): Map<String, Any?> {
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
        return document
    }
}
