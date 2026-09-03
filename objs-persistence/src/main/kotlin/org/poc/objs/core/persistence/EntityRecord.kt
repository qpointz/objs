package org.poc.objs.core.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * JPA row for [org.poc.objs.api.domain.Entity] — generic columns + JSON payload/annotations.
 */
@Entity(name = "BoMEntityRecord")
@Table(name = "objs_entity")
class EntityRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "type", nullable = false)
    var type: String = "",

    @Column(name = "schema_version", nullable = false)
    var schemaVersion: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "json")
    var payload: MutableMap<String, Any?> = mutableMapOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "head_version")
    var headVersion: Long? = null,
)

/**
 * JPA row for [org.poc.objs.api.domain.Edge].
 */
@Entity(name = "BoMEdgeRecord")
@Table(name = "objs_graph_edge")
class EdgeRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "graph_id", nullable = false)
    var graphId: UUID = UUID.randomUUID(),

    @Column(name = "source_id", nullable = false)
    var sourceId: UUID = UUID.randomUUID(),

    @Column(name = "target_id", nullable = false)
    var targetId: UUID = UUID.randomUUID(),

    @Column(name = "role", nullable = false)
    var role: String = "",

    @Column(name = "type")
    var type: String? = null,

    @Column(name = "schema_version")
    var schemaVersion: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "json")
    var properties: MutableMap<String, Any?>? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "head_version")
    var headVersion: Long? = null,
)
