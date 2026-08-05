package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDsl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/**
 * Verify existing entity/edge persistence against real PostgreSQL, including JSONB round-trips.
 */
@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class)
@Testcontainers
class BoMGraphStorePostgresIT {

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
    lateinit var store: BoMGraphStore

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @Autowired
    lateinit var allowed: BoMAllowedEdgeCatalog

    @BeforeEach
    fun catalogs() {
        schemas.clear()
        allowed.clear()
        schemas.register(
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
        allowed.register(BoMAllowedEdgeRule("Person", "knows", "Person", BoMPropertiesPolicy.NONE))
    }

    @Test
    fun shouldRoundTripEntitiesAndEdgesOnPostgres() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(id = a, type = "Person", schemaVersion = "1",
                    payload = mutableMapOf("name" to "Alice"),
                    annotations = mutableMapOf("env" to "test", "team" to "core")),
                BoMEntity(id = b, type = "Person", schemaVersion = "1",
                    payload = mutableMapOf("name" to "Bob"),
                    annotations = mutableMapOf("env" to "test")),
                BoMEntity(id = c, type = "Person", schemaVersion = "1",
                    payload = mutableMapOf("name" to "Carol"),
                    annotations = mutableMapOf("env" to "prod", "team" to "core")),
            ),
            edges = mutableListOf(
                BoMEdge(source = a, target = b, role = "knows"),
                BoMEdge(source = a, target = c, role = "knows"),
            ),
        )
        assertThat(store.write(graph).isValid).isTrue()

        val loaded = store.loadAll()
        assertThat(loaded.entities).hasSize(3)
        assertThat(loaded.edges).hasSize(2)

        val alice = loaded.entities.find { it.id == a }!!
        assertThat(alice.payload["name"]).isEqualTo("Alice")
        assertThat(alice.annotations["env"]).isEqualTo("test")
        assertThat(alice.annotations["team"]).isEqualTo("core")

        val sub = store.selectSubgraphMatchAll(mapOf("env" to "test", "team" to "core"))
        assertThat(sub.entities.map { it.id }).containsExactly(a)
        assertThat(sub.edges).isEmpty()

        val testSubgraph = store.selectSubgraphMatchAll(mapOf("env" to "test"))
        assertThat(testSubgraph.entities.map { it.id }).containsExactlyInAnyOrder(a, b)
        assertThat(testSubgraph.edges).hasSize(1)

        val filterOnly = store.selectSubgraph(
            org.poc.objs.core.match.BoMAnnotationMatcher { entity ->
                entity.annotations["team"] == "core"
            },
        )
        assertThat(filterOnly.entities.map { it.id }).containsExactlyInAnyOrder(a, c)
        assertThat(filterOnly.edges).hasSize(1)
        assertThat(filterOnly.edges.single().source).isEqualTo(a)
        assertThat(filterOnly.edges.single().target).isEqualTo(c)
    }

    @Test
    fun shouldHaveAnnotationsGinIndex_afterJsonbMigration() {
        val count = jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM pg_indexes
            WHERE tablename = 'bom_graph_entity'
              AND indexname = 'idx_bom_graph_entity_annotations_gin'
            """.trimIndent(),
            Int::class.java,
        )
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun shouldAssignIdsAndPersist() {
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "X")),
            ),
        )
        assertThat(store.write(graph).isValid).isTrue()
        assertThat(graph.entities[0].id).isNotNull()
        assertThat(store.loadAll().entities).hasSize(1)
    }

    @Test
    fun shouldDeleteEntityAndIncidentEdges() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                BoMEntity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
            ),
            edges = mutableListOf(BoMEdge(source = a, target = b, role = "knows")),
        )
        assertThat(store.write(graph).isValid).isTrue()
        assertThat(store.delete(entityIds = listOf(a)).isValid).isTrue()
        val loaded = store.loadAll()
        assertThat(loaded.entities.map { it.id }).containsExactly(b)
        assertThat(loaded.edges).isEmpty()
    }
}
