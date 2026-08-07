package org.poc.objs.core.persistence

import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-subgraph;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class BoMSubgraphPersistenceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var entities: BoMEntityRepository

    @Autowired
    lateinit var edges: BoMEdgeRepository

    @Autowired
    lateinit var subgraphs: BoMSubgraphRepository

    @Autowired
    lateinit var subgraphEntities: BoMSubgraphEntityRepository

    @Autowired
    lateinit var subgraphEdges: BoMSubgraphEdgeRepository

    @Test
    fun shouldCascadeEntityMembership_whenEntityDeleted() {
        val e1 = UUID.randomUUID()
        val e2 = UUID.randomUUID()
        entities.saveAll(
            listOf(
                BoMEntityRecord(
                    id = e1,
                    type = "Person",
                    schemaVersion = "1",
                    payload = mutableMapOf("name" to "A"),
                    annotations = mutableMapOf("k" to "v"),
                ),
                BoMEntityRecord(
                    id = e2,
                    type = "Person",
                    schemaVersion = "1",
                    payload = mutableMapOf("name" to "B"),
                    annotations = mutableMapOf(),
                ),
            ),
        )

        val sgId = UUID.randomUUID()
        subgraphs.save(BoMSubgraphRecord(id = sgId, annotations = mutableMapOf("pack" to "demo")))
        subgraphEntities.saveAll(
            listOf(
                BoMSubgraphEntityRecord(subgraphId = sgId, entityId = e1),
                BoMSubgraphEntityRecord(subgraphId = sgId, entityId = e2),
            ),
        )

        entities.deleteById(e1)
        entities.flush()

        assertThat(subgraphEntities.findBySubgraphId(sgId).map { it.entityId }).containsExactly(e2)
        assertThat(subgraphs.findById(sgId)).isPresent
    }

    @Test
    fun shouldCascadeEdgeMembership_whenEdgeDeleted() {
        val e1 = UUID.randomUUID()
        val e2 = UUID.randomUUID()
        entities.saveAll(
            listOf(
                BoMEntityRecord(id = e1, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
                BoMEntityRecord(id = e2, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
            ),
        )
        val edgeId = UUID.randomUUID()
        edges.save(BoMEdgeRecord(id = edgeId, sourceId = e1, targetId = e2, role = "knows"))

        val sgId = UUID.randomUUID()
        subgraphs.save(BoMSubgraphRecord(id = sgId, annotations = mutableMapOf()))
        subgraphEntities.saveAll(
            listOf(
                BoMSubgraphEntityRecord(subgraphId = sgId, entityId = e1),
                BoMSubgraphEntityRecord(subgraphId = sgId, entityId = e2),
            ),
        )
        subgraphEdges.save(BoMSubgraphEdgeRecord(subgraphId = sgId, edgeId = edgeId))

        edges.deleteById(edgeId)
        edges.flush()

        assertThat(subgraphEdges.findBySubgraphId(sgId)).isEmpty()
        assertThat(subgraphEntities.countBySubgraphId(sgId)).isEqualTo(2)
        assertThat(entities.existsById(e1)).isTrue()
    }

    @Test
    fun shouldCascadeMembership_whenSubgraphDeleted_withoutDeletingGraphObjects() {
        val e1 = UUID.randomUUID()
        entities.save(
            BoMEntityRecord(
                id = e1,
                type = "Person",
                schemaVersion = "1",
                payload = mutableMapOf("name" to "A"),
                annotations = mutableMapOf(),
            ),
        )
        val sgId = UUID.randomUUID()
        subgraphs.save(BoMSubgraphRecord(id = sgId, annotations = mutableMapOf("x" to "y")))
        subgraphEntities.save(BoMSubgraphEntityRecord(subgraphId = sgId, entityId = e1))

        subgraphs.deleteById(sgId)
        subgraphs.flush()

        assertThat(subgraphs.findById(sgId)).isEmpty
        assertThat(subgraphEntities.findBySubgraphId(sgId)).isEmpty()
        assertThat(entities.findById(e1)).isPresent
    }
}
