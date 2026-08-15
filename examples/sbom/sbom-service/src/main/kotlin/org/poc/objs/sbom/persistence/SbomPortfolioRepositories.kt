package org.poc.objs.sbom.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SbomPortfolioRepository : JpaRepository<SbomPortfolioRecord, UUID> {
    fun findByNameIgnoreCase(name: String): SbomPortfolioRecord?
}

interface SbomPortfolioNodeRepository : JpaRepository<SbomPortfolioNodeRecord, UUID> {
    fun findByPortfolioIdOrderBySortOrderAscNameAsc(portfolioId: UUID): List<SbomPortfolioNodeRecord>

    fun findByIdAndPortfolioId(id: UUID, portfolioId: UUID): SbomPortfolioNodeRecord?
}

interface SbomPortfolioMembershipRepository : JpaRepository<SbomPortfolioMembershipRecord, UUID> {
    fun findByPortfolioId(portfolioId: UUID): List<SbomPortfolioMembershipRecord>

    fun findByPortfolioIdAndApplicationId(
        portfolioId: UUID,
        applicationId: UUID,
    ): SbomPortfolioMembershipRecord?

    fun findByIdAndPortfolioId(id: UUID, portfolioId: UUID): SbomPortfolioMembershipRecord?

    fun findByPortfolioIdAndNodeId(portfolioId: UUID, nodeId: UUID?): List<SbomPortfolioMembershipRecord>
}
