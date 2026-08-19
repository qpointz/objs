package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BoMEntityVersionRepository : JpaRepository<BoMEntityVersionRecord, BoMEntityVersionId> {
    fun findByEntityIdAndVersion(entityId: UUID, version: Long): BoMEntityVersionRecord?
}

interface BoMGraphVersionRepository : JpaRepository<BoMGraphVersionRecord, BoMGraphVersionId> {
    fun findByGraphIdAndVersion(graphId: UUID, version: Long): BoMGraphVersionRecord?

    fun findByGraphIdOrderByVersionDesc(graphId: UUID): List<BoMGraphVersionRecord>
}

interface BoMEdgeVersionRepository : JpaRepository<BoMEdgeVersionRecord, BoMEdgeVersionId> {
    fun findByEdgeIdAndVersion(edgeId: UUID, version: Long): BoMEdgeVersionRecord?
}

interface BoMGraphVersionMemberRepository : JpaRepository<BoMGraphVersionMemberRecord, BoMGraphVersionMemberId> {
    fun findByGraphIdAndGraphVersion(graphId: UUID, graphVersion: Long): List<BoMGraphVersionMemberRecord>
}

interface BoMGraphVersionEdgeRepository : JpaRepository<BoMGraphVersionEdgeRecord, BoMGraphVersionEdgeId> {
    fun findByGraphIdAndGraphVersion(graphId: UUID, graphVersion: Long): List<BoMGraphVersionEdgeRecord>
}
