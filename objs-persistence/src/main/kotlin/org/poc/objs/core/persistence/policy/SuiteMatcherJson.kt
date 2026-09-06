package org.poc.objs.core.persistence.policy

import org.poc.objs.policy.api.MetadataMatcher
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteMatcher
import org.poc.objs.policy.api.SuiteMatcherMode
import java.util.UUID

/**
 * [SuiteMatcher] is a sealed class family (not a plain data class), so [PolicySuiteFolderRecord.matchers]
 * stores a JSON array of `{kind, ...}` maps instead of a further-normalized table/column set.
 */
internal fun SuiteMatcher.toRecordMap(): MutableMap<String, Any?> = when (this) {
    is StaticListMatcher -> mutableMapOf(
        "kind" to "STATIC_LIST",
        "mode" to mode.name,
        "skipMissing" to skipMissing,
        "entries" to entries.map {
            mutableMapOf<String, Any?>("policyId" to it.policyId.toString(), "version" to it.version)
        },
    )
    is MetadataMatcher -> mutableMapOf(
        "kind" to "METADATA",
        "mode" to mode.name,
        "skipMissing" to skipMissing,
        "categoryKey" to categoryKey,
        "tags" to tags,
        "annotations" to annotations,
    )
}

internal fun matcherFromRecordMap(raw: Map<*, *>): SuiteMatcher {
    val kind = raw["kind"] as? String ?: error("Suite matcher JSON missing 'kind'")
    val mode = (raw["mode"] as? String)?.let { SuiteMatcherMode.valueOf(it) } ?: SuiteMatcherMode.INCLUDE
    val skipMissing = raw["skipMissing"] as? Boolean ?: false
    return when (kind) {
        "STATIC_LIST" -> {
            val entries = (raw["entries"] as? List<*>).orEmpty().mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val policyId = (map["policyId"] as? String)?.let(UUID::fromString) ?: return@mapNotNull null
                StaticPolicyEntry(policyId = policyId, version = map["version"] as? String)
            }
            StaticListMatcher(mode = mode, skipMissing = skipMissing, entries = entries)
        }
        "METADATA" -> MetadataMatcher(
            mode = mode,
            skipMissing = skipMissing,
            categoryKey = raw["categoryKey"] as? String,
            tags = (raw["tags"] as? List<*>).orEmpty().filterIsInstance<String>(),
            annotations = (raw["annotations"] as? Map<*, *>).orEmpty()
                .entries.associate { (k, v) -> k.toString() to v.toString() },
        )
        else -> error("Unknown suite matcher kind: $kind")
    }
}
