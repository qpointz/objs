package org.poc.objs.policy.service.seed

import org.poc.objs.api.seed.ParsedSeedDocument
import org.poc.objs.api.seed.SeedDocumentHandler
import org.poc.objs.api.seed.SeedDocumentResult
import org.poc.objs.api.seed.SeedRawDocument
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicySeedKinds
import org.poc.objs.policy.api.SuiteRepository
import org.poc.objs.policy.api.POLICY_SEED_APPLY_ORDER
import java.util.UUID

/**
 * `kind: DropCategory` (G-P35seed). Cascades: wipes policies still referencing the resolved
 * category, then drops the category. No-op (skipped) when the key is unknown.
 */
class DropCategorySeedHandler(
    private val categories: CategoryRepository,
    private val policies: PolicyRepository,
) : SeedDocumentHandler {
    override val kind: String = PolicySeedKinds.DROP_CATEGORY
    override val applyOrder: Int = POLICY_SEED_APPLY_ORDER

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val key = requireSeedKey(document.raw, "key", document.index)
        return ParsedSeedDocument(document = document, identity = key, payload = key)
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val key = parsed.payload as String
        val existing = categories.findByKey(key)
        if (existing == null) {
            return SeedDocumentResult(
                index = parsed.document.index,
                kind = kind,
                apiVersion = parsed.document.apiVersion,
                identity = key,
                skipped = true,
            )
        }
        wipePoliciesForCategory(existing.id)
        categories.delete(existing.id)
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = key,
            applied = true,
        )
    }

    private fun wipePoliciesForCategory(categoryId: UUID) {
        policies.list()
            .filter { it.categoryId == categoryId }
            .map { it.id }
            .forEach { policies.delete(it) }
    }
}

/** `kind: DropPolicy` (G-P35seed). Drops **all** revisions for the key; no-op when unknown. */
class DropPolicySeedHandler(
    private val policies: PolicyRepository,
) : SeedDocumentHandler {
    override val kind: String = PolicySeedKinds.DROP_POLICY
    override val applyOrder: Int = POLICY_SEED_APPLY_ORDER

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val key = requireSeedKey(document.raw, "key", document.index)
        return ParsedSeedDocument(document = document, identity = key, payload = key)
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val key = parsed.payload as String
        val revisions = policies.findByKey(key)
        revisions.forEach { policies.delete(it.id) }
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = key,
            applied = revisions.isNotEmpty(),
            skipped = revisions.isEmpty(),
        )
    }
}

/** `kind: DropPolicySuite` (G-P35seed). Drops the suite tree only; no-op when unknown. */
class DropPolicySuiteSeedHandler(
    private val suites: SuiteRepository,
) : SeedDocumentHandler {
    override val kind: String = PolicySeedKinds.DROP_POLICY_SUITE
    override val applyOrder: Int = POLICY_SEED_APPLY_ORDER

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val key = requireSeedKey(document.raw, "key", document.index)
        return ParsedSeedDocument(document = document, identity = key, payload = key)
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val key = parsed.payload as String
        val existing = suites.findByKey(key)
        if (existing == null) {
            return SeedDocumentResult(
                index = parsed.document.index,
                kind = kind,
                apiVersion = parsed.document.apiVersion,
                identity = key,
                skipped = true,
            )
        }
        suites.delete(existing.id)
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = key,
            applied = true,
        )
    }
}
