package org.poc.objs.policy.service.seed

import org.poc.objs.api.seed.ParsedSeedDocument
import org.poc.objs.api.seed.SeedDocumentHandler
import org.poc.objs.api.seed.SeedDocumentResult
import org.poc.objs.api.seed.SeedRawDocument
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicySeedKinds
import org.poc.objs.policy.api.PolicySeedModes
import org.poc.objs.policy.api.POLICY_SEED_APPLY_ORDER

data class CategorySeedPayload(
    val write: CategoryWrite,
    val mode: String,
)

/**
 * `kind: Category` (G-P35seed). MERGE/resolve on [org.poc.objs.policy.api.Category.key]
 * (G-P36seed). `apply` upserts and keeps existing policies; `replace` wipes policies still
 * referencing the resolved category id, then upserts the category.
 */
class CategorySeedHandler(
    private val categories: CategoryRepository,
    private val policies: PolicyRepository,
) : SeedDocumentHandler {
    override val kind: String = PolicySeedKinds.CATEGORY
    override val applyOrder: Int = POLICY_SEED_APPLY_ORDER

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val key = requireSeedKey(document.raw, "key", document.index)
        val name = document.raw["name"]?.toString()?.trim()?.ifBlank { key } ?: key
        val mode = parseSeedMode(document.raw["mode"], document.index)
        return ParsedSeedDocument(
            document = document,
            identity = key,
            payload = CategorySeedPayload(write = CategoryWrite(name = name, key = key), mode = mode),
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val payload = parsed.payload as CategorySeedPayload
        val existing = categories.findByKey(payload.write.key)
        if (payload.mode == PolicySeedModes.REPLACE && existing != null) {
            wipePoliciesForCategory(existing.id)
        }
        if (existing != null) {
            categories.update(existing.id, payload.write)
        } else {
            categories.save(payload.write)
        }
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }

    private fun wipePoliciesForCategory(categoryId: java.util.UUID) {
        policies.list()
            .filter { it.categoryId == categoryId }
            .map { it.id }
            .forEach { policies.delete(it) }
    }
}