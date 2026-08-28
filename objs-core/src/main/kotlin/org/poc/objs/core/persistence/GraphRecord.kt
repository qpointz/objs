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

/**
 * Graph header — id + free-form annotations (C-13: objs_graph).
 */
@Entity(name = "BoMGraphRecord")
@Table(name = "objs_graph")
class GraphRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

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

data class GraphMembershipId(
    var graphId: UUID = UUID.randomUUID(),
    var entityId: UUID = UUID.randomUUID(),
) : Serializable

/** Membership M2M: graph ↔ entity (C-13: objs_graph_entity). */
@Entity(name = "BoMGraphMembershipRecord")
@Table(name = "objs_graph_entity")
@IdClass(GraphMembershipId::class)
class GraphMembershipRecord(
    @Id
    @Column(name = "graph_id", nullable = false)
    var graphId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "entity_id", nullable = false)
    var entityId: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
