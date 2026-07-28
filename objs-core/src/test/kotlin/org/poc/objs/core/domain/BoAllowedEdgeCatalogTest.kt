package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.validation.BoEntityTypeLookup
import org.poc.objs.core.validation.BoValidator
import java.util.UUID

class BoAllowedEdgeCatalogTest {
    private lateinit var catalog: BoAllowedEdgeCatalog

    @BeforeEach
    fun setUp() {
        catalog = BoAllowedEdgeCatalog()
    }

    @Test
    fun shouldMatchWildcardSourceAndTarget_whenRoleMatches() {
        catalog.register(
            BoAllowedEdgeRule(
                sourceType = BoAllowedEdgeRule.ANY,
                role = "depends_on",
                targetType = BoAllowedEdgeRule.ANY,
                propertiesPolicy = BoPropertiesPolicy.NONE,
            ),
        )
        val rule = catalog.find("Service", "depends_on", "Database")
        assertThat(rule).isNotNull
        assertThat(rule!!.role).isEqualTo("depends_on")
    }

    @Test
    fun shouldPreferExactRuleOverWildcard() {
        catalog.register(
            BoAllowedEdgeRule(BoAllowedEdgeRule.ANY, "knows", BoAllowedEdgeRule.ANY, BoPropertiesPolicy.NONE),
        )
        catalog.register(
            BoAllowedEdgeRule(
                "Person",
                "knows",
                "Person",
                BoPropertiesPolicy.SCHEMA,
                emptyPropertiesAllowed = false,
            ),
        )
        val rule = catalog.find("Person", "knows", "Person")
        assertThat(rule!!.propertiesPolicy).isEqualTo(BoPropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
    }

    @Test
    fun shouldDenyWhenRoleDoesNotMatchWildcardRule() {
        catalog.register(
            BoAllowedEdgeRule(BoAllowedEdgeRule.ANY, "depends_on", BoAllowedEdgeRule.ANY),
        )
        assertThat(catalog.find("A", "owns", "B")).isNull()
    }

    @Test
    fun shouldMatchOneSidedWildcard() {
        catalog.register(
            BoAllowedEdgeRule("Person", "reports_to", BoAllowedEdgeRule.ANY),
        )
        assertThat(catalog.find("Person", "reports_to", "Org")).isNotNull
        assertThat(catalog.find("Org", "reports_to", "Person")).isNull()
    }
}

class BoValidatorWildcardEdgeTest {
    @Test
    fun shouldAllowAnyTypes_whenWildcardRoleRuleRegistered() {
        val schemas = BoSchemaCatalog()
        val allowed = BoAllowedEdgeCatalog()
        allowed.register(
            BoAllowedEdgeRule(
                sourceType = BoAllowedEdgeRule.ANY,
                role = "depends_on",
                targetType = BoAllowedEdgeRule.ANY,
            ),
        )
        val validator = BoValidator(schemas, allowed)
        val edge = BoEdge(
            source = UUID.randomUUID(),
            target = UUID.randomUUID(),
            role = "depends_on",
        )
        val types = mapOf(edge.source to "Service", edge.target to "Database")
        val result = validator.validateEdges(listOf(edge), BoEntityTypeLookup { types[it] })
        assertThat(result.isValid).isTrue()
    }
}
