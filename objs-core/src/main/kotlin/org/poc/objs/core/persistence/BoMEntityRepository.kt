package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BoMEntityRepository : JpaRepository<BoMEntityRecord, UUID>

interface BoMEdgeRepository : JpaRepository<BoMEdgeRecord, UUID> {
    fun findBySourceIdInAndTargetIdIn(sourceIds: Collection<UUID>, targetIds: Collection<UUID>): List<BoMEdgeRecord>

    fun findBySourceIdInOrTargetIdIn(sourceIds: Collection<UUID>, targetIds: Collection<UUID>): List<BoMEdgeRecord>
}
