package org.poc.objs.core.domain

import org.poc.objs.api.domain.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.validation.EntityTypeLookup
import org.poc.objs.core.validation.Validator
import java.util.UUID

class AllowedEdgeCatalogTest {
    private lateinit var catalog: InMemoryAllowedEdgeCatalog

    @BeforeEach
    fun setUp() {
        catalog = InMemoryAllowedEdgeCatalog()
    }

    @Test
    fun shouldMatchWildcardSourceAndTarget_whenRoleMatches() {
        catalog.register(
            AllowedEdgeRule(
                sourceType = AllowedEdgeRule.ANY,
                role = "depends_on",
                targetType = AllowedEdgeRule.ANY,
                propertiesPolicy = PropertiesPolicy.NONE,
            ),
        )
        val rule = catalog.find("Service", "depends_on", "Database")
        assertThat(rule).isNotNull
        assertThat(rule!!.role).isEqualTo("depends_on")
    }

    @Test
    fun shouldPreferExactRuleOverWildcard() {
        catalog.register(
            AllowedEdgeRule(AllowedEdgeRule.ANY, "knows", AllowedEdgeRule.ANY, PropertiesPolicy.NONE),
        )
        catalog.register(
            AllowedEdgeRule(
                "Person",
                "knows",
                "Person",
                PropertiesPolicy.SCHEMA,
                emptyPropertiesAllowed = false,
            ),
        )
        val rule = catalog.find("Person", "knows", "Person")
        assertThat(rule!!.propertiesPolicy).isEqualTo(PropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
        assertThat(rule.cardinality).isEqualTo(EdgeCardinality.UNSPECIFIED)
    }

    @Test
    fun shouldRetainCardinalityOnRegister() {
        catalog.register(
            AllowedEdgeRule(
                "Product",
                "CONTAINS",
                "Component",
                cardinality = EdgeCardinality.ONE_TO_MANY,
            ),
        )
        assertThat(catalog.find("Product", "CONTAINS", "Component")!!.cardinality)
            .isEqualTo(EdgeCardinality.ONE_TO_MANY)
    }

    @Test
    fun shouldDenyWhenRoleDoesNotMatchWildcardRule() {
        catalog.register(
            AllowedEdgeRule(AllowedEdgeRule.ANY, "depends_on", AllowedEdgeRule.ANY),
        )
        assertThat(catalog.find("A", "owns", "B")).isNull()
    }

    @Test
    fun shouldMatchOneSidedWildcard() {
        catalog.register(
            AllowedEdgeRule("Person", "reports_to", AllowedEdgeRule.ANY),
        )
        assertThat(catalog.find("Person", "reports_to", "Org")).isNotNull
        assertThat(catalog.find("Org", "reports_to", "Person")).isNull()
    }

    @Test
    fun shouldRemoveExactTriple() {
        catalog.register(AllowedEdgeRule("A", "r", "B"))
        assertThat(catalog.remove("A", "r", "B")).isTrue()
        assertThat(catalog.find("A", "r", "B")).isNull()
        assertThat(catalog.remove("A", "r", "B")).isFalse()
    }
}

class SchemaCatalogTest {
    @Test
    fun shouldListByTypeAndRemove() {
        val catalog = InMemorySchemaCatalog()
        fun schema(type: String, version: String) = Schema(
            type,
            version,
            SchemaDsl.obj(type, "$type payload"),
        )
        catalog.register(schema("Person", "1"))
        catalog.register(schema("Person", "2"))
        catalog.register(schema("Org", "1"))

        assertThat(catalog.types()).containsExactlyInAnyOrder("Person", "Org")
        assertThat(catalog.listByType("Person")).hasSize(2)
        assertThat(catalog.remove("Person", "1")).isTrue()
        assertThat(catalog.listByType("Person")).hasSize(1)
        assertThat(catalog.get("Person", "1")).isNull()
        assertThat(catalog.remove("Person", "1")).isFalse()
    }
}

class ValidatorWildcardEdgeTest {
    @Test
    fun shouldAllowAnyTypes_whenWildcardRoleRuleRegistered() {
        val schemas = InMemorySchemaCatalog()
        val allowed = InMemoryAllowedEdgeCatalog()
        allowed.register(
            AllowedEdgeRule(
                sourceType = AllowedEdgeRule.ANY,
                role = "depends_on",
                targetType = AllowedEdgeRule.ANY,
            ),
        )
        val validator = Validator(schemas, allowed)
        val edge = Edge(
            source = UUID.randomUUID(),
            target = UUID.randomUUID(),
            role = "depends_on",
        )
        val types = mapOf(edge.source to "Service", edge.target to "Database")
        val result = validator.validateEdges(listOf(edge), EntityTypeLookup { types[it] })
        assertThat(result.isValid).isTrue()
    }
}
