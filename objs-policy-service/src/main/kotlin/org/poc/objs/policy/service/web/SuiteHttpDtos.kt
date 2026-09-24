package org.poc.objs.policy.service.web

import io.swagger.v3.oas.annotations.media.Schema
import org.poc.objs.policy.api.MetadataMatcher
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.PolicySuiteWrite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteEvaluateScope
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderWrite
import org.poc.objs.policy.api.SuiteMatcher
import org.poc.objs.policy.api.SuiteMatcherMode
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import org.poc.objs.policy.api.SuiteExecutionStrategyKinds
import tools.jackson.databind.JsonNode
import java.util.UUID

@Schema(description = "One policy pinned by a STATIC_LIST matcher")
data class StaticPolicyEntryDto(
    @field:Schema(description = "Policy id")
    val policyId: UUID,
    @field:Schema(description = "Policy version to pin; omit to follow the latest")
    val version: String? = null,
)

@Schema(description = "Folder matcher selecting policies either explicitly or by metadata")
data class SuiteMatcherDto(
    @field:Schema(description = "Matcher kind: `STATIC_LIST` or `METADATA`")
    val kind: String,
    @field:Schema(description = "`INCLUDE` (default) or `EXCLUDE`")
    val mode: String = "INCLUDE",
    @field:Schema(description = "Ignore entries that no longer resolve instead of failing")
    val skipMissing: Boolean = false,
    @field:Schema(description = "STATIC_LIST entries; ignored for METADATA matchers")
    val entries: List<StaticPolicyEntryDto> = emptyList(),
    @field:Schema(description = "METADATA: restrict to this policy category key")
    val categoryKey: String? = null,
    @field:Schema(description = "METADATA: policy tags that must match")
    val tags: List<String> = emptyList(),
    @field:Schema(description = "METADATA: policy annotations that must match")
    val annotations: Map<String, String> = emptyMap(),
)

@Schema(description = "Suite folder: a node of the suite tree carrying matchers and roll-up config")
data class SuiteFolderDto(
    @field:Schema(description = "Folder id; omit when creating")
    val id: UUID? = null,
    @field:Schema(description = "Stable folder key, unique within the suite")
    val key: String,
    @field:Schema(description = "Folder display name")
    val name: String,
    @field:Schema(description = "Parent folder id; null for a root folder")
    val parentId: UUID? = null,
    @field:Schema(description = "Sibling ordering hint")
    val sortOrder: Int = 0,
    @field:Schema(description = "`ENABLED` (default) or `DISABLED`")
    val participation: String = "ENABLED",
    @field:Schema(description = "How child outcomes roll up, e.g. `ALL_PASS`")
    val rollUpMode: String = "ALL_PASS",
    @field:Schema(description = "Matchers selecting the policies of this folder")
    val matchers: List<SuiteMatcherDto> = emptyList(),
    @field:Schema(description = "Folder tags")
    val tags: List<String> = emptyList(),
    @field:Schema(description = "Free-form folder annotations (string key/value)")
    val annotations: Map<String, String> = emptyMap(),
    @field:Schema(description = "Optional severity configuration for this folder")
    val severityConfig: String? = null,
)

@Schema(description = "Policy suite: a folder tree of matchers with execution and roll-up strategies")
data class PolicySuiteDto(
    @field:Schema(description = "Suite id; omit when creating")
    val id: UUID? = null,
    @field:Schema(description = "Stable suite key")
    val key: String,
    @field:Schema(description = "Suite display name")
    val name: String,
    @field:Schema(description = "Roll-up strategy kind; defaults to BUILTIN")
    val rollUpStrategyKind: String = SuiteRollUpStrategyKinds.BUILTIN,
    @field:Schema(description = "Execution strategy kind; defaults to DEDUPE")
    val executionStrategyKind: String = SuiteExecutionStrategyKinds.DEDUPE,
    @field:Schema(description = "Folder tree (flat list linked by parentId)")
    val folders: List<SuiteFolderDto> = emptyList(),
    @field:Schema(description = "Suite tags")
    val tags: List<String> = emptyList(),
    @field:Schema(description = "Free-form suite annotations (string key/value)")
    val annotations: Map<String, String> = emptyMap(),
    @field:Schema(description = "Suite strategy kind; defaults to BUILTIN")
    val suiteStrategyKind: String = org.poc.objs.policy.api.SuiteStrategyKinds.BUILTIN,
)

@Schema(description = "Evaluate a suite (or part of it) against a matcher-selected graph fragment")
data class SuiteEvaluateRequest(
    @field:Schema(description = "Suite to evaluate")
    val suiteId: UUID,
    @field:Schema(description = "Matcher DSL document selecting the input fragment; defaults to `{ \"all\": true }`")
    val matcher: JsonNode? = null,
    @field:Schema(description = "Graph to scope selection to; omit to select across graphs")
    val graphId: String? = null,
    @field:Schema(description = "Deep graph version pin; requires `graphId`")
    val graphVersion: Long? = null,
    @field:Schema(description = "`FULL` (default), `SUBFOLDER`, `SUBFOLDER_SET`, or `POLICY_SUBSET`")
    val scope: String = "FULL",
    @field:Schema(description = "Required for `SUBFOLDER` scope")
    val folderId: UUID? = null,
    @field:Schema(description = "Required for `SUBFOLDER_SET` scope")
    val folderIds: List<UUID>? = null,
    @field:Schema(description = "Required for `POLICY_SUBSET` scope")
    val policyIds: List<UUID>? = null,
)

@Schema(description = "Preview which policies a suite scope resolves to, without evaluating them")
data class SuiteSelectionRequest(
    @field:Schema(description = "Suite to resolve")
    val suiteId: UUID,
    @field:Schema(description = "Required for `SUBFOLDER` scope")
    val folderId: UUID? = null,
    @field:Schema(description = "`FULL` (default), `SUBFOLDER`, `SUBFOLDER_SET`, or `POLICY_SUBSET`")
    val scope: String = "FULL",
    @field:Schema(description = "Required for `SUBFOLDER_SET` scope")
    val folderIds: List<UUID>? = null,
    @field:Schema(description = "Required for `POLICY_SUBSET` scope")
    val policyIds: List<UUID>? = null,
)

object SuiteHttpMapping {
    fun toWrite(dto: PolicySuiteDto): PolicySuiteWrite =
        PolicySuiteWrite(
            key = dto.key,
            name = dto.name,
            rollUpStrategyKind = dto.rollUpStrategyKind,
            executionStrategyKind = dto.executionStrategyKind,
            folders = dto.folders.map { toFolderWrite(it) },
            tags = dto.tags,
            annotations = dto.annotations,
            suiteStrategyKind = dto.suiteStrategyKind,
        )

    fun toDto(suite: PolicySuite): PolicySuiteDto =
        PolicySuiteDto(
            id = suite.id,
            key = suite.key,
            name = suite.name,
            rollUpStrategyKind = suite.rollUpStrategyKind,
            executionStrategyKind = suite.executionStrategyKind,
            folders = suite.folders.map { toFolderDto(it) },
            tags = suite.tags,
            annotations = suite.annotations,
            suiteStrategyKind = suite.suiteStrategyKind,
        )

    fun parseScope(
        scope: String,
        folderId: UUID?,
        folderIds: List<UUID>?,
        policyIds: List<UUID>?,
    ): SuiteEvaluateScope =
        when (scope.trim().uppercase()) {
            "FULL" -> SuiteEvaluateScope.Full
            "SUBFOLDER" -> SuiteEvaluateScope.Subfolder(
                folderId ?: throw IllegalArgumentException("folderId required for SUBFOLDER scope"),
            )
            "SUBFOLDER_SET" -> SuiteEvaluateScope.SubfolderSet(
                folderIds ?: throw IllegalArgumentException("folderIds required for SUBFOLDER_SET scope"),
            )
            "POLICY_SUBSET" -> SuiteEvaluateScope.PolicySubset(
                policyIds ?: throw IllegalArgumentException("policyIds required for POLICY_SUBSET scope"),
            )
            else -> throw IllegalArgumentException("Unknown scope: $scope")
        }

    private fun toFolderWrite(dto: SuiteFolderDto): SuiteFolderWrite =
        SuiteFolderWrite(
            id = dto.id,
            key = dto.key,
            name = dto.name,
            parentId = dto.parentId,
            sortOrder = dto.sortOrder,
            participation = parseParticipation(dto.participation),
            rollUpMode = dto.rollUpMode,
            matchers = dto.matchers.map { toMatcher(it) },
            tags = dto.tags,
            annotations = dto.annotations,
            severityConfig = dto.severityConfig,
        )

    private fun toFolderDto(folder: SuiteFolder): SuiteFolderDto =
        SuiteFolderDto(
            id = folder.id,
            key = folder.key,
            name = folder.name,
            parentId = folder.parentId,
            sortOrder = folder.sortOrder,
            participation = folder.participation.name,
            rollUpMode = folder.rollUpMode,
            matchers = folder.matchers.map { toMatcherDto(it) },
            tags = folder.tags,
            annotations = folder.annotations,
            severityConfig = folder.severityConfig,
        )

    private fun toMatcher(dto: SuiteMatcherDto): SuiteMatcher {
        val mode = parseMode(dto.mode)
        return when (dto.kind.trim().uppercase()) {
            "STATIC_LIST" -> StaticListMatcher(
                mode = mode,
                skipMissing = dto.skipMissing,
                entries = dto.entries.map { StaticPolicyEntry(it.policyId, it.version) },
            )
            "METADATA" -> MetadataMatcher(
                mode = mode,
                skipMissing = dto.skipMissing,
                categoryKey = dto.categoryKey,
                tags = dto.tags,
                annotations = dto.annotations,
            )
            else -> throw IllegalArgumentException("Unknown matcher kind: ${dto.kind}")
        }
    }

    private fun toMatcherDto(matcher: SuiteMatcher): SuiteMatcherDto =
        when (matcher) {
            is StaticListMatcher -> SuiteMatcherDto(
                kind = "STATIC_LIST",
                mode = matcher.mode.name,
                skipMissing = matcher.skipMissing,
                entries = matcher.entries.map { StaticPolicyEntryDto(it.policyId, it.version) },
            )
            is MetadataMatcher -> SuiteMatcherDto(
                kind = "METADATA",
                mode = matcher.mode.name,
                skipMissing = matcher.skipMissing,
                categoryKey = matcher.categoryKey,
                tags = matcher.tags,
                annotations = matcher.annotations,
            )
        }

    private fun parseParticipation(raw: String): SuiteFolderParticipation =
        try {
            SuiteFolderParticipation.valueOf(raw.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown participation: $raw")
        }

    private fun parseMode(raw: String): SuiteMatcherMode =
        try {
            SuiteMatcherMode.valueOf(raw.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown matcher mode: $raw")
        }
}
