package org.poc.objs.policy.service.seed

import org.poc.objs.api.seed.SEED_API_VERSION_V1
import org.poc.objs.api.seed.SeedYaml
import org.poc.objs.policy.api.Category
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.MetadataMatcher
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicySeedKinds
import org.poc.objs.policy.api.PolicySeedModes
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteMatcher
import org.poc.objs.policy.api.SuiteRepository

/**
 * Emits **REPLACE** multi-doc YAML for the full live policy catalog (G-P39seed / WI-005):
 * all categories, latest policy revision per key (inline body), all suites.
 * Document order: categories → policies → suites (helpful default; apply order remains user-owned).
 */
class PolicyCatalogSeedExporter(
    private val categories: CategoryRepository,
    private val policies: PolicyRepository,
    private val suites: SuiteRepository,
) {
    fun exportReplaceYaml(): String {
        val documents = mutableListOf<Map<String, Any?>>()
        categories.list().sortedBy { it.key }.forEach { documents += serializeCategory(it) }
        latestPoliciesByKey()
            .sortedBy { it.key }
            .forEach { documents += serializePolicy(it) }
        suites.list().sortedBy { it.key }.forEach { documents += serializeSuite(it) }
        return SeedYaml.writeDocuments(documents)
    }

    private fun latestPoliciesByKey(): List<Policy> =
        policies.list()
            .groupBy { it.key }
            .values
            .map { revs -> revs.maxBy { it.serial } }

    private fun serializeCategory(c: Category): Map<String, Any?> =
        linkedMapOf(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to PolicySeedKinds.CATEGORY,
            "mode" to PolicySeedModes.REPLACE,
            "key" to c.key,
            "name" to c.name,
        )

    private fun serializePolicy(p: Policy): Map<String, Any?> {
        val categoryKey = categories.findById(p.categoryId)?.key
            ?: error("Policy ${p.key} references missing category ${p.categoryId}")
        val doc = linkedMapOf<String, Any?>(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to PolicySeedKinds.POLICY,
            "mode" to PolicySeedModes.REPLACE,
            "key" to p.key,
            "name" to p.name,
            "engineKind" to p.engineKind,
            "categoryKey" to categoryKey,
            "tags" to p.tags,
            "version" to p.version,
            "body" to p.body,
        )
        if (p.description.isNotBlank()) doc["description"] = p.description
        if (p.contentType != null) doc["contentType"] = p.contentType
        if (p.applicabilityKind != null) doc["applicabilityKind"] = p.applicabilityKind
        if (p.applicabilityBody != null) doc["applicabilityBody"] = p.applicabilityBody
        if (p.annotations.isNotEmpty()) doc["annotations"] = p.annotations
        return doc
    }

    private fun serializeSuite(s: PolicySuite): Map<String, Any?> {
        val foldersById = s.folders.associateBy { it.id }
        return linkedMapOf(
            "apiVersion" to SEED_API_VERSION_V1,
            "kind" to PolicySeedKinds.POLICY_SUITE,
            "mode" to PolicySeedModes.REPLACE,
            "key" to s.key,
            "name" to s.name,
            "rollUpStrategyKind" to s.rollUpStrategyKind,
            "executionStrategyKind" to s.executionStrategyKind,
            "folders" to s.folders.sortedBy { it.sortOrder }.map { serializeFolder(it, foldersById) },
            "tags" to s.tags,
            "annotations" to s.annotations,
        )
    }

    private fun serializeFolder(
        f: SuiteFolder,
        foldersById: Map<java.util.UUID, SuiteFolder>,
    ): Map<String, Any?> {
        val doc = linkedMapOf<String, Any?>(
            "key" to f.key,
            "name" to f.name,
            "sortOrder" to f.sortOrder,
            "participation" to f.participation.name,
            "rollUpMode" to f.rollUpMode,
            "matchers" to f.matchers.map { serializeMatcher(it) },
            "tags" to f.tags,
            "annotations" to f.annotations,
        )
        f.parentId?.let { pid ->
            foldersById[pid]?.let { doc["parentKey"] = it.key }
        }
        f.severityConfig?.let { doc["severityConfig"] = it }
        return doc
    }

    private fun serializeMatcher(m: SuiteMatcher): Map<String, Any?> =
        when (m) {
            is StaticListMatcher -> linkedMapOf(
                "kind" to "STATIC_LIST",
                "mode" to m.mode.name,
                "skipMissing" to m.skipMissing,
                "entries" to m.entries.map { e ->
                    val pol = policies.findById(e.policyId)
                    linkedMapOf<String, Any?>(
                        "policyKey" to (pol?.key ?: e.policyId.toString()),
                        "version" to e.version,
                    ).filterValues { it != null }
                },
            )
            is MetadataMatcher -> {
                val doc = linkedMapOf<String, Any?>(
                    "kind" to "METADATA",
                    "mode" to m.mode.name,
                    "skipMissing" to m.skipMissing,
                )
                m.categoryKey?.let { doc["categoryKey"] = it }
                if (m.tags.isNotEmpty()) doc["tags"] = m.tags
                if (m.annotations.isNotEmpty()) doc["annotations"] = m.annotations
                doc
            }
        }
}
