package org.poc.objs.policy.drools

import org.kie.api.event.rule.BeforeMatchFiredEvent
import org.kie.api.event.rule.DefaultAgendaEventListener
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyEngine
import org.poc.objs.policy.api.PolicyEngineResult
import org.poc.objs.policy.api.PolicyEvaluationContext
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.kie.api.runtime.KieSession

/**
 * Drools [PolicyEngine]: [EntityFact]/[EdgeFact]/[ObjectFact] facts, per-call session,
 * KB cache by policy id (G-P18–20).
 */
class DroolsPolicyEngine(
    private val knowledgeBaseCache: PolicyKnowledgeBaseCache = PolicyKnowledgeBaseCache(),
) : PolicyEngine {

    override fun evaluate(context: PolicyEvaluationContext, policy: Policy): PolicyEngineResult {
        if (policy.body.isBlank()) {
            return PolicyEngineResult(
                status = PolicyOutcomeStatus.ERROR,
                message = "DROOLS body is empty",
            )
        }

        val container = try {
            knowledgeBaseCache.containerFor(policy)
        } catch (ex: Exception) {
            return PolicyEngineResult(
                status = PolicyOutcomeStatus.ERROR,
                message = sanitizeDroolsMessage(
                    ex.message ?: ex.cause?.message ?: "Drools compile failed",
                ),
            )
        }

        val scratch = DroolsEvaluationScratch()
        val session = container.newKieSession()
        return try {
            session.setGlobal("scratch", scratch)
            session.addEventListener(RuleNameAgendaListener(scratch))
            insertFacts(session, context)
            session.fireAllRules()
            scratch.toResult()
        } catch (ex: Exception) {
            PolicyEngineResult(
                status = PolicyOutcomeStatus.ERROR,
                message = sanitizeDroolsMessage(ex.message ?: ex::class.simpleName ?: "Drools error"),
            )
        } finally {
            session.dispose()
        }
    }

    private fun insertFacts(session: KieSession, context: PolicyEvaluationContext) {
        context.fragment.entities.forEach { session.insert(EntityFact.from(it)) }
        context.fragment.edges.forEach { session.insert(EdgeFact.from(it)) }
        context.facts.forEach { (name, value) ->
            session.insert(toObjectFact(name, value))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toObjectFact(name: String, value: Any?): ObjectFact =
        when (value) {
            is ObjectFact -> value
            is EntityFact, is EdgeFact ->
                ObjectFact(name, mapOf("value" to value))
            is Map<*, *> -> ObjectFact(name, value as Map<String, Any?>)
            null -> ObjectFact(name, emptyMap())
            else -> ObjectFact(name, mapOf("value" to value))
        }

    /** Captures the firing rule name onto [scratch] for finding attribution. */
    private class RuleNameAgendaListener(
        private val scratch: DroolsEvaluationScratch,
    ) : DefaultAgendaEventListener() {
        override fun beforeMatchFired(event: BeforeMatchFiredEvent) {
            scratch.currentRuleName = event.match?.rule?.name
        }

        override fun afterMatchFired(event: org.kie.api.event.rule.AfterMatchFiredEvent) {
            scratch.currentRuleName = null
        }
    }
}
