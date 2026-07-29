package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

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
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.flyway.locations") { "classpath:db/migration" }
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
        val schema = BoMSchema("Person", "1", mapOf("type" to "object", "required" to listOf("name")))
        schemaCatalog.register(schema)

        assertThat(schemaCatalog.get("Person", "1")).isNotNull
        assertThat(schemaCatalog.get("Person", "1")!!.schema["type"]).isEqualTo("object")
        assertThat(schemaCatalog.contains("Person", "1")).isTrue()
        assertThat(schemaCatalog.types()).contains("Person")
        assertThat(schemaRepo.count()).isEqualTo(1)
    }

    @Test
    fun shouldUpsertSchema() {
        schemaCatalog.register(BoMSchema("X", "1", mapOf("type" to "object")))
        schemaCatalog.register(BoMSchema("X", "1", mapOf("type" to "object", "title" to "updated")))

        val schema = schemaCatalog.get("X", "1")!!
        assertThat(schema.schema["title"]).isEqualTo("updated")
        assertThat(schemaCatalog.all()).hasSize(1)
    }

    @Test
    fun shouldRemoveSchema() {
        schemaCatalog.register(BoMSchema("A", "1", mapOf("type" to "object")))
        assertThat(schemaCatalog.remove("A", "1")).isTrue()
        assertThat(schemaCatalog.get("A", "1")).isNull()
        assertThat(schemaCatalog.remove("A", "1")).isFalse()
    }

    @Test
    fun shouldListByType() {
        schemaCatalog.register(BoMSchema("P", "1", mapOf("type" to "object")))
        schemaCatalog.register(BoMSchema("P", "2", mapOf("type" to "object")))
        schemaCatalog.register(BoMSchema("Q", "1", mapOf("type" to "object")))

        assertThat(schemaCatalog.listByType("P")).hasSize(2)
        assertThat(schemaCatalog.types()).containsExactlyInAnyOrder("P", "Q")
    }

    @Test
    fun shouldHydrate() {
        // Insert directly via repo to ensure the row is in the DB
        schemaRepo.save(BoMSchemaCatalogRecord(
            type = "H", version = "1",
            schemaDoc = mutableMapOf("type" to "object"),
        ))
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
            BoMAllowedEdgeRule("Person", "knows", "Person", BoMPropertiesPolicy.SCHEMA, false),
        )
        val rule = edgeCatalog.find("Person", "knows", "Person")
        assertThat(rule).isNotNull
        assertThat(rule!!.propertiesPolicy).isEqualTo(BoMPropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
        assertThat(edgeRuleRepo.count()).isEqualTo(1)
    }

    @Test
    fun shouldUpsertEdgeRule() {
        edgeCatalog.register(BoMAllowedEdgeRule("A", "r", "B", BoMPropertiesPolicy.NONE, true))
        edgeCatalog.register(BoMAllowedEdgeRule("A", "r", "B", BoMPropertiesPolicy.SCHEMA, false))

        val rule = edgeCatalog.find("A", "r", "B")!!
        assertThat(rule.propertiesPolicy).isEqualTo(BoMPropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
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
