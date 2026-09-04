package org.poc.objs.policy.drools

import org.poc.objs.policy.api.Finding
import org.poc.objs.policy.api.PolicyEngineResult
import org.poc.objs.policy.api.PolicyOutcomeStatus
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Per-session scratch bound as Drools `global`. Default status is [PolicyOutcomeStatus.PASS].
 */
class DroolsEvaluationScratch {
    @Volatile
    var status: PolicyOutcomeStatus = PolicyOutcomeStatus.PASS
        private set

    private val findings = CopyOnWriteArrayList<Finding>()

    fun fail(message: String) {
        if (status != PolicyOutcomeStatus.ERROR) {
            status = PolicyOutcomeStatus.FAIL
        }
        findings += Finding(message = message)
    }

    fun error(message: String) {
        status = PolicyOutcomeStatus.ERROR
        findings += Finding(message = message, severity = "ERROR")
    }

    fun finding(
        message: String,
        severity: String? = null,
        code: String? = null,
        entityId: String? = null,
        edgeId: String? = null,
    ) {
        findings += Finding(
            message = message,
            severity = severity,
            code = code,
            entities = listOfNotNull(parseUuid(entityId)),
            edges = listOfNotNull(parseUuid(edgeId)),
        )
    }

    fun toResult(message: String? = null): PolicyEngineResult =
        PolicyEngineResult(
            status = status,
            findings = findings.toList(),
            message = message,
        )

    private fun parseUuid(raw: String?): UUID? =
        raw?.takeIf { it.isNotBlank() }?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}
