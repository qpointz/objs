package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EntityRepository : JpaRepository<EntityRecord, UUID> {
    @org.springframework.data.jpa.repository.Query(
        "select e.type as type, count(e) as cnt from BoMEntityRecord e group by e.type",
    )
    fun countGroupedByType(): List<TypeCount>

    @org.springframework.data.jpa.repository.Query(
        """
        select e.type as type, count(e) as cnt
        from BoMEntityRecord e, BoMGraphMembershipRecord m
        where m.entityId = e.id and m.graphId = :graphId
        group by e.type
        """,
    )
    fun countGroupedByTypeInGraph(graphId: UUID): List<TypeCount>
}

interface TypeCount {
    fun getType(): String
    fun getCnt(): Long
}

interface EdgeRepository : JpaRepository<EdgeRecord, UUID> {
    fun findBySourceIdInAndTargetIdIn(sourceIds: Collection<UUID>, targetIds: Collection<UUID>): List<EdgeRecord>

    fun findBySourceIdInOrTargetIdIn(sourceIds: Collection<UUID>, targetIds: Collection<UUID>): List<EdgeRecord>

    fun findByGraphId(graphId: UUID): List<EdgeRecord>

    fun countByGraphId(graphId: UUID): Long

    @org.springframework.data.jpa.repository.Query(
        "select e from BoMEdgeRecord e where e.sourceId = :entityId or e.targetId = :entityId",
    )
    fun findIncident(entityId: UUID): List<EdgeRecord>

    @org.springframework.data.jpa.repository.Query(
        """
        select e from BoMEdgeRecord e
        where e.graphId = :graphId and (e.sourceId = :entityId or e.targetId = :entityId)
        """,
    )
    fun findIncidentInGraph(entityId: UUID, graphId: UUID): List<EdgeRecord>
}
