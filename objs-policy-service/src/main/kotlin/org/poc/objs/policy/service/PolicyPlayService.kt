package org.poc.objs.policy.service

import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.api.domain.GraphMaterializationException
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.match.Matcher
import org.poc.objs.api.store.GraphStore
import org.poc.objs.policy.api.ApplicabilityKinds
import org.poc.objs.policy.api.Category
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.EvaluationArchive
import org.poc.objs.policy.api.EvaluationResult
import org.poc.objs.policy.api.ExecutionContextSnapshot
import org.poc.objs.policy.api.PersistSpec
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.PolicySuiteWrite
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.api.PolicyEvaluator
import org.poc.objs.policy.api.PolicyQuery
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.SuiteEffectiveSet
import org.poc.objs.policy.api.SuiteEvaluateScope
import org.poc.objs.policy.api.SuiteEvaluationResult
import org.poc.objs.policy.api.SuiteEvaluator
import org.poc.objs.policy.api.SuiteRepository
import org.poc.objs.policy.drools.PolicyKnowledgeBaseCache
import org.poc.objs.policy.service.seed.PolicyCatalogSeedExporter
import org.springframework.stereotype.Service
import java.util.Optional
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
    private val categories: CategoryRepository,
    private val suites: SuiteRepository,
    private val evaluator: PolicyEvaluator,
    private val suiteEvaluator: SuiteEvaluator,
    private val knowledgeBaseCache: PolicyKnowledgeBaseCache,
    private val catalogExporter: PolicyCatalogSeedExporter,
    private val evaluationArchive: Optional<EvaluationArchive> = Optional.empty(),
) {
    fun capabilities(): PolicyCapabilities {
        val ops =
            mutableListOf(
                "list",
                "create",
                "update",
                "delete",
                "check",
                "evaluate",
                "categories",
                "query",
                "suites",
                "evaluateSuite",
                "suiteSelection",
                "export",
            )
        if (evaluationArchive.isPresent) {
            ops.add("archive")
        }
        return PolicyCapabilities(
            engines = listOf(PolicyEngineKinds.DROOLS),
            operations = ops,
        )
    }

    /** Full catalog REPLACE seed YAML (WI-005). */
    fun exportCatalogSeeds(): String = catalogExporter.exportReplaceYaml()

    fun list(query: PolicyQuery = PolicyQuery()): List<Policy> =
        if (query == PolicyQuery()) repository.list() else repository.query(query)

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

    fun listCategories(): List<Category> = categories.list()

    fun getCategory(id: UUID): Category? = categories.findById(id)

    fun createCategory(write: CategoryWrite): Category = categories.save(write)

    fun updateCategory(id: UUID, write: CategoryWrite): Category? = categories.update(id, write)

    fun deleteCategory(id: UUID): Boolean = categories.delete(id)

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
            key = "check",
            name = "check",
            serial = 1L,
            engineKind = PolicyEngineKinds.DROOLS,
            body = body,
            applicabilityKind = ApplicabilityKinds.ALWAYS_APPLY,
            categoryId = UUID(0L, 0L),
            tags = listOf("check"),
            version = "0.1",
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

    fun listSuites(): List<PolicySuite> = suites.list()

    fun getSuite(id: UUID): PolicySuite? = suites.findById(id)

    fun createSuite(write: PolicySuiteWrite): PolicySuite = suites.save(write)

    fun updateSuite(id: UUID, write: PolicySuiteWrite): PolicySuite? = suites.update(id, write)

    fun deleteSuite(id: UUID): Boolean = suites.delete(id)

    fun suiteSelection(suiteId: UUID, scope: SuiteEvaluateScope): SuiteEffectiveSet {
        val suite = suites.findById(suiteId)
            ?: throw IllegalArgumentException("Suite not found: $suiteId")
        return suiteEvaluator.resolveSelection(suite, scope)
    }

    fun evaluateSuite(
        matcher: Matcher,
        graphId: UUID?,
        graphVersion: Long?,
        suiteId: UUID,
        scope: SuiteEvaluateScope,
    ): SuiteEvaluationResult {
        val suite = suites.findById(suiteId)
            ?: throw IllegalArgumentException("Suite not found: $suiteId")
        val contents = selectContents(matcher, graphId, graphVersion)
        val resolved = fragmentPolicy.resolve(contents)
        if (resolved.hasErrors()) {
            throw GraphMaterializationException(
                resolved.diagnostics.joinToString("; ") { it.message },
                diagnostics = resolved.diagnostics,
            )
        }
        return suiteEvaluator.evaluateSuite(resolved, suite, scope)
    }

    /**
     * Persist a suite evaluation via [EvaluationArchive.saveSuite].
     * Returns null when axes are EPHEMERAL (no durable write).
     */
    fun saveSuiteEvaluation(
        result: SuiteEvaluationResult,
        spec: PersistSpec,
        executionContext: ExecutionContextSnapshot? = null,
        input: org.poc.objs.api.domain.GraphFragment? = null,
    ): UUID? {
        val archive =
            evaluationArchive.orElseThrow {
                IllegalStateException("Evaluation archive is not available (persistence not configured)")
            }
        return archive.saveSuite(
            result = result,
            spec = spec,
            executionContext = executionContext,
            input = input,
        )
    }

    fun resolveFragment(
        matcher: Matcher,
        graphId: UUID?,
        graphVersion: Long?,
    ): org.poc.objs.api.domain.GraphFragment {
        val contents = selectContents(matcher, graphId, graphVersion)
        val resolved = fragmentPolicy.resolve(contents)
        if (resolved.hasErrors()) {
            throw GraphMaterializationException(
                resolved.diagnostics.joinToString("; ") { it.message },
                diagnostics = resolved.diagnostics,
            )
        }
        return resolved
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
        val label = policyName?.takeIf { it.isNotBlank() } ?: "ephemeral"
        return Policy(
            id = UUID.randomUUID(),
            key = label,
            name = label,
            serial = 0L,
            engineKind = engineKind ?: PolicyEngineKinds.DROOLS,
            body = body,
            applicabilityKind = ApplicabilityKinds.ALWAYS_APPLY,
            categoryId = UUID(0L, 0L),
            tags = listOf("ephemeral"),
            version = "0.1",
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
