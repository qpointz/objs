package org.poc.objs.core.persistence.policy

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * JPA row for [org.poc.objs.policy.api.PolicySuite] (C-27 / C-28 WI-002). [key] is the
 * human-managed logical identity (G-P36seed); [name] is a separate display label.
 */
@Entity(name = "PolicySuiteRecord")
@Table(name = "objs_policy_suite")
class PolicySuiteRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "`key`", nullable = false)
    var key: String = "",

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "roll_up_strategy_kind", nullable = false)
    var rollUpStrategyKind: String = "BUILTIN",

    @Column(name = "execution_strategy_kind", nullable = false)
    var executionStrategyKind: String = "DEDUPE",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "json")
    var tags: MutableList<String> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

/**
 * JPA row for [org.poc.objs.policy.api.SuiteFolder]. [matchers] is a JSON array of
 * `{kind, ...}` maps (see `SuiteMatcherJson.kt`) — [org.poc.objs.policy.api.SuiteMatcher] is a
 * sealed class family so it does not map cleanly to further normalized columns/tables.
 */
@Entity(name = "PolicySuiteFolderRecord")
@Table(name = "objs_policy_suite_folder")
class PolicySuiteFolderRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "suite_id", nullable = false)
    var suiteId: UUID = UUID.randomUUID(),

    @Column(name = "`key`", nullable = false)
    var key: String = "",

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "parent_id")
    var parentId: UUID? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "participation", nullable = false)
    var participation: String = "ENABLED",

    @Column(name = "roll_up_mode", nullable = false)
    var rollUpMode: String = "ALL_PASS",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matchers", nullable = false, columnDefinition = "json")
    var matchers: MutableList<MutableMap<String, Any?>> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "json")
    var tags: MutableList<String> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),

    @Column(name = "severity_config")
    var severityConfig: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
