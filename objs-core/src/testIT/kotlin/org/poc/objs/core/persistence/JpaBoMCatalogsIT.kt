package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMEdgeCardinality
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.typed.PayloadMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

private fun schema(type: String, version: String, title: String = type) =
    BoMSchema(type, version, BoMSchemaDsl.obj(title, "$title payload"))

/**
 * Integration tests for [JpaBoMSchemaCatalog] and [JpaBoMAllowedEdgeCatalog] against real
 * PostgreSQL via Testcontainers.
 */
@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Testcontainers
class JpaBoMCatalogsIT {

    companion object {
        @Container
        @JvmStatic
        val pg = PostgreSQLContainer("postgres:17-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun pgProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { pg.jdbcUrl }
            registry.add("spring.datasource.username") { pg.username }
            registry.add("spring.datasource.password") { pg.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.flyway.enabled") { "false" }
        }
    }

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var schemaCatalog: BoMSchemaCatalog

    @Autowired
    lateinit var edgeCatalog: BoMAllowedEdgeCatalog

    @Autowired
    lateinit var schemaRepo: BoMSchemaCatalogRepository

    @Autowired
    lateinit var edgeRuleRepo: BoMAllowedEdgeRuleRepository


    @BeforeEach
    fun reset() {
        schemaCatalog.clear()
        edgeCatalog.clear()
    }

    @Test
    fun shouldWireJpaCatalogsAsAuthoritativeImplementations() {
        assertThat(schemaCatalog).isInstanceOf(JpaBoMSchemaCatalog::class.java)
        assertThat(edgeCatalog).isInstanceOf(JpaBoMAllowedEdgeCatalog::class.java)
    }

    // ── Schema catalog ──

    @Test
    fun shouldRegisterAndRetrieveSchema() {
        schemaCatalog.register(
            BoMSchema(
                "Person",
                "1",
                BoMSchemaDsl.obj(
                    "Person",
                    "Person payload",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Person name"))),
                ),
            ),
        )

        assertThat(schemaCatalog.get("Person", "1")).isNotNull
        assertThat(schemaCatalog.get("Person", "1")!!.contentSchema.type.name).isEqualTo("OBJECT")
        assertThat(schemaCatalog.get("Person", "1")!!.usage).isEqualTo(BoMSchemaUsage.ENTITY)
        assertThat(schemaCatalog.contains("Person", "1")).isTrue()
        assertThat(schemaCatalog.types()).contains("Person")
        assertThat(schemaRepo.count()).isEqualTo(1)
    }

    @Test
    fun shouldUpsertSchema() {
        schemaCatalog.register(schema("X", "1"))
        schemaCatalog.register(schema("X", "1", "Updated"))

        val schema = schemaCatalog.get("X", "1")!!
        assertThat(schema.contentSchema.title).isEqualTo("Updated")
        assertThat(schemaCatalog.all()).hasSize(1)
    }

    @Test
    fun shouldRemoveSchema() {
        schemaCatalog.register(schema("A", "1"))
        assertThat(schemaCatalog.remove("A", "1")).isTrue()
        assertThat(schemaCatalog.get("A", "1")).isNull()
        assertThat(schemaCatalog.remove("A", "1")).isFalse()
    }

    @Test
    fun shouldListByType() {
        schemaCatalog.register(schema("P", "1"))
        schemaCatalog.register(schema("P", "2"))
        schemaCatalog.register(schema("Q", "1"))

        assertThat(schemaCatalog.listByType("P")).hasSize(2)
        assertThat(schemaCatalog.types()).containsExactlyInAnyOrder("P", "Q")
    }

    @Test
    fun shouldHydrate() {
        // Insert directly via repo to ensure the row is in the DB
        schemaRepo.save(
            BoMSchemaCatalogRecord(
                type = "H",
                version = "1",
                definitionDoc = PayloadMapper.toMap(schema("H", "1").contentSchema),
                usage = "ENTITY",
            ),
        )
        schemaRepo.flush()
        // Simulate restart: create a fresh catalog and hydrate from the same repo
        val fresh = JpaBoMSchemaCatalog(schemaRepo)
        assertThat(fresh.get("H", "1")).isNull() // cache empty before hydrate
        fresh.hydrate()
        assertThat(fresh.get("H", "1")).isNotNull
    }

    // ── Allowed-edge catalog ──

    @Test
    fun shouldRegisterAndFindEdgeRule() {
        edgeCatalog.register(
            BoMAllowedEdgeRule(
                "Person",
                "knows",
                "Person",
                BoMPropertiesPolicy.SCHEMA,
                false,
                propertiesSchemaType = "CanonicalEdge",
                propertiesSchemaVersion = "1.0.0",
            ),
        )
        val rule = edgeCatalog.find("Person", "knows", "Person")
        assertThat(rule).isNotNull
        assertThat(rule!!.propertiesPolicy).isEqualTo(BoMPropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
        assertThat(rule.propertiesSchemaType).isEqualTo("CanonicalEdge")
        assertThat(rule.propertiesSchemaVersion).isEqualTo("1.0.0")
        assertThat(rule.cardinality).isEqualTo(BoMEdgeCardinality.UNSPECIFIED)
        assertThat(edgeRuleRepo.count()).isEqualTo(1)
    }

    @Test
    fun shouldPersistCardinalityWireValues() {
        edgeCatalog.register(
            BoMAllowedEdgeRule(
                "Product",
                "CONTAINS",
                "Component",
                cardinality = BoMEdgeCardinality.ONE_TO_ONE,
            ),
        )
        edgeCatalog.register(
            BoMAllowedEdgeRule(
                "Component",
                "DEPENDS_ON",
                "Component",
                cardinality = BoMEdgeCardinality.ONE_TO_MANY,
            ),
        )
        assertThat(edgeCatalog.find("Product", "CONTAINS", "Component")!!.cardinality)
            .isEqualTo(BoMEdgeCardinality.ONE_TO_ONE)
        assertThat(edgeCatalog.find("Component", "DEPENDS_ON", "Component")!!.cardinality)
            .isEqualTo(BoMEdgeCardinality.ONE_TO_MANY)
        assertThat(edgeRuleRepo.findById(
            org.poc.objs.core.persistence.BoMAllowedEdgeRuleId("Product", "CONTAINS", "Component"),
        ).get().cardinality).isEqualTo(BoMEdgeCardinality.ONE_TO_ONE)
    }

    @Test
    fun shouldUpsertEdgeRule() {
        edgeCatalog.register(BoMAllowedEdgeRule("A", "r", "B", BoMPropertiesPolicy.NONE, true))
        edgeCatalog.register(
            BoMAllowedEdgeRule(
                "A",
                "r",
                "B",
                BoMPropertiesPolicy.SCHEMA,
                false,
                cardinality = BoMEdgeCardinality.ONE_TO_MANY,
            ),
        )

        val rule = edgeCatalog.find("A", "r", "B")!!
        assertThat(rule.propertiesPolicy).isEqualTo(BoMPropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
        assertThat(rule.cardinality).isEqualTo(BoMEdgeCardinality.ONE_TO_MANY)
        assertThat(edgeCatalog.all()).hasSize(1)
    }

    @Test
    fun shouldRemoveEdgeRule() {
        edgeCatalog.register(BoMAllowedEdgeRule("A", "r", "B"))
        assertThat(edgeCatalog.remove("A", "r", "B")).isTrue()
        assertThat(edgeCatalog.find("A", "r", "B")).isNull()
        assertThat(edgeCatalog.remove("A", "r", "B")).isFalse()
    }

    @Test
    fun shouldPreferMostSpecificWildcard() {
        edgeCatalog.register(BoMAllowedEdgeRule("*", "x", "*", BoMPropertiesPolicy.NONE))
        edgeCatalog.register(BoMAllowedEdgeRule("A", "x", "B", BoMPropertiesPolicy.SCHEMA))
        assertThat(edgeCatalog.find("A", "x", "B")!!.propertiesPolicy).isEqualTo(BoMPropertiesPolicy.SCHEMA)
        assertThat(edgeCatalog.find("C", "x", "D")!!.propertiesPolicy).isEqualTo(BoMPropertiesPolicy.NONE)
    }

    @Test
    fun shouldHydrateEdges() {
        edgeRuleRepo.save(BoMAllowedEdgeRuleRecord(
            sourceType = "X", role = "y", targetType = "Z",
        ))
        edgeRuleRepo.flush()
        val fresh = JpaBoMAllowedEdgeCatalog(edgeRuleRepo)
        assertThat(fresh.find("X", "y", "Z")).isNull()
        fresh.hydrate()
        assertThat(fresh.find("X", "y", "Z")).isNotNull
    }
}
