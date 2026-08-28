package org.poc.objs.core.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface EntityVersionRepository : JpaRepository<EntityVersionRecord, EntityVersionId> {
    fun findByEntityIdAndVersion(entityId: UUID, version: Long): EntityVersionRecord?

    fun findByEntityIdOrderByVersionDesc(entityId: UUID): List<EntityVersionRecord>

    fun findByEntityIdOrderByVersionDesc(entityId: UUID, pageable: Pageable): List<EntityVersionRecord>

    fun countByEntityId(entityId: UUID): Long
}

interface GraphVersionRepository : JpaRepository<GraphVersionRecord, GraphVersionId> {
    fun findByGraphIdAndVersion(graphId: UUID, version: Long): GraphVersionRecord?

    fun findByGraphIdOrderByVersionDesc(graphId: UUID): List<GraphVersionRecord>
}

interface EdgeVersionRepository : JpaRepository<EdgeVersionRecord, EdgeVersionId> {
    fun findByEdgeIdAndVersion(edgeId: UUID, version: Long): EdgeVersionRecord?

    fun findByEdgeIdOrderByVersionDesc(edgeId: UUID): List<EdgeVersionRecord>

    fun findByEdgeIdOrderByVersionDesc(edgeId: UUID, pageable: Pageable): List<EdgeVersionRecord>

    fun countByEdgeId(edgeId: UUID): Long
}

interface GraphVersionMemberRepository : JpaRepository<GraphVersionMemberRecord, GraphVersionMemberId> {
    fun findByGraphIdAndGraphVersion(graphId: UUID, graphVersion: Long): List<GraphVersionMemberRecord>

    @Query(
        """
        SELECT DISTINCT m.graphId FROM BoMGraphVersionMemberRecord m
        WHERE m.entityId = :entityId
        """,
    )
    fun findDistinctGraphIdsByEntityId(entityId: UUID): List<UUID>
}

interface GraphVersionEdgeRepository : JpaRepository<GraphVersionEdgeRecord, GraphVersionEdgeId> {
    fun findByGraphIdAndGraphVersion(graphId: UUID, graphVersion: Long): List<GraphVersionEdgeRecord>
}
