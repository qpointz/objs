package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BoMSubgraphRepository : JpaRepository<BoMSubgraphRecord, UUID>

interface BoMSubgraphEntityRepository : JpaRepository<BoMSubgraphEntityRecord, BoMSubgraphEntityId> {
    fun findBySubgraphId(subgraphId: UUID): List<BoMSubgraphEntityRecord>

    fun deleteBySubgraphId(subgraphId: UUID)

    fun countBySubgraphId(subgraphId: UUID): Long
}

interface BoMSubgraphEdgeRepository : JpaRepository<BoMSubgraphEdgeRecord, BoMSubgraphEdgeId> {
    fun findBySubgraphId(subgraphId: UUID): List<BoMSubgraphEdgeRecord>

    fun deleteBySubgraphId(subgraphId: UUID)

    fun countBySubgraphId(subgraphId: UUID): Long
}
