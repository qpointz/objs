package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GraphRepository : JpaRepository<GraphRecord, UUID>

interface GraphMembershipRepository : JpaRepository<GraphMembershipRecord, GraphMembershipId> {
    fun findByGraphId(graphId: UUID): List<GraphMembershipRecord>

    fun findByEntityId(entityId: UUID): List<GraphMembershipRecord>

    fun deleteByGraphId(graphId: UUID)

    fun deleteByGraphIdAndEntityId(graphId: UUID, entityId: UUID)

    fun countByGraphId(graphId: UUID): Long
}
