package org.poc.objs.core.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BoMEntityVersionRepository : JpaRepository<BoMEntityVersionRecord, BoMEntityVersionId> {
    fun findByEntityIdAndVersion(entityId: UUID, version: Long): BoMEntityVersionRecord?

    fun findByEntityIdOrderByVersionDesc(entityId: UUID): List<BoMEntityVersionRecord>

    fun findByEntityIdOrderByVersionDesc(entityId: UUID, pageable: Pageable): List<BoMEntityVersionRecord>

    fun countByEntityId(entityId: UUID): Long
}

interface BoMGraphVersionRepository : JpaRepository<BoMGraphVersionRecord, BoMGraphVersionId> {
    fun findByGraphIdAndVersion(graphId: UUID, version: Long): BoMGraphVersionRecord?

    fun findByGraphIdOrderByVersionDesc(graphId: UUID): List<BoMGraphVersionRecord>
}

interface BoMEdgeVersionRepository : JpaRepository<BoMEdgeVersionRecord, BoMEdgeVersionId> {
    fun findByEdgeIdAndVersion(edgeId: UUID, version: Long): BoMEdgeVersionRecord?

    fun findByEdgeIdOrderByVersionDesc(edgeId: UUID): List<BoMEdgeVersionRecord>

    fun findByEdgeIdOrderByVersionDesc(edgeId: UUID, pageable: Pageable): List<BoMEdgeVersionRecord>

    fun countByEdgeId(edgeId: UUID): Long
}

interface BoMGraphVersionMemberRepository : JpaRepository<BoMGraphVersionMemberRecord, BoMGraphVersionMemberId> {
    fun findByGraphIdAndGraphVersion(graphId: UUID, graphVersion: Long): List<BoMGraphVersionMemberRecord>

    @Query(
        """
        SELECT DISTINCT m.graphId FROM BoMGraphVersionMemberRecord m
        WHERE m.entityId = :entityId
        """,
    )
    fun findDistinctGraphIdsByEntityId(entityId: UUID): List<UUID>
}

interface BoMGraphVersionEdgeRepository : JpaRepository<BoMGraphVersionEdgeRecord, BoMGraphVersionEdgeId> {
    fun findByGraphIdAndGraphVersion(graphId: UUID, graphVersion: Long): List<BoMGraphVersionEdgeRecord>
}
