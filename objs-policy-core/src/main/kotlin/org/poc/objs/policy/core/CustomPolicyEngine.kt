package org.poc.objs.policy.core

import org.poc.objs.policy.api.Finding
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyEngine
import org.poc.objs.policy.api.PolicyEngineResult
import org.poc.objs.policy.api.PolicyEvaluationContext
import org.poc.objs.policy.api.PolicyOutcomeStatus
import java.util.UUID

/**
 * CUSTOM stub engine for tests and embedders.
 *
 * Body protocol (first line = status token, case-insensitive):
 * - `PASS` / `FAIL` / `ERROR`
 * - Optional following lines: `FINDING|<message>|<entityUuid?>|<edgeUuid?>`
 */
object CustomPolicyEngine : PolicyEngine {
    override fun evaluate(context: PolicyEvaluationContext, policy: Policy): PolicyEngineResult {
        val lines = policy.body.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return PolicyEngineResult(
                status = PolicyOutcomeStatus.ERROR,
                message = "CUSTOM body is empty",
            )
        }

        val statusToken = lines.first().uppercase()
        val status = when (statusToken) {
            "PASS" -> PolicyOutcomeStatus.PASS
            "FAIL" -> PolicyOutcomeStatus.FAIL
            "ERROR" -> PolicyOutcomeStatus.ERROR
            else -> {
                return PolicyEngineResult(
                    status = PolicyOutcomeStatus.ERROR,
                    message = "Unrecognized CUSTOM body status '$statusToken'",
                )
            }
        }

        val findings = lines.drop(1).mapNotNull { parseFinding(it) }
        return PolicyEngineResult(
            status = status,
            findings = findings,
            message = if (status == PolicyOutcomeStatus.ERROR) "CUSTOM engine ERROR body" else null,
        )
    }

    private fun parseFinding(line: String): Finding? {
        if (!line.startsWith("FINDING|", ignoreCase = true)) {
            return null
        }
        val parts = line.split('|')
        val message = parts.getOrNull(1)?.ifBlank { "finding" } ?: "finding"
        val entity = parts.getOrNull(2)?.takeIf { it.isNotBlank() }?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val edge = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        return Finding(
            message = message,
            entities = listOfNotNull(entity),
            edges = listOfNotNull(edge),
        )
    }
}
