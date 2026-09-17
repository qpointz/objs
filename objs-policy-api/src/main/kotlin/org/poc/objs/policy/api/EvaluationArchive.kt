package org.poc.objs.policy.api

import org.poc.objs.api.domain.GraphFragment
import java.util.UUID

/** Which content slices to write on save (C-33 G-P48r). */
data class PersistContentAxes(
    val results: Boolean = false,
    val executionContext: Boolean = false,
    val input: Boolean = false,
) {
    val anyDurable: Boolean get() = results || executionContext || input
}

/**
 * Fine-tune which outcome/finding sets persist when [PersistContentAxes.results] is on (G-P49r / G-P54v).
 * Null set = all values. Empty set = persist none of that kind.
 * Finding severity [UNSPECIFIED] matches findings with null severity.
 * Finding severity filter tokens: CRITICAL…INFO; dual-read legacy ERROR→HIGH, WARNING→MEDIUM, OK→null.
 * Outcome status filter: dual-read legacy ERROR→EXEC_ERROR via [PolicyOutcomeStatus.parseToken].
 */
data class PersistResultFilters(
    val outcomeStatuses: Set<PolicyOutcomeStatus>? = null,
    val findingSeverities: Set<String>? = null,
) {
    companion object {
        const val UNSPECIFIED: String = "UNSPECIFIED"

        val ALL: PersistResultFilters = PersistResultFilters(outcomeStatuses = null, findingSeverities = null)
    }
}

/**
 * Persist options: content axes + filters + labeling/runtime (C-33).
 * Prefer [PersistPresets] for EPHEMERAL / STANDARD / FULL.
 */
data class PersistSpec(
    val axes: PersistContentAxes,
    val filters: PersistResultFilters = PersistResultFilters.ALL,
    val name: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
    val durationMs: Long? = null,
    val evaluatedAtEpochMs: Long? = null,
    val evaluationId: UUID? = null,
    val presetName: String? = null,
    val origin: String? = null,
)

object PersistPresets {
    const val EPHEMERAL: String = "EPHEMERAL"
    const val STANDARD: String = "STANDARD"
    const val FULL: String = "FULL"

    fun ephemeral(): PersistSpec =
        PersistSpec(axes = PersistContentAxes(), presetName = EPHEMERAL)

    fun standard(): PersistSpec =
        PersistSpec(
            axes = PersistContentAxes(results = true, executionContext = true),
            presetName = STANDARD,
        )

    fun full(): PersistSpec =
        PersistSpec(
            axes = PersistContentAxes(results = true, executionContext = true, input = true),
            presetName = FULL,
        )
}

/** Opaque execution-context snapshot (policies/matchers/bodies at T₀) as a JSON-friendly map. */
typealias ExecutionContextSnapshot = Map<String, Any?>

/**
 * Thin list row for [EvaluationArchive.list] (C-39). Row metadata + persist_profile axes only —
 * no outcomes, tree, executionContext, or input.
 */
data class EvaluationArchiveSummary(
    val evaluationId: UUID,
    val kind: String,
    val name: String? = null,
    val description: String? = null,
    val evaluatedAtEpochMs: Long,
    val overallStatus: PolicyOutcomeStatus? = null,
    val overallSeverity: FindingSeverity? = null,
    val suiteId: UUID? = null,
    val suiteName: String? = null,
    val tags: List<String> = emptyList(),
    val presetName: String? = null,
    val origin: String? = null,
    val durationMs: Long? = null,
    val axes: PersistContentAxes,
)

/**
 * Loaded evaluation archive (C-33). When [includeInput] is false (STANDARD view), [input] is null
 * even if the row stored a pack.
 */
data class EvaluationArchiveDocument(
    val evaluationId: UUID,
    val kind: String,
    val name: String? = null,
    val description: String? = null,
    val meta: PolicyEvaluationMeta,
    val outcomes: List<PolicyOutcome> = emptyList(),
    val tree: SuiteFolderResult? = null,
    val overall: PolicyOutcomeStatus? = null,
    val axes: PersistContentAxes,
    val filters: PersistResultFilters,
    val presetName: String? = null,
    val durationMs: Long? = null,
    val origin: String? = null,
    val executionContext: ExecutionContextSnapshot? = null,
    val input: GraphFragment? = null,
)

/**
 * Durable evaluation archive port (C-33). Explicit save only — evaluate paths do not auto-persist.
 */
interface EvaluationArchive {
    /**
     * Persist a flat [EvaluationResult]. No-op (returns null) when [PersistSpec.axes] has no durable axes
     * (EPHEMERAL).
     */
    fun saveFlat(
        result: EvaluationResult,
        spec: PersistSpec,
        meta: PolicyEvaluationMeta? = null,
        executionContext: ExecutionContextSnapshot? = null,
        input: GraphFragment? = null,
    ): UUID?

    /**
     * Persist a [SuiteEvaluationResult]. No-op when axes are empty.
     */
    fun saveSuite(
        result: SuiteEvaluationResult,
        spec: PersistSpec,
        executionContext: ExecutionContextSnapshot? = null,
        input: GraphFragment? = null,
    ): UUID?

    fun load(evaluationId: UUID): EvaluationArchiveDocument?

    /** Load omitting input pack even if stored (STANDARD view). */
    fun loadAsStandard(evaluationId: UUID): EvaluationArchiveDocument?

    fun delete(evaluationId: UUID)

    /**
     * List archive summaries (newest first). Does not hydrate outcomes/findings/tree/context/input.
     * [limit] is clamped to 1..[LIST_LIMIT_MAX] (default [LIST_LIMIT_DEFAULT]).
     * [kind] exact match when non-null; [tag] matches when the row's tags contain that string.
     */
    fun list(
        limit: Int = LIST_LIMIT_DEFAULT,
        offset: Int = 0,
        kind: String? = null,
        tag: String? = null,
    ): List<EvaluationArchiveSummary>

    companion object {
        const val LIST_LIMIT_DEFAULT: Int = 50
        const val LIST_LIMIT_MAX: Int = 200
    }
}
