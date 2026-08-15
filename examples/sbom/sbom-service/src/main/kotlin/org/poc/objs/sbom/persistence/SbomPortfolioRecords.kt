package org.poc.objs.sbom.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sbom_portfolio")
class SbomPortfolioRecord(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 255)
    var name: String = "",
    @Column(length = 2048)
    var description: String? = null,
    @Column(nullable = false, length = 32)
    var uniqueness: String = "UNIQUE_APP",
    @Column(nullable = false, length = 32)
    var origin: String = "MANUAL",
    @Column(length = 255)
    var source: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "sbom_portfolio_node")
class SbomPortfolioNodeRecord(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "portfolio_id", nullable = false)
    var portfolioId: UUID = UUID.randomUUID(),
    @Column(name = "parent_id")
    var parentId: UUID? = null,
    @Column(nullable = false, length = 255)
    var name: String = "",
    @Column(length = 2048)
    var description: String? = null,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
)

@Entity
@Table(name = "sbom_portfolio_membership")
class SbomPortfolioMembershipRecord(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "portfolio_id", nullable = false)
    var portfolioId: UUID = UUID.randomUUID(),
    @Column(name = "node_id")
    var nodeId: UUID? = null,
    @Column(name = "application_id", nullable = false)
    var applicationId: UUID = UUID.randomUUID(),
    @Column(name = "version_id")
    var versionId: UUID? = null,
)
