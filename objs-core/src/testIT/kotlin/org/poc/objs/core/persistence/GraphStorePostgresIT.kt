package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.core.domain.GraphSpec
import org.poc.objs.api.domain.PropertiesPolicy
import org.poc.objs.core.domain.Schema
import org.poc.objs.core.domain.SchemaCatalog
import org.poc.objs.core.domain.SchemaDsl
import org.poc.objs.core.match.GraphExprMatcher
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
@Import(GraphStore::class, NamedGraphStore::class, PoolEntityReader::class)
@Testcontainers
class GraphStorePostgresIT {

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
    lateinit var store: GraphStore

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var schemas: SchemaCatalog

    @Autowired
    lateinit var allowed: AllowedEdgeCatalog

    @Autowired
    lateinit var graphRepository: GraphRepository

    @Autowired
    lateinit var namedGraphs: NamedGraphStore

    /** Edges require an owning graph (`graph_id` NOT NULL); every edge in this file shares [graphId]. */
    private lateinit var graphId: UUID

    @BeforeEach
    fun catalogs() {
        schemas.clear()
        allowed.clear()
        schemas.register(
            Schema(
                "Person",
                "1",
                SchemaDsl.obj(
                    "Person",
                    "Person payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Person name"))),
                ),
            ),
        )
        allowed.register(AllowedEdgeRule("Person", "knows", "Person", PropertiesPolicy.NONE))
        graphId = UUID.randomUUID()
        graphRepository.save(GraphRecord(id = graphId))
    }

    @Test
    fun shouldRoundTripEntitiesAndEdgesOnPostgres() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()
        val graph = Graph(
            entities = mutableListOf(
                Entity(id = a, type = "Person", schemaVersion = "1",
                    payload = mutableMapOf("name" to "Alice"),
                    annotations = mutableMapOf("env" to "test", "team" to "core")),
                Entity(id = b, type = "Person", schemaVersion = "1",
                    payload = mutableMapOf("name" to "Bob"),
                    annotations = mutableMapOf("env" to "test")),
                Entity(id = c, type = "Person", schemaVersion = "1",
                    payload = mutableMapOf("name" to "Carol"),
                    annotations = mutableMapOf("env" to "prod", "team" to "core")),
            ),
            edges = mutableListOf(
                Edge(graphId = graphId, source = a, target = b, role = "knows"),
                Edge(graphId = graphId, source = a, target = c, role = "knows"),
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
        assertThat(alice.createdAt).isNotNull
        assertThat(alice.updatedAt).isNotNull
        assertThat(alice.updatedAt).isAfterOrEqualTo(alice.createdAt)
    }

    @Test
    fun shouldHaveAnnotationsGinIndex_afterJsonbMigration() {
        val count = jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM pg_indexes
            WHERE tablename = 'objs_entity'
              AND indexname = 'idx_objs_entity_annotations_gin'
            """.trimIndent(),
            Int::class.java,
        )
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun shouldHaveGraphAnnotationsGinIndex_afterV2() {
        val count = jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM pg_indexes
            WHERE tablename = 'objs_graph'
              AND indexname = 'idx_objs_graph_annotations_gin'
            """.trimIndent(),
            Int::class.java,
        )
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun shouldHaveGraphScopedEdgeCompositeIndexes_afterV2() {
        val names = jdbc.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE tablename = 'objs_graph_edge'
              AND indexname IN (
                'idx_objs_graph_edge_graph_source',
                'idx_objs_graph_edge_graph_target'
              )
            ORDER BY indexname
            """.trimIndent(),
            String::class.java,
        )
        assertThat(names).containsExactly(
            "idx_objs_graph_edge_graph_source",
            "idx_objs_graph_edge_graph_target",
        )
    }

    @Test
    fun shouldPushdownGraphExpr_byAnnotationContainment() {
        val entityId = UUID.randomUUID()
        assertThat(
            store.write(
                Graph(
                    entities = mutableListOf(
                        Entity(
                            id = entityId,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "X"),
                        ),
                    ),
                ),
            ).isValid,
        ).isTrue()
        val hit = namedGraphs.create(
            GraphSpec(
                annotations = mapOf("app" to "payments-api", "appVersion" to "2.3.1"),
                entityIds = setOf(entityId),
            ),
        )
        namedGraphs.create(
            GraphSpec(
                annotations = mapOf("app" to "payments-api", "appVersion" to "2.4.0"),
                entityIds = setOf(entityId),
            ),
        )

        val headers = namedGraphs.matchingHeaders(
            GraphExprMatcher("a.app == 'payments-api' && a.appVersion == '2.3.1'"),
        )
        assertThat(headers).hasSize(1)
        assertThat(headers[0].id).isEqualTo(hit.id)

        val searched = namedGraphs.search(expr = "a.app == 'payments-api' && a.appVersion == '2.3.1'")
        assertThat(searched.map { it.id }).containsExactly(hit.id)
    }

    @Test
    fun shouldAssignIdsAndPersist() {
        val graph = Graph(
            entities = mutableListOf(
                Entity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "X")),
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
        val graph = Graph(
            entities = mutableListOf(
                Entity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                Entity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
            ),
            edges = mutableListOf(Edge(graphId = graphId, source = a, target = b, role = "knows")),
        )
        assertThat(store.write(graph).isValid).isTrue()
        assertThat(store.delete(entityIds = listOf(a)).isValid).isTrue()
        val loaded = store.loadAll()
        assertThat(loaded.entities.map { it.id }).containsExactly(b)
        assertThat(loaded.edges).isEmpty()
    }
}
