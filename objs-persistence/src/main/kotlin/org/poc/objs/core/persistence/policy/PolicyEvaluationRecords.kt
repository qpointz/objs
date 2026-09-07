package org.poc.objs.core.persistence.policy

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/** JPA row for evaluation archives (C-33). No FK to catalog tables. */
@Entity(name = "PolicyEvaluationRecord")
@Table(name = "objs_policy_evaluation")
class PolicyEvaluationRecord(
    @Id
    @Column(name = "evaluation_id", nullable = false)
    var evaluationId: UUID = UUID.randomUUID(),

    @Column(name = "kind", nullable = false)
    var kind: String = "",

    @Column(name = "evaluated_at", nullable = false)
    var evaluatedAt: Instant = Instant.now(),

    @Column(name = "execution_strategy_kind")
    var executionStrategyKind: String? = null,

    @Column(name = "rollup_strategy_kind")
    var rollupStrategyKind: String? = null,

    @Column(name = "overall_status")
    var overallStatus: String? = null,

    @Column(name = "overall_severity")
    var overallSeverity: String? = null,

    @Column(name = "suite_id")
    var suiteId: UUID? = null,

    @Column(name = "suite_name")
    var suiteName: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "json")
    var tags: MutableList<String> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),

    /**
     * Extensible persist options (axes, filters, preset, labeling, runtime, …).
     * Known keys today — see [PersistProfileKeys]; unknown keys are preserved for forward compatibility.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "persist_profile", nullable = false, columnDefinition = "json")
    var persistProfile: MutableMap<String, Any?> = mutableMapOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suite_tree", columnDefinition = "json")
    var suiteTree: MutableMap<String, Any?>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_context", columnDefinition = "json")
    var executionContext: MutableMap<String, Any?>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_fragment", columnDefinition = "json")
    var inputFragment: MutableMap<String, Any?>? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

/** Documented keys inside [PolicyEvaluationRecord.persistProfile] (open for extension). */
object PersistProfileKeys {
    const val PRESET_NAME = "presetName"
    const val AXES = "axes"
    const val FILTERS = "filters"
    const val NAME = "name"
    const val DESCRIPTION = "description"
    const val ORIGIN = "origin"
    const val DURATION_MS = "durationMs"

    const val AXIS_RESULTS = "results"
    const val AXIS_EXECUTION_CONTEXT = "executionContext"
    const val AXIS_INPUT = "input"

    const val FILTER_OUTCOME_STATUSES = "outcomeStatuses"
    const val FILTER_FINDING_SEVERITIES = "findingSeverities"
}

@Entity(name = "PolicyOutcomeRecord")
@Table(name = "objs_policy_outcome")
class PolicyOutcomeRecord(
    @Id
    @Column(name = "outcome_id", nullable = false)
    var outcomeId: UUID = UUID.randomUUID(),

    @Column(name = "evaluation_id", nullable = false)
    var evaluationId: UUID = UUID.randomUUID(),

    @Column(name = "ordinal", nullable = false)
    var ordinal: Int = 0,

    @Column(name = "policy_name", nullable = false)
    var policyName: String = "",

    @Column(name = "policy_serial", nullable = false)
    var policySerial: Long = 0L,

    @Column(name = "engine_kind", nullable = false)
    var engineKind: String = "",

    @Column(name = "status", nullable = false)
    var status: String = "",

    @Column(name = "not_applicable_reason")
    var notApplicableReason: String? = null,

    @Column(name = "message")
    var message: String? = null,
)

@Entity(name = "PolicyFindingRecord")
@Table(name = "objs_policy_finding")
class PolicyFindingRecord(
    @Id
    @Column(name = "finding_id", nullable = false)
    var findingId: UUID = UUID.randomUUID(),

    @Column(name = "outcome_id", nullable = false)
    var outcomeId: UUID = UUID.randomUUID(),

    @Column(name = "idx", nullable = false)
    var idx: Int = 0,

    @Column(name = "message", nullable = false)
    var message: String = "",

    @Column(name = "severity")
    var severity: String? = null,

    @Column(name = "code")
    var code: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entity_ids", nullable = false, columnDefinition = "json")
    var entityIds: MutableList<String> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edge_ids", nullable = false, columnDefinition = "json")
    var edgeIds: MutableList<String> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extras", nullable = false, columnDefinition = "json")
    var extras: MutableMap<String, Any?> = mutableMapOf(),
)
