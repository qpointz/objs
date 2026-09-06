package org.poc.objs.policy.api

/**
 * Policy catalog seed kinds (G-P35seed, C-28 WI-003). Same seed envelope as graph/object seeds
 * (`apiVersion: objs.poc.org/v1`, [org.poc.objs.api.seed.SeedDocumentHandler]). Independent
 * kinds — no forced category → policy → suite apply pipeline (G-P39seed): file + document
 * order only.
 */
object PolicySeedKinds {
    const val CATEGORY: String = "Category"
    const val POLICY: String = "Policy"
    const val POLICY_SUITE: String = "PolicySuite"
    const val DROP_CATEGORY: String = "DropCategory"
    const val DROP_POLICY: String = "DropPolicy"
    const val DROP_POLICY_SUITE: String = "DropPolicySuite"

    /** All policy catalog seed kinds (content + drop). */
    val ALL: Set<String> = setOf(CATEGORY, POLICY, POLICY_SUITE, DROP_CATEGORY, DROP_POLICY, DROP_POLICY_SUITE)
}

/** [org.poc.objs.api.seed.SeedDocumentHandler] shared apply order for policy catalog kinds (G-P39seed). */
const val POLICY_SEED_APPLY_ORDER: Int = 50

/** Seed document `mode` values (G-P35seed): `apply` (MERGE, default) vs `replace`. */
object PolicySeedModes {
    const val APPLY: String = "apply"
    const val REPLACE: String = "replace"

    fun requireValid(raw: Any?): String {
        val mode = raw?.toString()?.trim()?.lowercase()?.ifBlank { APPLY } ?: APPLY
        require(mode == APPLY || mode == REPLACE) {
            "mode must be '$APPLY' or '$REPLACE' (got: '$mode')"
        }
        return mode
    }
}
