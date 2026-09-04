package org.poc.objs.policy.drools

import org.poc.objs.policy.api.Finding
import org.poc.objs.policy.api.PolicyEngineResult
import org.poc.objs.policy.api.PolicyOutcomeStatus
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.jvm.JvmOverloads

/**
 * Per-session scratch bound as Drools `global`. Default status is [PolicyOutcomeStatus.PASS].
 *
 * While a rule is firing, [DroolsPolicyEngine] sets [currentRuleName] via an agenda listener so
 * [fail] / [error] / [finding] / [pass] automatically attach `extras.rule` (and a `[rule]` message prefix).
 *
 * [@JvmOverloads] is required so Drools-generated Java can call single-arg forms.
 */
class DroolsEvaluationScratch {
    @Volatile
    var status: PolicyOutcomeStatus = PolicyOutcomeStatus.PASS
        private set

    /**
     * Name of the rule currently firing (set by the engine agenda listener).
     * DRL can also pass an explicit [rule] argument to override.
     */
    @Volatile
    var currentRuleName: String? = null

    private val findings = CopyOnWriteArrayList<Finding>()

    @JvmOverloads
    fun fail(message: String, rule: String? = null) {
        if (status != PolicyOutcomeStatus.ERROR) {
            status = PolicyOutcomeStatus.FAIL
        }
        findings += findingOf(message = message, severity = null, code = null, rule = rule)
    }

    @JvmOverloads
    fun error(message: String, rule: String? = null) {
        status = PolicyOutcomeStatus.ERROR
        findings += findingOf(message = message, severity = "ERROR", code = null, rule = rule)
    }

    @JvmOverloads
    fun finding(
        message: String,
        severity: String? = null,
        code: String? = null,
        entityId: String? = null,
        edgeId: String? = null,
        rule: String? = null,
    ) {
        findings += findingOf(
            message = message,
            severity = severity,
            code = code,
            entityId = entityId,
            edgeId = edgeId,
            rule = rule,
        )
    }

    /** PASS-path note (does not change status). Surfaces the firing rule on PASS. */
    @JvmOverloads
    fun pass(message: String = "ok", rule: String? = null) {
        findings += findingOf(
            message = message,
            severity = "OK",
            code = null,
            rule = rule,
        )
    }

    fun toResult(message: String? = null): PolicyEngineResult =
        PolicyEngineResult(
            status = status,
            findings = findings.toList(),
            message = message,
        )

    private fun findingOf(
        message: String,
        severity: String?,
        code: String?,
        entityId: String? = null,
        edgeId: String? = null,
        rule: String?,
    ): Finding {
        val resolved = rule?.takeIf { it.isNotBlank() } ?: currentRuleName?.takeIf { it.isNotBlank() }
        val display =
            if (!resolved.isNullOrBlank() && !message.contains(resolved)) {
                "[$resolved] $message"
            } else {
                message
            }
        return Finding(
            message = display,
            severity = severity,
            code = code,
            entities = listOfNotNull(parseUuid(entityId)),
            edges = listOfNotNull(parseUuid(edgeId)),
            extras = if (resolved.isNullOrBlank()) emptyMap() else mapOf(EXTRA_RULE to resolved),
        )
    }

    private fun parseUuid(raw: String?): UUID? =
        raw?.takeIf { it.isNotBlank() }?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    companion object {
        const val EXTRA_RULE: String = "rule"
    }
}
