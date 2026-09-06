package org.poc.objs.sbom.assessment

import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.api.domain.GraphMaterializationException
import org.poc.objs.api.match.GraphIdsMatcher
import org.poc.objs.api.store.GraphStore
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.SuiteEvaluateScope
import org.poc.objs.policy.api.SuiteEvaluationResult
import org.poc.objs.policy.api.SuiteEvaluator
import org.poc.objs.policy.api.SuiteFolderResult
import org.poc.objs.policy.api.SuiteRepository
import org.poc.objs.sbom.domain.ApplicationAssessmentResult
import org.poc.objs.sbom.domain.AssessmentDimensionColumn
import org.poc.objs.sbom.domain.AssessmentFindingView
import org.poc.objs.sbom.domain.AssessmentMatrixCell
import org.poc.objs.sbom.domain.AssessmentMatrixRow
import org.poc.objs.sbom.domain.AssessmentMeasureColumn
import org.poc.objs.sbom.domain.AssessmentOutcomeView
import org.poc.objs.sbom.domain.AssessmentSuiteSummary
import org.poc.objs.sbom.domain.PortfolioAssessmentMatrix
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.service.ApplicationBomService
import org.poc.objs.sbom.service.ApplicationVersionService
import org.poc.objs.sbom.service.PortfolioService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class AppBomTarget(
    val applicationId: UUID,
    val applicationName: String,
    val versionId: UUID?,
    val graphIds: List<UUID>,
)

/**
 * Replaceable orchestration for portfolio assessment (MVP: sequential per-app evaluate).
 * Swap implementation for true batch (C-29) without changing HTTP or UI contracts.
 */
interface PortfolioAssessmentRunner {
    fun run(suite: PolicySuite, targets: List<AppBomTarget>): PortfolioAssessmentMatrix
}

@Service
class SequentialPortfolioAssessmentRunner(
    private val assessor: ObjectProvider<SbomAssessmentService>,
) : PortfolioAssessmentRunner {
    override fun run(suite: PolicySuite, targets: List<AppBomTarget>): PortfolioAssessmentMatrix {
        val svc = assessor.getObject()
        val dimensions = svc.dimensionColumns(suite)
        val measureKeys = dimensions.flatMap { d -> d.measures.map { it.key } }.toSet()
        val rows = mutableListOf<AssessmentMatrixRow>()
        val cells = mutableListOf<AssessmentMatrixCell>()
        for (target in targets) {
            if (target.graphIds.isEmpty()) {
                rows += AssessmentMatrixRow(
                    applicationId = target.applicationId,
                    applicationName = target.applicationName,
                    error = "No BOM available",
                )
                continue
            }
            try {
                val result = svc.evaluateTarget(suite, target)
                rows += AssessmentMatrixRow(target.applicationId, target.applicationName)
                cells += svc.measureCells(suite, target.applicationId, result, measureKeys)
            } catch (ex: Exception) {
                val message =
                    when (ex) {
                        is ResponseStatusException -> ex.reason ?: ex.message
                        else -> ex.message
                    } ?: ex.javaClass.simpleName
                rows += AssessmentMatrixRow(
                    applicationId = target.applicationId,
                    applicationName = target.applicationName,
                    error = message,
                )
            }
        }
        return PortfolioAssessmentMatrix(
            suiteId = suite.id,
            suiteKey = suite.key,
            suiteName = suite.name,
            dimensions = dimensions,
            rows = rows,
            cells = cells,
        )
    }
}

@Service
class SbomAssessmentService(
    private val suites: ObjectProvider<SuiteRepository>,
    private val suiteEvaluator: ObjectProvider<SuiteEvaluator>,
    private val store: GraphStore,
    private val fragmentPolicy: GraphFragmentPolicy,
    private val applications: SbomApplicationRepository,
    private val versions: ApplicationVersionService,
    private val boms: ApplicationBomService,
    private val portfolios: PortfolioService,
    @Lazy private val runner: PortfolioAssessmentRunner,
) {
    fun listSuites(): List<AssessmentSuiteSummary> {
        val repo = requireSuites()
        return repo.list()
            .filter { it.key.startsWith("sbom-") }
            .sortedBy { it.name }
            .map { AssessmentSuiteSummary(id = it.id, key = it.key, name = it.name) }
    }

    fun runApplication(
        applicationId: UUID,
        suiteId: UUID,
        versionId: UUID?,
        bomIds: List<UUID>? = null,
    ): ApplicationAssessmentResult {
        val suite = requireSuite(suiteId)
        val target = resolveTarget(applicationId, versionId, bomIds)
        val result = evaluateTarget(suite, target)
        return toAppResult(suite, target, result)
    }

    fun runPortfolio(
        portfolioId: UUID,
        suiteId: UUID,
        applicationIds: List<UUID>?,
        level: String = "root",
        includeSubcategories: Boolean = true,
    ): PortfolioAssessmentMatrix {
        val suite = requireSuite(suiteId)
        val resolvedIds =
            if (!applicationIds.isNullOrEmpty()) {
                applicationIds.distinct()
            } else {
                portfolios.applicationsInScope(portfolioId, level, includeSubcategories)
                    .map { it.applicationId }
                    .distinct()
            }
        if (resolvedIds.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No applications in portfolio level '$level'",
            )
        }
        val targets = resolvedIds.map { resolveTarget(it, versionId = null, bomIds = null) }
        return runner.run(suite, targets)
    }

    fun evaluateTarget(suite: PolicySuite, target: AppBomTarget): SuiteEvaluationResult {
        if (target.graphIds.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No BOM graph for application ${target.applicationId}")
        }
        val evaluator = suiteEvaluator.getIfAvailable()
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Policy evaluation unavailable")
        val matcher = GraphIdsMatcher(target.graphIds)
        val contents = store.select(matcher)
        val resolved = fragmentPolicy.resolve(contents)
        if (resolved.hasErrors()) {
            throw GraphMaterializationException(
                resolved.diagnostics.joinToString("; ") { it.message },
                diagnostics = resolved.diagnostics,
            )
        }
        return evaluator.evaluateSuite(resolved, suite, SuiteEvaluateScope.Full)
    }

    fun dimensionColumns(suite: PolicySuite): List<AssessmentDimensionColumn> {
        val byParent = suite.folders.groupBy { it.parentId }
        val root = suite.folders.singleOrNull { it.parentId == null }
            ?: return emptyList()
        val dimensions = byParent[root.id].orEmpty().sortedBy { it.sortOrder }
        return dimensions.map { dim ->
            val measures = byParent[dim.id].orEmpty().sortedBy { it.sortOrder }
            AssessmentDimensionColumn(
                key = dim.key,
                name = dim.name,
                measures = measures.map { AssessmentMeasureColumn(key = it.key, name = it.name) },
            )
        }
    }

    fun measureCells(
        suite: PolicySuite,
        applicationId: UUID,
        result: SuiteEvaluationResult,
        measureKeys: Set<String>,
    ): List<AssessmentMatrixCell> {
        val measureFolders = suite.folders.filter { it.key in measureKeys }.associateBy { it.id }
        val folderResults = flattenFolderResults(result.tree).associateBy { it.folderId }
        return measureFolders.values.map { folder ->
            val fr = folderResults[folder.id]
            AssessmentMatrixCell(
                applicationId = applicationId,
                measureKey = folder.key,
                status = (fr?.status ?: PolicyOutcomeStatus.NOT_APPLICABLE).name,
                severity = fr?.severity,
            )
        }
    }

    private fun resolveTarget(
        applicationId: UUID,
        versionId: UUID?,
        bomIds: List<UUID>? = null,
    ): AppBomTarget {
        val app = applications.findById(applicationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: $applicationId")
        }
        if (versionId != null) {
            return AppBomTarget(
                applicationId = app.id,
                applicationName = app.name,
                versionId = versionId,
                graphIds = boms.graphIds(versionId, bomIds),
            )
        }
        // Prefer latest RELEASED; fall back to open DRAFT (demo apps with no release yet).
        val released = versions.latest(applicationId)
        if (released != null) {
            return AppBomTarget(
                applicationId = app.id,
                applicationName = app.name,
                versionId = released.id,
                graphIds = boms.graphIds(released.id, bomIds),
            )
        }
        val draft = versions.draft(applicationId)
        if (draft != null) {
            return AppBomTarget(
                applicationId = app.id,
                applicationName = app.name,
                versionId = draft.id,
                graphIds = boms.graphIds(draft.id, bomIds),
            )
        }
        return AppBomTarget(
            applicationId = app.id,
            applicationName = app.name,
            versionId = null,
            graphIds = emptyList(),
        )
    }

    private fun toAppResult(
        suite: PolicySuite,
        target: AppBomTarget,
        result: SuiteEvaluationResult,
    ): ApplicationAssessmentResult {
        val findings = mutableListOf<AssessmentFindingView>()
        val entitySeverities = linkedMapOf<UUID, String>()
        for (outcome in result.outcomes) {
            for (finding in outcome.findings) {
                val severity = effectiveSeverity(finding.severity, outcome.status, finding.entities.isNotEmpty())
                val view =
                    AssessmentFindingView(
                        message = finding.message,
                        severity = severity,
                        code = finding.code,
                        entityIds = finding.entities,
                        policyName = outcome.policyName,
                        outcomeStatus = outcome.status.name,
                    )
                findings += view
                for (entityId in finding.entities) {
                    val next = maxSeverity(entitySeverities[entityId], severity)
                    if (next != null) {
                        entitySeverities[entityId] = next
                    }
                }
            }
        }
        return ApplicationAssessmentResult(
            suiteId = suite.id,
            suiteKey = suite.key,
            suiteName = suite.name,
            applicationId = target.applicationId,
            applicationName = target.applicationName,
            versionId = target.versionId,
            overallStatus = result.meta.overallStatus?.name,
            overallSeverity = result.meta.overallSeverity,
            entitySeverities = entitySeverities,
            findings = findings,
            outcomes =
                result.outcomes.map { o ->
                    AssessmentOutcomeView(
                        policyName = o.policyName,
                        status = o.status.name,
                        message = o.message,
                        findings =
                            o.findings.map { f ->
                                AssessmentFindingView(
                                    message = f.message,
                                    severity = effectiveSeverity(f.severity, o.status, f.entities.isNotEmpty()),
                                    code = f.code,
                                    entityIds = f.entities,
                                    policyName = o.policyName,
                                    outcomeStatus = o.status.name,
                                )
                            },
                    )
                },
        )
    }

    /**
     * Normalize Drools scratch severities for UI:
     * - OK / INFO → INFO (tree pills)
     * - null + entity locus → ERROR when policy FAIL/ERROR, else INFO
     * - policy-level notes (no entity) keep null (not shown on tree)
     */
    private fun effectiveSeverity(
        raw: String?,
        status: PolicyOutcomeStatus,
        hasEntity: Boolean,
    ): String? {
        val key = raw?.trim()?.uppercase().orEmpty()
        return when (key) {
            "OK", "INFO" -> "INFO"
            "WARN", "WARNING" -> "WARN"
            "ERROR", "FAIL" -> "ERROR"
            "" ->
                when {
                    !hasEntity -> null
                    status == PolicyOutcomeStatus.FAIL || status == PolicyOutcomeStatus.ERROR -> "ERROR"
                    else -> "INFO"
                }
            else -> key
        }
    }

    private fun flattenFolderResults(root: SuiteFolderResult?): List<SuiteFolderResult> {
        if (root == null) return emptyList()
        val out = mutableListOf<SuiteFolderResult>()
        fun walk(node: SuiteFolderResult) {
            out += node
            node.children.forEach(::walk)
        }
        walk(root)
        return out
    }

    private fun requireSuites(): SuiteRepository =
        suites.getIfAvailable()
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Policy suites unavailable")

    private fun requireSuite(suiteId: UUID): PolicySuite =
        requireSuites().findById(suiteId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Suite not found: $suiteId")

    companion object {
        private val SEVERITY_RANK =
            mapOf(
                "ERROR" to 40,
                "FAIL" to 40,
                "WARN" to 30,
                "WARNING" to 30,
                "INFO" to 20,
                "OK" to 10,
            )

        fun maxSeverity(a: String?, b: String?): String? {
            if (a == null) return b
            if (b == null) return a
            val ra = SEVERITY_RANK[a.trim().uppercase()] ?: 5
            val rb = SEVERITY_RANK[b.trim().uppercase()] ?: 5
            return if (ra >= rb) a else b
        }
    }
}
