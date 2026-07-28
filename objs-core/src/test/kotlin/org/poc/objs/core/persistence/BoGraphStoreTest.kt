package org.poc.objs.core.persistence

import java.util.UUID

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoAllowedEdgeCatalog
import org.poc.objs.core.domain.BoAllowedEdgeRule
import org.poc.objs.core.domain.BoEdge
import org.poc.objs.core.domain.BoEntity
import org.poc.objs.core.domain.BoGraph
import org.poc.objs.core.domain.BoPropertiesPolicy
import org.poc.objs.core.domain.BoSchema
import org.poc.objs.core.domain.BoSchemaCatalog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoGraphStore::class)
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
class BoGraphStoreTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var store: BoGraphStore

    @Autowired
    lateinit var schemas: BoSchemaCatalog

    @Autowired
    lateinit var allowed: BoAllowedEdgeCatalog

    @BeforeEach
    fun catalogs() {
        schemas.clear()
        allowed.clear()
        schemas.register(
            BoSchema(
                type = "Person",
                version = "1",
                schema = mapOf(
                    "type" to "object",
                    "required" to listOf("name"),
                    "properties" to mapOf("name" to mapOf("type" to "string")),
                ),
            ),
        )
        allowed.register(BoAllowedEdgeRule("Person", "knows", "Person", BoPropertiesPolicy.NONE))
    }

    @Test
    fun shouldRoundTripBatchWriteAndSelectSubgraph() {
        val existingId = UUID.randomUUID()
        val seed = BoGraph(
            entities = mutableListOf(
                BoEntity(
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
        val batch = BoGraph(
            entities = mutableListOf(
                BoEntity(
                    id = neu,
                    type = "Person",
                    schemaVersion = "1",
                    payload = mutableMapOf("name" to "New"),
                    annotations = mutableMapOf("item" to "X", "src" to "ui"),
                ),
            ),
            edges = mutableListOf(
                BoEdge(source = neu, target = existingId, role = "knows"),
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
        val graph = BoGraph(
            entities = mutableListOf(
                BoEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf()),
            ),
        )
        val result = store.write(graph)
        assertThat(result.isValid).isFalse()
        assertThat(store.loadAll().entities).isEmpty()
    }
}
