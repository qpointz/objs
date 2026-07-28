package org.poc.objs.core.persistence

import java.util.UUID

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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class BoMGraphStoreTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var store: BoMGraphStore

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
                type = "Person",
                version = "1",
                schema = mapOf(
                    "type" to "object",
                    "required" to listOf("name"),
                    "properties" to mapOf("name" to mapOf("type" to "string")),
                ),
            ),
        )
        allowed.register(BoMAllowedEdgeRule("Person", "knows", "Person", BoMPropertiesPolicy.NONE))
    }

    @Test
    fun shouldRoundTripBatchWriteAndSelectSubgraph() {
        val existingId = UUID.randomUUID()
        val seed = BoMGraph(
            entities = mutableListOf(
                BoMEntity(
                    id = existingId,
                    type = "Person",
                    schemaVersion = "1",
                    payload = mutableMapOf("name" to "Existing"),
                    annotations = mutableMapOf("item" to "X"),
                ),
            ),
        )
        assertThat(store.write(seed).isValid).isTrue()

        val neu = UUID.randomUUID()
        val batch = BoMGraph(
            entities = mutableListOf(
                BoMEntity(
                    id = neu,
                    type = "Person",
                    schemaVersion = "1",
                    payload = mutableMapOf("name" to "New"),
                    annotations = mutableMapOf("item" to "X", "src" to "ui"),
                ),
            ),
            edges = mutableListOf(
                BoMEdge(source = neu, target = existingId, role = "knows"),
            ),
        )
        assertThat(store.write(batch).isValid).isTrue()

        val loaded = store.loadAll()
        assertThat(loaded.entities).hasSize(2)
        assertThat(loaded.edges).hasSize(1)

        val sub = store.selectSubgraphMatchAll(mapOf("item" to "X", "src" to "ui"))
        assertThat(sub.entities).hasSize(1)
        assertThat(sub.edges).isEmpty()

        val allX = store.selectSubgraphMatchAll(mapOf("item" to "X"))
        assertThat(allX.entities).hasSize(2)
        assertThat(allX.edges).hasSize(1)
    }

    @Test
    fun shouldRejectInvalidBatch() {
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf()),
            ),
        )
        val result = store.write(graph)
        assertThat(result.isValid).isFalse()
        assertThat(store.loadAll().entities).isEmpty()
    }

    @Test
    fun shouldAssignIdsOnWrite_whenMissing() {
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(
                    type = "Person",
                    schemaVersion = "1",
                    payload = mutableMapOf("name" to "A"),
                ),
            ),
        )
        assertThat(store.write(graph).isValid).isTrue()
        assertThat(graph.entities[0].id).isNotNull()
        assertThat(store.loadAll().entities).hasSize(1)
        assertThat(store.loadAll().entities[0].id).isEqualTo(graph.entities[0].id)
    }

    @Test
    fun shouldBatchDeleteEntitiesAndIncidentEdges() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                BoMEntity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
            ),
            edges = mutableListOf(
                BoMEdge(source = a, target = b, role = "knows"),
            ),
        )
        assertThat(store.write(graph).isValid).isTrue()
        val edgeId = graph.edges[0].id!!

        assertThat(store.delete(entityIds = listOf(a)).isValid).isTrue()
        val loaded = store.loadAll()
        assertThat(loaded.entities.map { it.id }).containsExactly(b)
        assertThat(loaded.edges).isEmpty()
        assertThat(store.delete(edgeIds = listOf(edgeId)).isValid).isFalse()
    }

    @Test
    fun shouldFailBatchDelete_whenUnknownId_andLeaveStoreUnchanged() {
        val a = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val result = store.delete(entityIds = listOf(a, UUID.randomUUID()))
        assertThat(result.isValid).isFalse()
        assertThat(store.loadAll().entities).hasSize(1)
    }

    @Test
    fun shouldRejectEmptyBatchDelete() {
        assertThat(store.delete().isValid).isFalse()
    }
}
