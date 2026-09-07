package org.poc.objs.core.persistence.policy

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.GraphFragment
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.policy.api.EvaluationArchive
import org.poc.objs.policy.api.EvaluationArchiveDocument
import org.poc.objs.policy.api.EvaluationResult
import org.poc.objs.policy.api.ExecutionContextSnapshot
import org.poc.objs.policy.api.Finding
import org.poc.objs.policy.api.PersistContentAxes
import org.poc.objs.policy.api.PersistResultFilters
import org.poc.objs.policy.api.PersistSpec
import org.poc.objs.policy.api.PolicyEvaluationKinds
import org.poc.objs.policy.api.PolicyEvaluationMeta
import org.poc.objs.policy.api.PolicyOutcome
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.SuiteEvaluationResult
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderResult
import org.poc.objs.policy.api.SuitePolicyLeafResult
import org.poc.objs.policy.api.aggregateOverall
import java.time.Instant
import java.util.UUID

class JpaEvaluationArchive(
    private val evaluationDao: PolicyEvaluationDao,
    private val outcomeDao: PolicyOutcomeDao,
    private val findingDao: PolicyFindingDao,
    private val uow: UnitOfWork,
) : EvaluationArchive {

    override fun saveFlat(
        result: EvaluationResult,
        spec: PersistSpec,
        meta: PolicyEvaluationMeta?,
        executionContext: ExecutionContextSnapshot?,
        input: GraphFragment?,
    ): UUID? {
        if (!spec.axes.anyDurable) return null
        val evaluatedAt = spec.evaluatedAtEpochMs ?: System.currentTimeMillis()
        val evaluationId = spec.evaluationId ?: meta?.evaluationId ?: UUID.randomUUID()
        val resolvedMeta =
            meta ?: PolicyEvaluationMeta(
                evaluationId = evaluationId,
                kind = PolicyEvaluationKinds.FLAT,
                evaluatedAtEpochMs = evaluatedAt,
                overallStatus = result.overall ?: aggregateOverall(result.outcomes),
                tags = spec.tags,
                annotations = spec.annotations,
            )
        return persist(
            kind = PolicyEvaluationKinds.FLAT,
            meta = resolvedMeta.copy(
                evaluationId = evaluationId,
                tags = if (spec.tags.isNotEmpty()) spec.tags else resolvedMeta.tags,
                annotations = if (spec.annotations.isNotEmpty()) spec.annotations else resolvedMeta.annotations,
            ),
            outcomes = result.outcomes,
            tree = null,
            overall = result.overall ?: aggregateOverall(result.outcomes),
            spec = spec,
            executionContext = executionContext,
            input = input,
        )
    }

    override fun saveSuite(
        result: SuiteEvaluationResult,
        spec: PersistSpec,
        executionContext: ExecutionContextSnapshot?,
        input: GraphFragment?,
    ): UUID? {
        if (!spec.axes.anyDurable) return null
        val evaluationId = spec.evaluationId ?: result.meta.evaluationId
        val meta =
            result.meta.copy(
                evaluationId = evaluationId,
                tags = if (spec.tags.isNotEmpty()) spec.tags else result.meta.tags,
                annotations = if (spec.annotations.isNotEmpty()) spec.annotations else result.meta.annotations,
                evaluatedAtEpochMs = spec.evaluatedAtEpochMs ?: result.meta.evaluatedAtEpochMs,
            )
        return persist(
            kind = PolicyEvaluationKinds.SUITE,
            meta = meta,
            outcomes = result.outcomes,
            tree = result.tree,
            overall = meta.overallStatus,
            spec = spec,
            executionContext = executionContext,
            input = input,
        )
    }

    override fun load(evaluationId: UUID): EvaluationArchiveDocument? =
        uow.read { loadInternal(evaluationId, includeInput = true) }

    override fun loadAsStandard(evaluationId: UUID): EvaluationArchiveDocument? =
        uow.read { loadInternal(evaluationId, includeInput = false) }

    override fun delete(evaluationId: UUID) {
        uow.write {
            val outcomes = outcomeDao.findByEvaluationId(evaluationId)
            findingDao.deleteByOutcomeIds(outcomes.map { it.outcomeId })
            outcomeDao.deleteByEvaluationId(evaluationId)
            evaluationDao.deleteById(evaluationId)
        }
    }

    private fun persist(
        kind: String,
        meta: PolicyEvaluationMeta,
        outcomes: List<PolicyOutcome>,
        tree: SuiteFolderResult?,
        overall: PolicyOutcomeStatus?,
        spec: PersistSpec,
        executionContext: ExecutionContextSnapshot?,
        input: GraphFragment?,
    ): UUID {
        val evaluationId = meta.evaluationId
        uow.write {
            // Replace existing archive with same id
            val existingOutcomes = outcomeDao.findByEvaluationId(evaluationId)
            findingDao.deleteByOutcomeIds(existingOutcomes.map { it.outcomeId })
            outcomeDao.deleteByEvaluationId(evaluationId)

            val filteredOutcomes =
                if (spec.axes.results) filterOutcomes(outcomes, spec.filters) else emptyList()
            val filteredTree =
                if (spec.axes.results && tree != null) filterTree(tree, filteredOutcomes) else null

            val labels = if (spec.tags.isNotEmpty()) spec.tags else meta.tags
            val anns = if (spec.annotations.isNotEmpty()) spec.annotations else meta.annotations

            val record =
                PolicyEvaluationRecord(
                    evaluationId = evaluationId,
                    kind = kind,
                    evaluatedAt = Instant.ofEpochMilli(meta.evaluatedAtEpochMs),
                    executionStrategyKind = meta.executionStrategyKind,
                    rollupStrategyKind = meta.rollUpStrategyKind,
                    overallStatus = (overall ?: meta.overallStatus)?.name,
                    overallSeverity = meta.overallSeverity,
                    suiteId = meta.suiteId,
                    suiteName = meta.suiteName,
                    tags = labels.toMutableList(),
                    annotations = anns.toMutableMap(),
                    persistProfile = persistProfileToMap(spec),
                    suiteTree = filteredTree?.let { suiteTreeToMap(it) },
                    executionContext =
                        if (spec.axes.executionContext) {
                            executionContext?.mapValues { it.value }?.toMutableMap()
                        } else {
                            null
                        },
                    inputFragment =
                        if (spec.axes.input) {
                            input?.let { fragmentToMap(it) }
                        } else {
                            null
                        },
                )
            evaluationDao.save(record)

            if (spec.axes.results) {
                filteredOutcomes.forEachIndexed { index, outcome ->
                    val outcomeId = UUID.randomUUID()
                    outcomeDao.save(
                        PolicyOutcomeRecord(
                            outcomeId = outcomeId,
                            evaluationId = evaluationId,
                            ordinal = index,
                            policyName = outcome.policyName,
                            policySerial = outcome.policySerial,
                            engineKind = outcome.engineKind,
                            status = outcome.status.name,
                            notApplicableReason = outcome.notApplicableReason,
                            message = outcome.message,
                        ),
                    )
                    outcome.findings.forEachIndexed { fIdx, finding ->
                        findingDao.save(
                            PolicyFindingRecord(
                                findingId = UUID.randomUUID(),
                                outcomeId = outcomeId,
                                idx = fIdx,
                                message = finding.message,
                                severity = finding.severity,
                                code = finding.code,
                                entityIds = finding.entities.map { it.toString() }.toMutableList(),
                                edgeIds = finding.edges.map { it.toString() }.toMutableList(),
                                extras = finding.extras.toMutableMap(),
                            ),
                        )
                    }
                }
            }
        }
        return evaluationId
    }

    private fun loadInternal(evaluationId: UUID, includeInput: Boolean): EvaluationArchiveDocument? {
        val record = evaluationDao.findById(evaluationId) ?: return null
        val tags = record.tags.toList()
        val annotations = record.annotations.toMap()
        val outcomeRecords = outcomeDao.findByEvaluationId(evaluationId)
        val findingsByOutcome =
            findingDao.findByOutcomeIds(outcomeRecords.map { it.outcomeId }).groupBy { it.outcomeId }
        val outcomes =
            outcomeRecords.map { o ->
                PolicyOutcome(
                    policyName = o.policyName,
                    policySerial = o.policySerial,
                    engineKind = o.engineKind,
                    status = PolicyOutcomeStatus.valueOf(o.status),
                    notApplicableReason = o.notApplicableReason,
                    message = o.message,
                    findings =
                        findingsByOutcome[o.outcomeId].orEmpty().map { f ->
                            Finding(
                                message = f.message,
                                severity = f.severity,
                                code = f.code,
                                entities = f.entityIds.map(UUID::fromString),
                                edges = f.edgeIds.map(UUID::fromString),
                                extras = f.extras.toMap(),
                            )
                        },
                )
            }
        val tree = record.suiteTree?.let { suiteTreeFromMap(it) }
        val profile = persistProfileFromMap(record.persistProfile)
        val meta =
            PolicyEvaluationMeta(
                evaluationId = record.evaluationId,
                kind = record.kind,
                evaluatedAtEpochMs = record.evaluatedAt.toEpochMilli(),
                executionStrategyKind = record.executionStrategyKind,
                rollUpStrategyKind = record.rollupStrategyKind,
                overallStatus = record.overallStatus?.let { PolicyOutcomeStatus.valueOf(it) },
                overallSeverity = record.overallSeverity,
                tags = tags,
                annotations = annotations,
                suiteId = record.suiteId,
                suiteName = record.suiteName,
            )
        return EvaluationArchiveDocument(
            evaluationId = record.evaluationId,
            kind = record.kind,
            name = profile.name,
            description = profile.description,
            meta = meta,
            outcomes = outcomes,
            tree = tree,
            overall = meta.overallStatus,
            axes = profile.axes,
            filters = profile.filters,
            presetName = profile.presetName,
            durationMs = profile.durationMs,
            origin = profile.origin,
            executionContext = if (profile.axes.executionContext) record.executionContext?.toMap() else null,
            input =
                if (includeInput && profile.axes.input) {
                    record.inputFragment?.let { fragmentFromMap(it) }
                } else {
                    null
                },
        )
    }
}

/** Known PersistSpec fields projected into [PolicyEvaluationRecord.persistProfile]; extra map keys are preserved. */
internal data class ParsedPersistProfile(
    val axes: PersistContentAxes,
    val filters: PersistResultFilters,
    val presetName: String? = null,
    val name: String? = null,
    val description: String? = null,
    val origin: String? = null,
    val durationMs: Long? = null,
)

internal fun persistProfileToMap(spec: PersistSpec): MutableMap<String, Any?> {
    val axes =
        mutableMapOf<String, Any?>(
            PersistProfileKeys.AXIS_RESULTS to spec.axes.results,
            PersistProfileKeys.AXIS_EXECUTION_CONTEXT to spec.axes.executionContext,
            PersistProfileKeys.AXIS_INPUT to spec.axes.input,
        )
    val filters = mutableMapOf<String, Any?>()
    spec.filters.outcomeStatuses?.let { filters[PersistProfileKeys.FILTER_OUTCOME_STATUSES] = it.map { s -> s.name } }
    spec.filters.findingSeverities?.let { filters[PersistProfileKeys.FILTER_FINDING_SEVERITIES] = it.toList() }
    return mutableMapOf(
        PersistProfileKeys.PRESET_NAME to spec.presetName,
        PersistProfileKeys.AXES to axes,
        PersistProfileKeys.FILTERS to filters,
        PersistProfileKeys.NAME to spec.name,
        PersistProfileKeys.DESCRIPTION to spec.description,
        PersistProfileKeys.ORIGIN to spec.origin,
        PersistProfileKeys.DURATION_MS to spec.durationMs,
    )
}

@Suppress("UNCHECKED_CAST")
internal fun persistProfileFromMap(raw: Map<String, Any?>): ParsedPersistProfile {
    val axesRaw = raw[PersistProfileKeys.AXES] as? Map<*, *>
    val axes =
        PersistContentAxes(
            results = axesRaw?.get(PersistProfileKeys.AXIS_RESULTS) as? Boolean ?: false,
            executionContext = axesRaw?.get(PersistProfileKeys.AXIS_EXECUTION_CONTEXT) as? Boolean ?: false,
            input = axesRaw?.get(PersistProfileKeys.AXIS_INPUT) as? Boolean ?: false,
        )
    val filtersRaw = raw[PersistProfileKeys.FILTERS] as? Map<*, *>
    val statuses =
        (filtersRaw?.get(PersistProfileKeys.FILTER_OUTCOME_STATUSES) as? List<*>)
            ?.mapNotNull { (it as? String)?.let(PolicyOutcomeStatus::valueOf) }
            ?.toSet()
    val severities =
        (filtersRaw?.get(PersistProfileKeys.FILTER_FINDING_SEVERITIES) as? List<*>)
            ?.mapNotNull { it as? String }
            ?.toSet()
    val duration =
        when (val d = raw[PersistProfileKeys.DURATION_MS]) {
            null -> null
            is Number -> d.toLong()
            else -> null
        }
    return ParsedPersistProfile(
        axes = axes,
        filters = PersistResultFilters(outcomeStatuses = statuses, findingSeverities = severities),
        presetName = raw[PersistProfileKeys.PRESET_NAME] as? String,
        name = raw[PersistProfileKeys.NAME] as? String,
        description = raw[PersistProfileKeys.DESCRIPTION] as? String,
        origin = raw[PersistProfileKeys.ORIGIN] as? String,
        durationMs = duration,
    )
}

internal fun filterOutcomes(
    outcomes: List<PolicyOutcome>,
    filters: PersistResultFilters,
): List<PolicyOutcome> {
    val statusSet = filters.outcomeStatuses
    val severitySet = filters.findingSeverities
    return outcomes.mapNotNull { outcome ->
        if (statusSet != null && outcome.status !in statusSet) {
            return@mapNotNull null
        }
        val findings =
            when {
                severitySet == null -> outcome.findings
                severitySet.isEmpty() -> emptyList()
                else ->
                    outcome.findings.filter { finding ->
                        val sev = finding.severity
                        if (sev == null) {
                            PersistResultFilters.UNSPECIFIED in severitySet
                        } else {
                            sev in severitySet
                        }
                    }
            }
        outcome.copy(findings = findings)
    }
}

/** Drop leaves whose outcomes were filtered out; keep folder structure for remaining leaves. */
internal fun filterTree(tree: SuiteFolderResult, keptOutcomes: List<PolicyOutcome>): SuiteFolderResult {
    val keptIndexes = keptOutcomes.indices.toSet()
    // Remap: old outcome indexes may not match after status filter — rebuild by ordinal match on outcomes list.
    // Leaves reference outcomeIndex into the *filtered* outcomes list: recompute by matching policy identity.
    fun remap(folder: SuiteFolderResult): SuiteFolderResult {
        val children = folder.children.map { remap(it) }.filter { it.leaves.isNotEmpty() || it.children.isNotEmpty() }
        val leaves =
            folder.leaves.mapNotNull { leaf ->
                val idx =
                    keptOutcomes.indexOfFirst {
                        it.policyName == leaf.policyName &&
                            it.policySerial == leaf.policySerial &&
                            it.status == leaf.status
                    }
                if (idx < 0) null else leaf.copy(outcomeIndex = idx)
            }
        return folder.copy(children = children, leaves = leaves)
    }
    return remap(tree)
}

internal fun suiteTreeToMap(tree: SuiteFolderResult): MutableMap<String, Any?> =
    mutableMapOf(
        "folderId" to tree.folderId.toString(),
        "key" to tree.key,
        "name" to tree.name,
        "parentFolderId" to tree.parentFolderId?.toString(),
        "participation" to tree.participation.name,
        "status" to tree.status.name,
        "severity" to tree.severity,
        "votes" to tree.votes,
        "tags" to tree.tags,
        "annotations" to tree.annotations,
        "children" to tree.children.map { suiteTreeToMap(it) },
        "leaves" to
            tree.leaves.map {
                mutableMapOf<String, Any?>(
                    "policyId" to it.policyId.toString(),
                    "policyName" to it.policyName,
                    "policySerial" to it.policySerial,
                    "policyVersion" to it.policyVersion,
                    "status" to it.status.name,
                    "severity" to it.severity,
                    "outcomeIndex" to it.outcomeIndex,
                )
            },
    )

@Suppress("UNCHECKED_CAST")
internal fun suiteTreeFromMap(raw: Map<String, Any?>): SuiteFolderResult {
    val children =
        (raw["children"] as? List<*>).orEmpty().mapNotNull { child ->
            (child as? Map<*, *>)?.let { suiteTreeFromMap(it as Map<String, Any?>) }
        }
    val leaves =
        (raw["leaves"] as? List<*>).orEmpty().mapNotNull { leaf ->
            val m = leaf as? Map<*, *> ?: return@mapNotNull null
            SuitePolicyLeafResult(
                policyId = UUID.fromString(m["policyId"] as String),
                policyName = m["policyName"] as String,
                policySerial = (m["policySerial"] as Number).toLong(),
                policyVersion = m["policyVersion"] as String,
                status = PolicyOutcomeStatus.valueOf(m["status"] as String),
                severity = m["severity"] as? String,
                outcomeIndex = (m["outcomeIndex"] as Number).toInt(),
            )
        }
    return SuiteFolderResult(
        folderId = UUID.fromString(raw["folderId"] as String),
        key = raw["key"] as String,
        name = raw["name"] as String,
        parentFolderId = (raw["parentFolderId"] as? String)?.let(UUID::fromString),
        participation = SuiteFolderParticipation.valueOf(raw["participation"] as String),
        status = PolicyOutcomeStatus.valueOf(raw["status"] as String),
        severity = raw["severity"] as? String,
        votes = raw["votes"] as? Boolean ?: true,
        tags = (raw["tags"] as? List<*>).orEmpty().filterIsInstance<String>(),
        annotations =
            (raw["annotations"] as? Map<*, *>).orEmpty()
                .entries.associate { (k, v) -> k.toString() to v.toString() },
        children = children,
        leaves = leaves,
    )
}

internal fun fragmentToMap(fragment: GraphFragment): MutableMap<String, Any?> =
    mutableMapOf(
        "entities" to
            fragment.entities.map { e ->
                mutableMapOf<String, Any?>(
                    "id" to e.id?.toString(),
                    "type" to e.type,
                    "schemaVersion" to e.schemaVersion,
                    "payload" to e.payload,
                    "annotations" to e.annotations,
                )
            },
        "edges" to
            fragment.edges.map { e ->
                mutableMapOf<String, Any?>(
                    "id" to e.id?.toString(),
                    "source" to e.source.toString(),
                    "target" to e.target.toString(),
                    "role" to e.role,
                    "type" to e.type,
                    "schemaVersion" to e.schemaVersion,
                    "properties" to e.properties,
                )
            },
    )

@Suppress("UNCHECKED_CAST")
internal fun fragmentFromMap(raw: Map<String, Any?>): GraphContents {
    val entities =
        (raw["entities"] as? List<*>).orEmpty().mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            Entity(
                id = (m["id"] as? String)?.let(UUID::fromString),
                type = m["type"] as String,
                schemaVersion = m["schemaVersion"] as String,
                payload = ((m["payload"] as? Map<*, *>)?.entries?.associate { (k, v) -> k.toString() to v } ?: emptyMap())
                    .toMutableMap(),
                annotations =
                    ((m["annotations"] as? Map<*, *>)?.entries?.associate { (k, v) -> k.toString() to v.toString() }
                        ?: emptyMap())
                        .toMutableMap(),
            )
        }
    val edges =
        (raw["edges"] as? List<*>).orEmpty().mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            Edge(
                id = (m["id"] as? String)?.let(UUID::fromString),
                source = UUID.fromString(m["source"] as String),
                target = UUID.fromString(m["target"] as String),
                role = m["role"] as String,
                type = m["type"] as? String,
                schemaVersion = m["schemaVersion"] as? String,
                properties =
                    (m["properties"] as? Map<*, *>)?.entries?.associate { (k, v) -> k.toString() to v }?.toMutableMap(),
            )
        }
    return GraphContents(entities = entities, edges = edges)
}
