package org.poc.objs.policy.api

import java.util.UUID

/** Folder participation in suite evaluate / roll-up (C-27). */
enum class SuiteFolderParticipation {
    /** Evaluate subtree; votes in parent roll-up. */
    ENABLED,
    /** No evaluation; no results; no vote. */
    DISABLED,
    /** Evaluate; results kept; does not vote in parent. */
    IGNORED,
}

/** Matcher include vs exclude mode. */
enum class SuiteMatcherMode {
    INCLUDE,
    EXCLUDE,
}

/**
 * Per-folder roll-up **mode** — input interpreted by the suite's [SuiteRollUpStrategy]
 * (G-P29s). Not a strategy class selection.
 */
object SuiteFolderRollUpModes {
    const val ALL_PASS: String = "ALL_PASS"
    const val ANY_PASS: String = "ANY_PASS"
}

/**
 * Well-known [PolicySuite.rollUpStrategyKind] values — which **strategy implementation**
 * interprets folder roll-up inputs suite-wide (G-P29s).
 */
object SuiteRollUpStrategyKinds {
    /** Built-in: reads [SuiteFolder.rollUpMode] + severityConfig. */
    const val BUILTIN: String = "BUILTIN"
    // Future: DROOLS — Drools-backed SuiteRollUpStrategy (not in C-27).
}

/** Well-known [PolicySuite.executionStrategyKind] / run defaults. */
object SuiteExecutionStrategyKinds {
    const val DEDUPE: String = "DEDUPE"
    const val NO_DEDUPE: String = "NO_DEDUPE"
}

/**
 * One policy entry in a [StaticListMatcher].
 * [policyId] is [Policy.id] (stable across serial bumps).
 * [version] omitted = latest; `major.minor.serial` = exact; `major.*` = latest in major line.
 */
data class StaticPolicyEntry(
    val policyId: UUID,
    val version: String? = null,
)

/** Extensible folder matcher (kind + payload). */
sealed class SuiteMatcher {
    abstract val mode: SuiteMatcherMode
    /** When true, unresolved STATIC_LIST entries are skipped; default false → evaluation ERROR. */
    abstract val skipMissing: Boolean
}

data class StaticListMatcher(
    override val mode: SuiteMatcherMode = SuiteMatcherMode.INCLUDE,
    override val skipMissing: Boolean = false,
    val entries: List<StaticPolicyEntry> = emptyList(),
) : SuiteMatcher()

/**
 * Metadata matcher: at least one of [categorySlug], [tags], [annotations] must be set;
 * criteria combine with AND. Resolves to latest matching policies.
 */
data class MetadataMatcher(
    override val mode: SuiteMatcherMode = SuiteMatcherMode.INCLUDE,
    override val skipMissing: Boolean = false,
    val categorySlug: String? = null,
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
) : SuiteMatcher() {
    init {
        require(
            !categorySlug.isNullOrBlank() || tags.isNotEmpty() || annotations.isNotEmpty(),
        ) { "MetadataMatcher requires at least one of categorySlug, tags, annotations" }
    }
}

data class SuiteFolder(
    val id: UUID,
    val key: String,
    val name: String,
    val parentId: UUID? = null,
    val sortOrder: Int = 0,
    val participation: SuiteFolderParticipation = SuiteFolderParticipation.ENABLED,
    /**
     * Roll-up mode for this folder ([SuiteFolderRollUpModes]); interpreted by the suite
     * [PolicySuite.rollUpStrategyKind] strategy.
     */
    val rollUpMode: String = SuiteFolderRollUpModes.ALL_PASS,
    val matchers: List<SuiteMatcher> = emptyList(),
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
    /** When non-passing, optional severity override; null = max of children. */
    val severityConfig: String? = null,
)

data class PolicySuite(
    val id: UUID,
    val name: String,
    /** Which roll-up strategy implementation to use ([SuiteRollUpStrategyKinds]). */
    val rollUpStrategyKind: String = SuiteRollUpStrategyKinds.BUILTIN,
    val executionStrategyKind: String = SuiteExecutionStrategyKinds.DEDUPE,
    val folders: List<SuiteFolder> = emptyList(),
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
)

data class SuiteFolderWrite(
    val id: UUID? = null,
    val key: String,
    val name: String,
    val parentId: UUID? = null,
    val sortOrder: Int = 0,
    val participation: SuiteFolderParticipation = SuiteFolderParticipation.ENABLED,
    val rollUpMode: String = SuiteFolderRollUpModes.ALL_PASS,
    val matchers: List<SuiteMatcher> = emptyList(),
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
    val severityConfig: String? = null,
)

data class PolicySuiteWrite(
    val name: String,
    val rollUpStrategyKind: String = SuiteRollUpStrategyKinds.BUILTIN,
    val executionStrategyKind: String = SuiteExecutionStrategyKinds.DEDUPE,
    val folders: List<SuiteFolderWrite> = emptyList(),
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
)
