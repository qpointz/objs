package org.poc.objs.policy.service

import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.api.domain.GraphMaterializationException
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.match.Matcher
import org.poc.objs.api.store.GraphStore
import org.poc.objs.policy.api.ApplicabilityKinds
import org.poc.objs.policy.api.EvaluationResult
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.api.PolicyEvaluator
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.drools.PolicyKnowledgeBaseCache
import org.springframework.stereotype.Service
import java.util.UUID

data class PolicyCheckIssue(
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
)

data class PolicyCheckResult(
    val ok: Boolean,
    /** Structured diagnostics (preferred). */
    val issues: List<PolicyCheckIssue> = emptyList(),
    /** Flat display strings (compat / convenience). */
    val messages: List<String> = emptyList(),
)

@Service
class PolicyPlayService(
    private val store: GraphStore,
    private val fragmentPolicy: GraphFragmentPolicy,
    private val repository: PolicyRepository,
    private val evaluator: PolicyEvaluator,
    private val knowledgeBaseCache: PolicyKnowledgeBaseCache,
) {
    fun capabilities(): PolicyCapabilities =
        PolicyCapabilities(
            engines = listOf(PolicyEngineKinds.DROOLS),
            operations = listOf("list", "create", "update", "delete", "check", "evaluate"),
        )

    fun list(): List<Policy> = repository.list()

    fun get(id: UUID): Policy? = repository.findById(id)

    fun create(write: PolicyWrite): Policy = repository.save(normalizeWrite(write))

    fun update(id: UUID, write: PolicyWrite): Policy? {
        val updated = repository.update(id, normalizeWrite(write)) ?: return null
        knowledgeBaseCache.invalidate(id)
        return updated
    }

    fun delete(id: UUID): Boolean {
        val removed = repository.delete(id)
        if (removed) knowledgeBaseCache.invalidate(id)
        return removed
    }

    fun check(body: String, engineKind: String = PolicyEngineKinds.DROOLS): PolicyCheckResult {
        if (engineKind != PolicyEngineKinds.DROOLS) {
            val issue = PolicyCheckIssue(message = "Only DROOLS check is supported")
            return PolicyCheckResult(ok = false, issues = listOf(issue), messages = listOf(issue.message))
        }
        if (body.isBlank()) {
            val issue = PolicyCheckIssue(message = "DROOLS body is empty")
            return PolicyCheckResult(ok = false, issues = listOf(issue), messages = listOf(issue.message))
        }
        val probe = Policy(
            id = UUID.randomUUID(),
            name = "check",
            version = 1L,
            engineKind = PolicyEngineKinds.DROOLS,
            body = body,
            applicabilityKind = ApplicabilityKinds.ALWAYS_APPLY,
        )
        val compiled = knowledgeBaseCache.tryCompile(probe)
        val issues = compiled.map {
            PolicyCheckIssue(message = it.message, line = it.line, column = it.column)
        }
        return PolicyCheckResult(
            ok = issues.isEmpty(),
            issues = issues,
            messages = compiled.map { it.display() },
        )
    }

    fun evaluate(
        matcher: Matcher,
        graphId: UUID?,
        graphVersion: Long?,
        policyId: UUID?,
        body: String?,
        engineKind: String?,
        policyName: String?,
    ): EvaluationResult {
        val contents = selectContents(matcher, graphId, graphVersion)
        val resolved = fragmentPolicy.resolve(contents)
        if (resolved.hasErrors()) {
            throw GraphMaterializationException(
                resolved.diagnostics.joinToString("; ") { it.message },
                diagnostics = resolved.diagnostics,
            )
        }

        val policy = resolveEvaluatePolicy(policyId, body, engineKind, policyName)
        return evaluator.evaluatePolicies(resolved, listOf(policy))
    }

    private fun resolveEvaluatePolicy(
        policyId: UUID?,
        body: String?,
        engineKind: String?,
        policyName: String?,
    ): Policy {
        if (policyId != null) {
            val stored = repository.findById(policyId)
                ?: throw IllegalArgumentException("Policy not found: $policyId")
            if (body == null) return stored
            return stored.copy(body = body, engineKind = engineKind ?: stored.engineKind)
        }
        require(!body.isNullOrBlank()) { "body or policyId is required" }
        return Policy(
            id = UUID.randomUUID(),
            name = policyName?.takeIf { it.isNotBlank() } ?: "ephemeral",
            version = 0L,
            engineKind = engineKind ?: PolicyEngineKinds.DROOLS,
            body = body,
            applicabilityKind = ApplicabilityKinds.ALWAYS_APPLY,
        )
    }

    private fun normalizeWrite(write: PolicyWrite): PolicyWrite =
        write.copy(
            engineKind = write.engineKind.ifBlank { PolicyEngineKinds.DROOLS },
            applicabilityKind = write.applicabilityKind ?: ApplicabilityKinds.ALWAYS_APPLY,
        )

    private fun selectContents(
        matcher: Matcher,
        graphId: UUID?,
        graphVersion: Long?,
    ): GraphContents =
        when {
            graphId != null && graphVersion != null ->
                store.selectInGraphVersion(graphId, graphVersion, matcher)
            graphId != null ->
                store.selectInGraph(graphId, matcher)
            else ->
                store.select(matcher)
        }
}
