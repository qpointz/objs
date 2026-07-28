package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BoEntityRepository : JpaRepository<BoEntityRecord, UUID>

interface BoEdgeRepository : JpaRepository<BoEdgeRecord, UUID> {
    fun findBySourceIdInAndTargetIdIn(sourceIds: Collection<UUID>, targetIds: Collection<UUID>): List<BoEdgeRecord>
}
