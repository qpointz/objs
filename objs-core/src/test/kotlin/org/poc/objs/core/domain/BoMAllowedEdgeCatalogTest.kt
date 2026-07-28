package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.validation.BoMEntityTypeLookup
import org.poc.objs.core.validation.BoMValidator
import java.util.UUID

class BoMAllowedEdgeCatalogTest {
    private lateinit var catalog: BoMAllowedEdgeCatalog

    @BeforeEach
    fun setUp() {
        catalog = BoMAllowedEdgeCatalog()
    }

    @Test
    fun shouldMatchWildcardSourceAndTarget_whenRoleMatches() {
        catalog.register(
            BoMAllowedEdgeRule(
                sourceType = BoMAllowedEdgeRule.ANY,
                role = "depends_on",
                targetType = BoMAllowedEdgeRule.ANY,
                propertiesPolicy = BoMPropertiesPolicy.NONE,
            ),
        )
        val rule = catalog.find("Service", "depends_on", "Database")
        assertThat(rule).isNotNull
        assertThat(rule!!.role).isEqualTo("depends_on")
    }

    @Test
    fun shouldPreferExactRuleOverWildcard() {
        catalog.register(
            BoMAllowedEdgeRule(BoMAllowedEdgeRule.ANY, "knows", BoMAllowedEdgeRule.ANY, BoMPropertiesPolicy.NONE),
        )
        catalog.register(
            BoMAllowedEdgeRule(
                "Person",
                "knows",
                "Person",
                BoMPropertiesPolicy.SCHEMA,
                emptyPropertiesAllowed = false,
            ),
        )
        val rule = catalog.find("Person", "knows", "Person")
        assertThat(rule!!.propertiesPolicy).isEqualTo(BoMPropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
    }

    @Test
    fun shouldDenyWhenRoleDoesNotMatchWildcardRule() {
        catalog.register(
            BoMAllowedEdgeRule(BoMAllowedEdgeRule.ANY, "depends_on", BoMAllowedEdgeRule.ANY),
        )
        assertThat(catalog.find("A", "owns", "B")).isNull()
    }

    @Test
    fun shouldMatchOneSidedWildcard() {
        catalog.register(
            BoMAllowedEdgeRule("Person", "reports_to", BoMAllowedEdgeRule.ANY),
        )
        assertThat(catalog.find("Person", "reports_to", "Org")).isNotNull
        assertThat(catalog.find("Org", "reports_to", "Person")).isNull()
    }

    @Test
    fun shouldRemoveExactTriple() {
        catalog.register(BoMAllowedEdgeRule("A", "r", "B"))
        assertThat(catalog.remove("A", "r", "B")).isTrue()
        assertThat(catalog.find("A", "r", "B")).isNull()
        assertThat(catalog.remove("A", "r", "B")).isFalse()
    }
}

class BoMSchemaCatalogTest {
    @Test
    fun shouldListByTypeAndRemove() {
        val catalog = BoMSchemaCatalog()
        catalog.register(BoMSchema("Person", "1", mapOf("type" to "object")))
        catalog.register(BoMSchema("Person", "2", mapOf("type" to "object")))
        catalog.register(BoMSchema("Org", "1", mapOf("type" to "object")))

        assertThat(catalog.types()).containsExactlyInAnyOrder("Person", "Org")
        assertThat(catalog.listByType("Person")).hasSize(2)
        assertThat(catalog.remove("Person", "1")).isTrue()
        assertThat(catalog.listByType("Person")).hasSize(1)
        assertThat(catalog.get("Person", "1")).isNull()
        assertThat(catalog.remove("Person", "1")).isFalse()
    }
}

class BoMValidatorWildcardEdgeTest {
    @Test
    fun shouldAllowAnyTypes_whenWildcardRoleRuleRegistered() {
        val schemas = BoMSchemaCatalog()
        val allowed = BoMAllowedEdgeCatalog()
        allowed.register(
            BoMAllowedEdgeRule(
                sourceType = BoMAllowedEdgeRule.ANY,
                role = "depends_on",
                targetType = BoMAllowedEdgeRule.ANY,
            ),
        )
        val validator = BoMValidator(schemas, allowed)
        val edge = BoMEdge(
            source = UUID.randomUUID(),
            target = UUID.randomUUID(),
            role = "depends_on",
        )
        val types = mapOf(edge.source to "Service", edge.target to "Database")
        val result = validator.validateEdges(listOf(edge), BoMEntityTypeLookup { types[it] })
        assertThat(result.isValid).isTrue()
    }
}
