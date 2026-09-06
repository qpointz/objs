package org.poc.objs.policy.service.seed

import org.poc.objs.api.seed.ParsedSeedDocument
import org.poc.objs.api.seed.SeedDocumentHandler
import org.poc.objs.api.seed.SeedDocumentParseException
import org.poc.objs.api.seed.SeedDocumentResult
import org.poc.objs.api.seed.SeedDocumentValidationException
import org.poc.objs.api.seed.SeedRawDocument
import org.poc.objs.api.seed.requireText
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.policy.api.MetadataMatcher
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicySeedKinds
import org.poc.objs.policy.api.PolicySeedModes
import org.poc.objs.policy.api.PolicySuiteWrite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteExecutionStrategyKinds
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderRollUpModes
import org.poc.objs.policy.api.SuiteFolderWrite
import org.poc.objs.policy.api.SuiteMatcher
import org.poc.objs.policy.api.SuiteMatcherMode
import org.poc.objs.policy.api.SuiteRepository
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import org.poc.objs.policy.api.POLICY_SEED_APPLY_ORDER
import java.util.UUID

/** One [StaticListMatcher] entry before `policyKey` → id resolution (apply time). */
data class SeedStaticEntry(
    val policyId: UUID? = null,
    val policyKey: String? = null,
    val version: String? = null,
)

data class SeedMatcher(
    val kind: String,
    val mode: SuiteMatcherMode,
    val skipMissing: Boolean,
    val entries: List<SeedStaticEntry> = emptyList(),
    val categoryKey: String? = null,
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
)

data class SeedFolder(
    val id: UUID,
    val key: String,
    val name: String,
    val parentId: UUID?,
    val sortOrder: Int,
    val participation: SuiteFolderParticipation,
    val rollUpMode: String,
    val matchers: List<SeedMatcher>,
    val tags: List<String>,
    val annotations: Map<String, String>,
    val severityConfig: String?,
)

data class PolicySuiteSeedPayload(
    val key: String,
    val name: String,
    val rollUpStrategyKind: String,
    val executionStrategyKind: String,
    val folders: List<SeedFolder>,
    val tags: List<String>,
    val annotations: Map<String, String>,
    val mode: String,
)

/**
 * `kind: PolicySuite` (G-P35seed). MERGE/resolve on
 * [org.poc.objs.policy.api.PolicySuite.key] (G-P36seed). `apply` upserts in place (keeps the
 * suite id when it already exists); `replace` deletes the existing suite then saves fresh.
 * Folders are addressed by `key` + optional `parentKey` (resolved to stable ids within the
 * document); `STATIC_LIST` matcher entries accept either `policyId` (UUID) or `policyKey`
 * (resolved to the latest revision at apply time — dangling ref fails the whole resource,
 * G-P40seed). Suite tree validation (cycles / orphans) happens inside [SuiteRepository].
 */
class PolicySuiteSeedHandler(
    private val suites: SuiteRepository,
    private val policies: PolicyRepository,
) : SeedDocumentHandler {
    override val kind: String = PolicySeedKinds.POLICY_SUITE
    override val applyOrder: Int = POLICY_SEED_APPLY_ORDER

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val raw = document.raw
        val index = document.index
        val key = requireSeedKey(raw, "key", index)
        val name = raw["name"]?.toString()?.trim()?.ifBlank { key } ?: key
        val rollUpStrategyKind = raw["rollUpStrategyKind"]?.toString()?.trim()
            ?.ifBlank { SuiteRollUpStrategyKinds.BUILTIN } ?: SuiteRollUpStrategyKinds.BUILTIN
        val executionStrategyKind = raw["executionStrategyKind"]?.toString()?.trim()
            ?.ifBlank { SuiteExecutionStrategyKinds.DEDUPE } ?: SuiteExecutionStrategyKinds.DEDUPE
        val tags = parseSeedStringList(raw["tags"], index, "tags")
        val annotations = parseSeedStringMap(raw["annotations"], index, "annotations")
        val mode = parseSeedMode(raw["mode"], index)
        val folders = parseFolders(raw["folders"], key, index)
        return ParsedSeedDocument(
            document = document,
            identity = key,
            payload = PolicySuiteSeedPayload(
                key = key,
                name = name,
                rollUpStrategyKind = rollUpStrategyKind,
                executionStrategyKind = executionStrategyKind,
                folders = folders,
                tags = tags,
                annotations = annotations,
                mode = mode,
            ),
        )
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val payload = parsed.payload as PolicySuiteSeedPayload
        val folderWrites = payload.folders.map { folder ->
            SuiteFolderWrite(
                id = folder.id,
                key = folder.key,
                name = folder.name,
                parentId = folder.parentId,
                sortOrder = folder.sortOrder,
                participation = folder.participation,
                rollUpMode = folder.rollUpMode,
                matchers = folder.matchers.map { resolveMatcher(it, parsed.document.index, payload.key) },
                tags = folder.tags,
                annotations = folder.annotations,
                severityConfig = folder.severityConfig,
            )
        }
        val write = PolicySuiteWrite(
            key = payload.key,
            name = payload.name,
            rollUpStrategyKind = payload.rollUpStrategyKind,
            executionStrategyKind = payload.executionStrategyKind,
            folders = folderWrites,
            tags = payload.tags,
            annotations = payload.annotations,
        )
        val existing = suites.findByKey(payload.key)
        if (payload.mode == PolicySeedModes.REPLACE) {
            if (existing != null) suites.delete(existing.id)
            suites.save(write)
        } else if (existing != null) {
            suites.update(existing.id, write)
        } else {
            suites.save(write)
        }
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = kind,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }

    private fun resolveMatcher(matcher: SeedMatcher, index: Int, suiteKey: String): SuiteMatcher =
        when (matcher.kind) {
            "STATIC_LIST" -> StaticListMatcher(
                mode = matcher.mode,
                skipMissing = matcher.skipMissing,
                entries = matcher.entries.map { resolveEntry(it, index, suiteKey) },
            )
            "METADATA" -> MetadataMatcher(
                mode = matcher.mode,
                skipMissing = matcher.skipMissing,
                categoryKey = matcher.categoryKey,
                tags = matcher.tags,
                annotations = matcher.annotations,
            )
            else -> throw SeedDocumentValidationException(
                index,
                listOf(ValidationIssue("SEED_MATCHER_KIND_UNKNOWN", "Unknown matcher kind: ${matcher.kind}")),
                "Unknown matcher kind: ${matcher.kind}",
            )
        }

    private fun resolveEntry(entry: SeedStaticEntry, index: Int, suiteKey: String): StaticPolicyEntry {
        if (entry.policyId != null) {
            return StaticPolicyEntry(entry.policyId, entry.version)
        }
        val key = entry.policyKey!!
        val latest = policies.findByKey(key).maxByOrNull { it.serial }
            ?: throw SeedDocumentValidationException(
                index,
                listOf(
                    ValidationIssue(
                        "SEED_DANGLING_REF",
                        "PolicySuite '$suiteKey' references unknown policyKey '$key'",
                        path = "folders[].matchers[].entries[].policyKey",
                    ),
                ),
                "PolicySuite '$suiteKey' references unknown policyKey '$key'",
            )
        return StaticPolicyEntry(latest.id, entry.version)
    }

    private fun parseFolders(raw: Any?, suiteKey: String, index: Int): List<SeedFolder> {
        if (raw == null) return emptyList()
        val list = raw as? List<*> ?: throw SeedDocumentParseException(index, "folders must be a list")
        val ids = linkedMapOf<String, UUID>()
        val folderMaps = list.mapIndexed { i, item ->
            @Suppress("UNCHECKED_CAST")
            val map = item as? Map<String, Any?>
                ?: throw SeedDocumentParseException(index, "folders[$i] must be an object")
            val folderKey = requireText(map, "key", index, "folders[$i].key")
            if (ids.containsKey(folderKey)) {
                throw SeedDocumentParseException(index, "Duplicate folder key: $folderKey")
            }
            ids[folderKey] = UUID.nameUUIDFromBytes("suite-folder:$suiteKey:$folderKey".toByteArray())
            map to folderKey
        }
        return folderMaps.mapIndexed { i, (map, folderKey) ->
            val name = map["name"]?.toString()?.trim()?.ifBlank { folderKey } ?: folderKey
            val parentKey = map["parentKey"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val parentId = parentKey?.let {
                ids[it] ?: throw SeedDocumentParseException(
                    index,
                    "folders[$i].parentKey '$it' does not match any folder key in this document",
                )
            }
            val sortOrder = (map["sortOrder"] as? Number)?.toInt() ?: 0
            val participation = parseParticipation(map["participation"], index, "folders[$i].participation")
            val rollUpMode = map["rollUpMode"]?.toString()?.trim()
                ?.ifBlank { SuiteFolderRollUpModes.ALL_PASS } ?: SuiteFolderRollUpModes.ALL_PASS
            val tags = parseSeedStringList(map["tags"], index, "folders[$i].tags")
            val annotations = parseSeedStringMap(map["annotations"], index, "folders[$i].annotations")
            val severityConfig = map["severityConfig"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val matchers = parseMatchers(map["matchers"], index, i)
            SeedFolder(
                id = ids[folderKey]!!,
                key = folderKey,
                name = name,
                parentId = parentId,
                sortOrder = sortOrder,
                participation = participation,
                rollUpMode = rollUpMode,
                matchers = matchers,
                tags = tags,
                annotations = annotations,
                severityConfig = severityConfig,
            )
        }
    }

    private fun parseMatchers(raw: Any?, index: Int, folderIndex: Int): List<SeedMatcher> {
        if (raw == null) return emptyList()
        val list = raw as? List<*>
            ?: throw SeedDocumentParseException(index, "folders[$folderIndex].matchers must be a list")
        return list.mapIndexed { j, item ->
            @Suppress("UNCHECKED_CAST")
            val map = item as? Map<String, Any?>
                ?: throw SeedDocumentParseException(index, "folders[$folderIndex].matchers[$j] must be an object")
            val path = "folders[$folderIndex].matchers[$j]"
            val matcherKind = requireText(map, "kind", index, "$path.kind").uppercase()
            val mode = parseMatcherMode(map["mode"], index, "$path.mode")
            val skipMissing = map["skipMissing"] as? Boolean ?: false
            when (matcherKind) {
                "STATIC_LIST" -> {
                    val entriesRaw = map["entries"] as? List<*> ?: emptyList<Any?>()
                    val entries = entriesRaw.mapIndexed { k, entryItem ->
                        @Suppress("UNCHECKED_CAST")
                        val entryMap = entryItem as? Map<String, Any?>
                            ?: throw SeedDocumentParseException(index, "$path.entries[$k] must be an object")
                        val policyId = entryMap["policyId"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                        val policyKey = entryMap["policyKey"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                        if ((policyId == null) == (policyKey == null)) {
                            throw SeedDocumentParseException(
                                index,
                                "$path.entries[$k] requires exactly one of policyId / policyKey",
                            )
                        }
                        val version = entryMap["version"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                        SeedStaticEntry(
                            policyId = policyId?.let {
                                try {
                                    UUID.fromString(it)
                                } catch (_: IllegalArgumentException) {
                                    throw SeedDocumentParseException(index, "$path.entries[$k].policyId must be a UUID")
                                }
                            },
                            policyKey = policyKey,
                            version = version,
                        )
                    }
                    SeedMatcher(kind = matcherKind, mode = mode, skipMissing = skipMissing, entries = entries)
                }
                "METADATA" -> {
                    val categoryKey = map["categoryKey"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                    val tags = parseSeedStringList(map["tags"], index, "$path.tags")
                    val annotations = parseSeedStringMap(map["annotations"], index, "$path.annotations")
                    if (categoryKey.isNullOrBlank() && tags.isEmpty() && annotations.isEmpty()) {
                        throw SeedDocumentParseException(
                            index,
                            "$path requires at least one of categoryKey, tags, annotations",
                        )
                    }
                    SeedMatcher(
                        kind = matcherKind,
                        mode = mode,
                        skipMissing = skipMissing,
                        categoryKey = categoryKey,
                        tags = tags,
                        annotations = annotations,
                    )
                }
                else -> throw SeedDocumentParseException(index, "$path.kind: unknown matcher kind '$matcherKind'")
            }
        }
    }

    private fun parseParticipation(raw: Any?, index: Int, path: String): SuiteFolderParticipation {
        val text = raw?.toString()?.trim()?.uppercase()?.ifBlank { "ENABLED" } ?: "ENABLED"
        return try {
            SuiteFolderParticipation.valueOf(text)
        } catch (_: IllegalArgumentException) {
            throw SeedDocumentParseException(index, "$path: unknown participation '$text'")
        }
    }

    private fun parseMatcherMode(raw: Any?, index: Int, path: String): SuiteMatcherMode {
        val text = raw?.toString()?.trim()?.uppercase()?.ifBlank { "INCLUDE" } ?: "INCLUDE"
        return try {
            SuiteMatcherMode.valueOf(text)
        } catch (_: IllegalArgumentException) {
            throw SeedDocumentParseException(index, "$path: unknown matcher mode '$text'")
        }
    }
}
