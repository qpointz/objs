package org.poc.objs.policy.service.web

import org.poc.objs.policy.api.SuiteEvaluationResult

/**
 * Persist a suite evaluation archive (C-33).
 * [result] is typically the payload returned from `POST …/suites/evaluate`.
 * When [axes] is null, STANDARD axes are used (results + executionContext).
 * Graph scope fields rematerialize the fragment when [PersistAxesDto.input] is true.
 */
data class PersistSuiteEvaluationRequest(
    val result: SuiteEvaluationResult,
    val name: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val annotations: Map<String, String> = emptyMap(),
    val axes: PersistAxesDto? = null,
    val presetName: String? = null,
    val graphId: String? = null,
    val graphVersion: Long? = null,
    val matcher: tools.jackson.databind.JsonNode? = null,
)

data class PersistAxesDto(
    val results: Boolean = false,
    val executionContext: Boolean = false,
    val input: Boolean = false,
)

data class PersistEvaluationResponse(
    val evaluationId: java.util.UUID,
)

/** List wrapper for `GET /evaluations` (C-39). */
data class EvaluationArchiveListResponse(
    val items: List<org.poc.objs.policy.api.EvaluationArchiveSummary>,
)
