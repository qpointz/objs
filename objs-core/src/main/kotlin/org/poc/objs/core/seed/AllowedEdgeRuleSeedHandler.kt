package org.poc.objs.core.seed

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.springframework.stereotype.Component

@Component
class AllowedEdgeRuleSeedHandler(
    private val edgeRules: BoMAllowedEdgeCatalog,
) : SeedDocumentHandler {
    override val kind: String = SEED_KIND_ALLOWED_EDGE_RULE

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val sourceType = requireText(document.metadata, "sourceType", document.index)
        val role = requireText(document.metadata, "role", document.index)
        val targetType = requireText(document.metadata, "targetType", document.index)
        val policy = when (val raw = document.spec["propertiesPolicy"]?.toString()) {
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
        val emptyAllowed = when (val raw = document.spec["emptyPropertiesAllowed"]) {
            null -> true
            is Boolean -> raw
            else -> throw SeedDocumentParseException(
                document.index,
                "emptyPropertiesAllowed must be a boolean",
            )
        }
        val propertiesSchemaType = document.spec["propertiesSchemaType"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val propertiesSchemaVersion =
            document.spec["propertiesSchemaVersion"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        if (policy == BoMPropertiesPolicy.SCHEMA) {
            if (propertiesSchemaType == null || propertiesSchemaVersion == null) {
                throw SeedDocumentParseException(
                    document.index,
                    "SCHEMA propertiesPolicy requires propertiesSchemaType and propertiesSchemaVersion",
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
        val spec = linkedMapOf<String, Any?>(
            "propertiesPolicy" to rule.propertiesPolicy.name,
            "emptyPropertiesAllowed" to rule.emptyPropertiesAllowed,
        )
        if (rule.propertiesSchemaType != null) {
            spec["propertiesSchemaType"] = rule.propertiesSchemaType
        }
        if (rule.propertiesSchemaVersion != null) {
            spec["propertiesSchemaVersion"] = rule.propertiesSchemaVersion
        }
        return linkedMapOf(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to kind,
            "metadata" to linkedMapOf(
                "sourceType" to rule.sourceType,
                "role" to rule.role,
                "targetType" to rule.targetType,
            ),
            "spec" to spec,
        )
    }
}
