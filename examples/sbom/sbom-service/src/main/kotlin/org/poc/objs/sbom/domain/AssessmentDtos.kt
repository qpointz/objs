package org.poc.objs.sbom.domain

import java.util.UUID

data class AssessmentSuiteSummary(
    val id: UUID,
    val key: String,
    val name: String,
)

data class ApplicationAssessmentRunRequest(
    val suiteId: UUID,
    val versionId: UUID? = null,
    /** When set, evaluate only these BOM graphs (multi-BOM selection). Null/empty = all BOMs for the version. */
    val bomIds: List<UUID>? = null,
)

data class AssessmentFindingView(
    val message: String,
    val severity: String?,
    val code: String?,
    val entityIds: List<UUID> = emptyList(),
    val policyName: String? = null,
    val outcomeStatus: String? = null,
)

data class ApplicationAssessmentResult(
    val suiteId: UUID,
    val suiteKey: String,
    val suiteName: String,
    val applicationId: UUID,
    val applicationName: String,
    val versionId: UUID?,
    val overallStatus: String?,
    val overallSeverity: String?,
    /** Worst finding severity per entity id (ERROR > WARN > …). */
    val entitySeverities: Map<UUID, String> = emptyMap(),
    val findings: List<AssessmentFindingView> = emptyList(),
    val outcomes: List<AssessmentOutcomeView> = emptyList(),
)

data class AssessmentOutcomeView(
    val policyName: String,
    val status: String,
    val message: String? = null,
    val findings: List<AssessmentFindingView> = emptyList(),
)

data class PortfolioAssessmentRunRequest(
    val suiteId: UUID,
    /** When null/empty, assess all applications in [level] (portfolio tree selection). */
    val applicationIds: List<UUID>? = null,
    val level: String = "root",
    val includeSubcategories: Boolean = true,
)

data class AssessmentDimensionColumn(
    val key: String,
    val name: String,
    val measures: List<AssessmentMeasureColumn>,
)

data class AssessmentMeasureColumn(
    val key: String,
    val name: String,
)

data class AssessmentMatrixCell(
    val applicationId: UUID,
    val measureKey: String,
    val status: String,
    val severity: String? = null,
)

data class AssessmentMatrixRow(
    val applicationId: UUID,
    val applicationName: String,
    val error: String? = null,
)

data class PortfolioAssessmentMatrix(
    val suiteId: UUID,
    val suiteKey: String,
    val suiteName: String,
    val dimensions: List<AssessmentDimensionColumn>,
    val rows: List<AssessmentMatrixRow>,
    val cells: List<AssessmentMatrixCell>,
)
