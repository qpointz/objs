package org.poc.objs.core.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.util.UUID

/**
 * Graph header — id + free-form annotations (C-13: bom_graph).
 */
@Entity
@Table(name = "bom_graph")
class BoMGraphRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),
)

data class BoMGraphMembershipId(
    var graphId: UUID = UUID.randomUUID(),
    var entityId: UUID = UUID.randomUUID(),
) : Serializable

/** Membership M2M: graph ↔ entity (C-13: bom_graph_entity). */
@Entity
@Table(name = "bom_graph_entity")
@IdClass(BoMGraphMembershipId::class)
class BoMGraphMembershipRecord(
    @Id
    @Column(name = "graph_id", nullable = false)
    var graphId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "entity_id", nullable = false)
    var entityId: UUID = UUID.randomUUID(),
)
