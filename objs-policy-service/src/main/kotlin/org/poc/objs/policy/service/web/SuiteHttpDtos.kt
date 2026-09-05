package org.poc.objs.policy.service.web

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

data class StaticPolicyEntryDto(
    val policyId: UUID,
    val version: String? = null,
)

data class SuiteMatcherDto(
    val kind: String,
    val mode: String = "INCLUDE",
    val skipMissing: Boolean = false,
    val entries: List<StaticPolicyEntryDto> = emptyList(),
    val categorySlug: String? = null,
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
)

data class SuiteFolderDto(
    val id: UUID? = null,
    val key: String,
    val name: String,
    val parentId: UUID? = null,
    val sortOrder: Int = 0,
    val participation: String = "ENABLED",
    val rollUpMode: String = "ALL_PASS",
    val matchers: List<SuiteMatcherDto> = emptyList(),
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
    val severityConfig: String? = null,
)

data class PolicySuiteDto(
    val id: UUID? = null,
    val name: String,
    val rollUpStrategyKind: String = SuiteRollUpStrategyKinds.BUILTIN,
    val executionStrategyKind: String = SuiteExecutionStrategyKinds.DEDUPE,
    val folders: List<SuiteFolderDto> = emptyList(),
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
)

data class SuiteEvaluateRequest(
    val suiteId: UUID,
    val matcher: JsonNode? = null,
    val graphId: String? = null,
    val graphVersion: Long? = null,
    val scope: String = "FULL",
    val folderId: UUID? = null,
    val folderIds: List<UUID>? = null,
    val policyIds: List<UUID>? = null,
)

data class SuiteSelectionRequest(
    val suiteId: UUID,
    val folderId: UUID? = null,
    val scope: String = "FULL",
    val folderIds: List<UUID>? = null,
    val policyIds: List<UUID>? = null,
)

object SuiteHttpMapping {
    fun toWrite(dto: PolicySuiteDto): PolicySuiteWrite =
        PolicySuiteWrite(
            name = dto.name,
            rollUpStrategyKind = dto.rollUpStrategyKind,
            executionStrategyKind = dto.executionStrategyKind,
            folders = dto.folders.map { toFolderWrite(it) },
            tags = dto.tags,
            annotations = dto.annotations,
        )

    fun toDto(suite: PolicySuite): PolicySuiteDto =
        PolicySuiteDto(
            id = suite.id,
            name = suite.name,
            rollUpStrategyKind = suite.rollUpStrategyKind,
            executionStrategyKind = suite.executionStrategyKind,
            folders = suite.folders.map { toFolderDto(it) },
            tags = suite.tags,
            annotations = suite.annotations,
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
                categorySlug = dto.categorySlug,
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
                categorySlug = matcher.categorySlug,
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
