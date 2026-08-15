package org.poc.objs.sbom.uniqueness

import org.poc.objs.sbom.domain.PortfolioUniqueness
import org.poc.objs.sbom.persistence.SbomPortfolioMembershipRecord
import org.poc.objs.sbom.persistence.SbomPortfolioRecord
import org.springframework.stereotype.Component
import java.util.UUID

data class PlacementCandidate(
    val applicationId: UUID,
    val nodeId: UUID?,
    val versionId: UUID?,
    val excludePlacementId: UUID? = null,
)

fun interface PortfolioUniquenessRule {
    fun conflict(
        portfolio: SbomPortfolioRecord,
        existing: List<SbomPortfolioMembershipRecord>,
        candidate: PlacementCandidate,
    ): String?
}

class UniqueAppRule : PortfolioUniquenessRule {
    override fun conflict(
        portfolio: SbomPortfolioRecord,
        existing: List<SbomPortfolioMembershipRecord>,
        candidate: PlacementCandidate,
    ): String? {
        if (candidate.versionId != null) {
            return "UNIQUE_APP placements must not pin a version"
        }
        val clash =
            existing.any {
                it.id != candidate.excludePlacementId && it.applicationId == candidate.applicationId
            }
        return if (clash) "Application already placed in this portfolio" else null
    }
}

class UniqueAppVersionRule : PortfolioUniquenessRule {
    override fun conflict(
        portfolio: SbomPortfolioRecord,
        existing: List<SbomPortfolioMembershipRecord>,
        candidate: PlacementCandidate,
    ): String? {
        if (candidate.versionId == null) {
            return "UNIQUE_APP_VERSION placements require a version"
        }
        val clash =
            existing.any {
                it.id != candidate.excludePlacementId &&
                    it.applicationId == candidate.applicationId &&
                    it.versionId == candidate.versionId
            }
        return if (clash) "Application version already placed in this portfolio" else null
    }
}

class NotUniqueRule : PortfolioUniquenessRule {
    override fun conflict(
        portfolio: SbomPortfolioRecord,
        existing: List<SbomPortfolioMembershipRecord>,
        candidate: PlacementCandidate,
    ): String? {
        if (candidate.versionId != null) {
            return "NOT_UNIQUE placements must not pin a version"
        }
        return null
    }
}

@Component
class PortfolioUniquenessRules {
    private val rules: Map<PortfolioUniqueness, PortfolioUniquenessRule> =
        mapOf(
            PortfolioUniqueness.UNIQUE_APP to UniqueAppRule(),
            PortfolioUniqueness.UNIQUE_APP_VERSION to UniqueAppVersionRule(),
            PortfolioUniqueness.NOT_UNIQUE to NotUniqueRule(),
        )

    fun rule(policy: PortfolioUniqueness): PortfolioUniquenessRule =
        rules[policy] ?: error("No uniqueness rule for $policy")
}
