package org.poc.objs.policy.core

import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.MetadataMatcher
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyQuery
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteEffectiveSet
import org.poc.objs.policy.api.SuiteEvaluateScope
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteMatcher
import org.poc.objs.policy.api.SuiteMatcherMode
import org.poc.objs.policy.api.SuiteSelectionException
import org.poc.objs.policy.api.SuiteVersionSelectors
import java.util.UUID

/**
 * Expands suite folder matchers into effective policy sets (G-P27s).
 * Children first (name order), own matchers last; [policyId] uniqueness after each step.
 */
class SuiteMatcherExpander(
    private val policies: PolicyRepository,
    private val categories: CategoryRepository,
) {
    fun resolveSelection(suite: PolicySuite, scope: SuiteEvaluateScope): SuiteEffectiveSet {
        val byId = suite.folders.associateBy { it.id }
        return when (scope) {
            is SuiteEvaluateScope.Full -> {
                val root = suite.folders.singleOrNull { it.parentId == null }
                    ?: return SuiteEffectiveSet(emptyList())
                effective(root, byId, throwOnMissing = true)
            }
            is SuiteEvaluateScope.Subfolder -> {
                val folder = byId[scope.folderId]
                    ?: throw IllegalArgumentException("Unknown folderId: ${scope.folderId}")
                effective(folder, byId, throwOnMissing = true)
            }
            is SuiteEvaluateScope.SubfolderSet -> {
                val acc = linkedMapOf<UUID, Policy>()
                val placement = linkedMapOf<UUID, UUID>()
                val missing = mutableListOf<StaticPolicyEntry>()
                for (fid in scope.folderIds) {
                    val folder = byId[fid]
                        ?: throw IllegalArgumentException("Unknown folderId: $fid")
                    val part = effective(folder, byId, throwOnMissing = false)
                    missing += part.missing
                    mergeUnique(acc, placement, part.policies, folder.id)
                }
                if (missing.isNotEmpty()) {
                    throw SuiteSelectionException(
                        "Unresolved STATIC_LIST entries: ${missing.map { it.policyId }}",
                        missing,
                    )
                }
                SuiteEffectiveSet(acc.values.toList(), placement, emptyList())
            }
            is SuiteEvaluateScope.PolicySubset -> {
                val wanted = scope.policyIds.toSet()
                val full = resolveSelection(suite, SuiteEvaluateScope.Full)
                val filtered = full.policies.filter { it.id in wanted }
                SuiteEffectiveSet(
                    policies = filtered,
                    placementByPolicyId = full.placementByPolicyId.filterKeys { it in wanted },
                )
            }
        }
    }

    fun effectivePolicies(suite: PolicySuite, folderId: UUID): SuiteEffectiveSet {
        val byId = suite.folders.associateBy { it.id }
        val folder = byId[folderId]
            ?: throw IllegalArgumentException("Unknown folderId: $folderId")
        return effective(folder, byId, throwOnMissing = true)
    }

    private fun effective(
        folder: SuiteFolder,
        byId: Map<UUID, SuiteFolder>,
        throwOnMissing: Boolean,
    ): SuiteEffectiveSet {
        if (folder.participation == SuiteFolderParticipation.DISABLED) {
            return SuiteEffectiveSet(emptyList())
        }
        val acc = linkedMapOf<UUID, Policy>()
        val placement = linkedMapOf<UUID, UUID>()
        val missing = mutableListOf<StaticPolicyEntry>()

        val children = byId.values
            .filter { it.parentId == folder.id }
            .sortedBy { it.name.lowercase() }
        for (child in children) {
            val childSet = effective(child, byId, throwOnMissing = false)
            missing += childSet.missing
            mergeUnique(acc, placement, childSet.policies, child.id)
        }

        for (matcher in folder.matchers) {
            val hit = resolveMatcher(matcher)
            missing += hit.missing
            when (matcher.mode) {
                SuiteMatcherMode.INCLUDE -> mergeUnique(acc, placement, hit.policies, folder.id)
                SuiteMatcherMode.EXCLUDE -> {
                    for (p in hit.policies) {
                        acc.remove(p.id)
                        placement.remove(p.id)
                    }
                }
            }
        }

        if (throwOnMissing && missing.isNotEmpty()) {
            throw SuiteSelectionException(
                "Unresolved STATIC_LIST entries: ${missing.map { it.policyId }}",
                missing,
            )
        }
        return SuiteEffectiveSet(acc.values.toList(), placement.toMap(), missing.toList())
    }

    private fun resolveMatcher(matcher: SuiteMatcher): SuiteEffectiveSet =
        when (matcher) {
            is StaticListMatcher -> resolveStatic(matcher)
            is MetadataMatcher -> resolveMetadata(matcher)
        }

    private fun resolveStatic(matcher: StaticListMatcher): SuiteEffectiveSet {
        val found = mutableListOf<Policy>()
        val missing = mutableListOf<StaticPolicyEntry>()
        val seen = linkedSetOf<UUID>()
        for (entry in matcher.entries) {
            val policy = resolveEntry(entry)
            if (policy == null) {
                if (!matcher.skipMissing) missing += entry
                continue
            }
            if (!seen.add(policy.id)) {
                throw SuiteSelectionException(
                    "Duplicate policyId ${policy.id} within STATIC_LIST matcher step",
                )
            }
            found += policy
        }
        return SuiteEffectiveSet(found, missing = missing)
    }

    private fun resolveEntry(entry: StaticPolicyEntry): Policy? {
        val policy = policies.findById(entry.policyId) ?: return null
        val parsed = SuiteVersionSelectors.parse(entry.version)
        return if (SuiteVersionSelectors.matches(policy, parsed)) policy else null
    }

    private fun resolveMetadata(matcher: MetadataMatcher): SuiteEffectiveSet {
        val categoryId = matcher.categorySlug?.trim()?.takeIf { it.isNotEmpty() }?.let { slug ->
            categories.list().find { it.slug == slug }?.id
                ?: return SuiteEffectiveSet(emptyList())
        }
        val matched = policies.query(
            PolicyQuery(
                categoryId = categoryId,
                tags = matcher.tags,
                annotations = matcher.annotations,
            ),
        )
        // Latest per name among matches (catalog may have multiple serials / ids per name).
        val latestByName = linkedMapOf<String, Policy>()
        for (p in matched.sortedBy { it.serial }) {
            latestByName[p.name] = p
        }
        val list = latestByName.values.toList()
        val uniq = linkedMapOf<UUID, Policy>()
        for (p in list) {
            if (uniq.put(p.id, p) != null) {
                throw SuiteSelectionException("Duplicate policyId ${p.id} within METADATA matcher step")
            }
        }
        return SuiteEffectiveSet(uniq.values.toList())
    }

    private fun mergeUnique(
        acc: LinkedHashMap<UUID, Policy>,
        placement: LinkedHashMap<UUID, UUID>,
        incoming: List<Policy>,
        folderId: UUID,
    ) {
        for (p in incoming) {
            val prev = acc[p.id]
            if (prev != null && prev.serial != p.serial) {
                throw SuiteSelectionException(
                    "policyId uniqueness violated for ${p.id}: serial ${prev.serial} vs ${p.serial}",
                )
            }
            acc[p.id] = p
            placement[p.id] = folderId
        }
    }
}
