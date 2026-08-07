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
 * Soft-link subgraph header — id + free-form annotations (same shape as entity annotations).
 */
@Entity
@Table(name = "bom_subgraph")
class BoMSubgraphRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),
)

data class BoMSubgraphEntityId(
    var subgraphId: UUID = UUID.randomUUID(),
    var entityId: UUID = UUID.randomUUID(),
) : Serializable

@Entity
@Table(name = "bom_subgraph_entities")
@IdClass(BoMSubgraphEntityId::class)
class BoMSubgraphEntityRecord(
    @Id
    @Column(name = "subgraph_id", nullable = false)
    var subgraphId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "entity_id", nullable = false)
    var entityId: UUID = UUID.randomUUID(),
)

data class BoMSubgraphEdgeId(
    var subgraphId: UUID = UUID.randomUUID(),
    var edgeId: UUID = UUID.randomUUID(),
) : Serializable

@Entity
@Table(name = "bom_subgraph_edges")
@IdClass(BoMSubgraphEdgeId::class)
class BoMSubgraphEdgeRecord(
    @Id
    @Column(name = "subgraph_id", nullable = false)
    var subgraphId: UUID = UUID.randomUUID(),

    @Id
    @Column(name = "edge_id", nullable = false)
    var edgeId: UUID = UUID.randomUUID(),
)
