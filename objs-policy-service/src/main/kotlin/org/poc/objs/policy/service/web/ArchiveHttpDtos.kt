package org.poc.objs.policy.service.web

import io.swagger.v3.oas.annotations.media.Schema
import org.poc.objs.policy.api.SuiteEvaluationResult

/**
 * Persist a suite evaluation archive (C-33).
 * [result] is typically the payload returned from `POST …/suites/evaluate`.
 * When [axes] is null, STANDARD axes are used (results + executionContext).
 * Graph scope fields rematerialize the fragment when [PersistAxesDto.input] is true.
 */
@Schema(description = "Explicit save of a suite evaluation into the archive (C-33)")
data class PersistSuiteEvaluationRequest(
    @field:Schema(description = "Evaluation payload returned by POST /policy/suites/evaluate")
    val result: SuiteEvaluationResult,
    @field:Schema(description = "Archive display name")
    val name: String? = null,
    @field:Schema(description = "Archive description")
    val description: String? = null,
    @field:Schema(description = "Archive tags, used by the list filter")
    val tags: List<String> = emptyList(),
    @field:Schema(description = "Free-form archive annotations (string key/value)")
    val annotations: Map<String, String> = emptyMap(),
    @field:Schema(description = "Explicit content axes; overrides `presetName` when set")
    val axes: PersistAxesDto? = null,
    @field:Schema(description = "Axis preset: `STANDARD` (default), `FULL`, or `EPHEMERAL`")
    val presetName: String? = null,
    @field:Schema(description = "Graph scope used to rematerialize the input when the input axis is on")
    val graphId: String? = null,
    @field:Schema(description = "Deep graph version pin for input rematerialization")
    val graphVersion: Long? = null,
    @field:Schema(description = "Matcher DSL for input rematerialization; defaults to `{ \"all\": true }`")
    val matcher: tools.jackson.databind.JsonNode? = null,
)

@Schema(description = "Which content axes are written durably; all false is rejected as EPHEMERAL")
data class PersistAxesDto(
    @field:Schema(description = "Persist policy outcomes")
    val results: Boolean = false,
    @field:Schema(description = "Persist the execution context (suite, strategies, policy roster)")
    val executionContext: Boolean = false,
    @field:Schema(description = "Persist the rematerialized input fragment")
    val input: Boolean = false,
)

@Schema(description = "Identifier of the stored evaluation archive")
data class PersistEvaluationResponse(
    @field:Schema(description = "Archive id; use it with GET /policy/evaluations/{id}")
    val evaluationId: java.util.UUID,
)

/** List wrapper for `GET /evaluations` (C-39). */
@Schema(description = "Evaluation archive list envelope (C-39)")
data class EvaluationArchiveListResponse(
    @field:Schema(description = "Archive summaries, newest first")
    val items: List<org.poc.objs.policy.api.EvaluationArchiveSummary>,
)
