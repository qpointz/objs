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
 * JPA row for [org.poc.objs.policy.api.Policy] (C-28 WI-002).
 * Identity: [key] (human-managed logical identity, G-P36seed) + immutable [serial];
 * `unique(key, serial)` at the DB level. [id] is the stable row id (playground edits update
 * the same row in place, bumping [serial]).
 */
@Entity(name = "PolicyRecord")
@Table(name = "objs_policy")
class PolicyRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "`key`", nullable = false)
    var key: String = "",

    @Column(name = "serial", nullable = false)
    var serial: Long = 0L,

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "engine_kind", nullable = false)
    var engineKind: String = "",

    @Column(name = "body", nullable = false)
    var body: String = "",

    @Column(name = "content_type")
    var contentType: String? = null,

    @Column(name = "applicability_kind")
    var applicabilityKind: String? = null,

    @Column(name = "applicability_body")
    var applicabilityBody: String? = null,

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "json")
    var tags: MutableList<String> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),

    @Column(name = "version", nullable = false)
    var version: String = "0.1",

    @Column(name = "description", nullable = false)
    var description: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
