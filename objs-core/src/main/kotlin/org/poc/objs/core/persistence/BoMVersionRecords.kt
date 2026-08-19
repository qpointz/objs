package org.poc.objs.core.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class BoMEntityVersionId(
    var entityId: UUID = UUID.randomUUID(),
    var version: Long = 0,
) : Serializable

@Entity
@Table(name = "bom_entity_version")
@IdClass(BoMEntityVersionId::class)
class BoMEntityVersionRecord(
    @Id
    @Column(name = "entity_id", nullable = false)
    var entityId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "version", nullable = false)
    var version: Long = 0,

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

    @Column(name = "head_deleted_at")
    var headDeletedAt: Instant? = null,
)

data class BoMGraphVersionId(
    var graphId: UUID = UUID.randomUUID(),
    var version: Long = 0,
) : Serializable

@Entity
@Table(name = "bom_graph_version")
@IdClass(BoMGraphVersionId::class)
class BoMGraphVersionRecord(
    @Id
    @Column(name = "graph_id", nullable = false)
    var graphId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "graph_annotations", nullable = false, columnDefinition = "json")
    var graphAnnotations: MutableMap<String, String> = mutableMapOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "head_deleted_at")
    var headDeletedAt: Instant? = null,
)

data class BoMEdgeVersionId(
    var edgeId: UUID = UUID.randomUUID(),
    var version: Long = 0,
) : Serializable

@Entity
@Table(name = "bom_graph_edge_version")
@IdClass(BoMEdgeVersionId::class)
class BoMEdgeVersionRecord(
    @Id
    @Column(name = "edge_id", nullable = false)
    var edgeId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "version", nullable = false)
    var version: Long = 0,

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

    @Column(name = "head_deleted_at")
    var headDeletedAt: Instant? = null,
)

data class BoMGraphVersionMemberId(
    var graphId: UUID = UUID.randomUUID(),
    var graphVersion: Long = 0,
    var entityId: UUID = UUID.randomUUID(),
) : Serializable

@Entity
@Table(name = "bom_graph_version_member")
@IdClass(BoMGraphVersionMemberId::class)
class BoMGraphVersionMemberRecord(
    @Id
    @Column(name = "graph_id", nullable = false)
    var graphId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "graph_version", nullable = false)
    var graphVersion: Long = 0,

    @Id
    @Column(name = "entity_id", nullable = false)
    var entityId: UUID = UUID.randomUUID(),

    @Column(name = "entity_version", nullable = false)
    var entityVersion: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

data class BoMGraphVersionEdgeId(
    var graphId: UUID = UUID.randomUUID(),
    var graphVersion: Long = 0,
    var edgeId: UUID = UUID.randomUUID(),
) : Serializable

@Entity
@Table(name = "bom_graph_version_edge")
@IdClass(BoMGraphVersionEdgeId::class)
class BoMGraphVersionEdgeRecord(
    @Id
    @Column(name = "graph_id", nullable = false)
    var graphId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "graph_version", nullable = false)
    var graphVersion: Long = 0,

    @Id
    @Column(name = "edge_id", nullable = false)
    var edgeId: UUID = UUID.randomUUID(),

    @Column(name = "edge_version", nullable = false)
    var edgeVersion: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
