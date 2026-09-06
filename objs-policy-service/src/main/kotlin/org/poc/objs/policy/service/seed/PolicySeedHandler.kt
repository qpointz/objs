package org.poc.objs.policy.service.seed

import org.poc.objs.api.seed.ParsedSeedDocument
import org.poc.objs.api.seed.SeedDocumentHandler
import org.poc.objs.api.seed.SeedDocumentResult
import org.poc.objs.api.seed.SeedDocumentValidationException
import org.poc.objs.api.seed.SeedRawDocument
import org.poc.objs.api.seed.requireText
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicySeedKinds
import org.poc.objs.policy.api.PolicySeedModes
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.api.POLICY_SEED_APPLY_ORDER

data class PolicySeedPayload(
    val key: String,
    val name: String,
    val categoryKey: String,
    val engineKind: String,
    val body: String,
    val contentType: String?,
    val applicabilityKind: String?,
    val applicabilityBody: String?,
    val tags: List<String>,
    val annotations: Map<String, String>,
    val version: String,
    val description: String,
    val mode: String,
)

/**
 * `kind: Policy` (G-P35seed). MERGE/resolve on [org.poc.objs.policy.api.Policy.key]
 * (G-P36seed). `apply` saves a new revision under the same key (repository allocates the
 * serial); `replace` deletes all existing revisions for the key first. `categoryKey` resolves
 * to a [org.poc.objs.policy.api.Category.id] at apply time (dangling ref fails the whole
 * resource — G-P40seed). Body: inline `body` or `bodyRef` / `bodyFile` (G-P37seed).
 */
class PolicySeedHandler(
    private val policies: PolicyRepository,
    private val categories: CategoryRepository,
    private val bodyLoader: SeedBodyLoader,
) : SeedDocumentHandler {
    override val kind: String = PolicySeedKinds.POLICY
    override val applyOrder: Int = POLICY_SEED_APPLY_ORDER

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val raw = document.raw
        val index = document.index
        val key = requireSeedKey(raw, "key", index)
        val name = raw["name"]?.toString()?.trim()?.ifBlank { key } ?: key
        val categoryKey = requireText(raw, "categoryKey", index)
        val engineKind = requireText(raw, "engineKind", index)
        val body = resolveSeedBody(raw, index, bodyLoader)
        val tags = parseSeedStringList(raw["tags"], index, "tags")
        val annotations = parseSeedStringMap(raw["annotations"], index, "annotations")
        val version = raw["version"]?.toString()?.trim()?.ifBlank { "0.1" } ?: "0.1"
        val description = raw["description"]?.toString()?.trim() ?: ""
        val contentType = raw["contentType"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val applicabilityKind = raw["applicabilityKind"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val applicabilityBody = raw["applicabilityBody"]?.toString()?.takeIf { it.isNotBlank() }
        val mode = parseSeedMode(raw["mode"], index)
        return ParsedSeedDocument(
            document = document,
            identity = key,
            payload = PolicySeedPayload(
                key = key,
                name = name,
                categoryKey = categoryKey,
                engineKind = engineKind,
                body = body,
                contentType = contentType,
                applicabilityKind = applicabilityKind,
                applicabilityBody = applicabilityBody,
                tags = tags,
                annotations = annotations,
                version = version,
                description = description,
                mode = mode,
            ),
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val payload = parsed.payload as PolicySeedPayload
        val category = categories.findByKey(payload.categoryKey)
            ?: throw SeedDocumentValidationException(
                parsed.document.index,
                listOf(
                    ValidationIssue(
                        "SEED_DANGLING_REF",
                        "Unknown categoryKey '${payload.categoryKey}' for Policy '${payload.key}'",
                        path = "categoryKey",
                    ),
                ),
                "Policy '${payload.key}' references unknown category '${payload.categoryKey}'",
            )
        val write = PolicyWrite(
            key = payload.key,
            name = payload.name,
            engineKind = payload.engineKind,
            body = payload.body,
            categoryId = category.id,
            tags = payload.tags,
            contentType = payload.contentType,
            applicabilityKind = payload.applicabilityKind,
            applicabilityBody = payload.applicabilityBody,
            annotations = payload.annotations,
            version = payload.version,
            description = payload.description,
        )
        if (payload.mode == PolicySeedModes.REPLACE) {
            policies.findByKey(payload.key).forEach { policies.delete(it.id) }
        }
        policies.save(write)
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }
}
