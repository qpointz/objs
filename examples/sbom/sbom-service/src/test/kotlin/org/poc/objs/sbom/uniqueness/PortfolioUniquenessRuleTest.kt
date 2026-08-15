package org.poc.objs.sbom.uniqueness

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.sbom.persistence.SbomPortfolioMembershipRecord
import org.poc.objs.sbom.persistence.SbomPortfolioRecord
import java.util.UUID

class PortfolioUniquenessRuleTest {
    private val rules = PortfolioUniquenessRules()
    private val portfolio = SbomPortfolioRecord(name = "P")

    @Test
    fun uniqueAppRejectsSecondPlacement() {
        val app = UUID.randomUUID()
        val existing = listOf(SbomPortfolioMembershipRecord(applicationId = app))
        val msg =
            rules.rule(org.poc.objs.sbom.domain.PortfolioUniqueness.UNIQUE_APP).conflict(
                portfolio,
                existing,
                PlacementCandidate(app, null, null),
            )
        assertThat(msg).isNotNull()
    }

    @Test
    fun notUniqueAllowsSecondCategory() {
        val app = UUID.randomUUID()
        val existing =
            listOf(SbomPortfolioMembershipRecord(applicationId = app, nodeId = UUID.randomUUID()))
        val msg =
            rules.rule(org.poc.objs.sbom.domain.PortfolioUniqueness.NOT_UNIQUE).conflict(
                portfolio,
                existing,
                PlacementCandidate(app, UUID.randomUUID(), null),
            )
        assertThat(msg).isNull()
    }
}
