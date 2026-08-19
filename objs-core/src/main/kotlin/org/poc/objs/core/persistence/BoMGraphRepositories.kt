package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BoMGraphRepository : JpaRepository<BoMGraphRecord, UUID>

interface BoMGraphMembershipRepository : JpaRepository<BoMGraphMembershipRecord, BoMGraphMembershipId> {
    fun findByGraphId(graphId: UUID): List<BoMGraphMembershipRecord>

    fun findByEntityId(entityId: UUID): List<BoMGraphMembershipRecord>

    fun deleteByGraphId(graphId: UUID)

    fun deleteByGraphIdAndEntityId(graphId: UUID, entityId: UUID)

    fun countByGraphId(graphId: UUID): Long
}
