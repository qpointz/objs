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
        "spring.datasource.url=jdbc:h2:mem:objs-graph-persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class BoMGraphPersistenceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var entities: BoMEntityRepository

    @Autowired
    lateinit var edges: BoMEdgeRepository

    @Autowired
    lateinit var graphs: BoMGraphRepository

    @Autowired
    lateinit var memberships: BoMGraphMembershipRepository

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
        graphs.save(BoMGraphRecord(id = sgId, annotations = mutableMapOf("pack" to "demo")))
        memberships.saveAll(
            listOf(
                BoMGraphMembershipRecord(graphId = sgId, entityId = e1),
                BoMGraphMembershipRecord(graphId = sgId, entityId = e2),
            ),
        )

        entities.deleteById(e1)
        entities.flush()

        assertThat(memberships.findByGraphId(sgId).map { it.entityId }).containsExactly(e2)
        assertThat(graphs.findById(sgId)).isPresent
    }

    @Test
    fun shouldCascadeEdgeDelete_whenGraphDeleted() {
        val e1 = UUID.randomUUID()
        val e2 = UUID.randomUUID()
        entities.saveAll(
            listOf(
                BoMEntityRecord(id = e1, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
                BoMEntityRecord(id = e2, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
            ),
        )
        val sgId = UUID.randomUUID()
        graphs.save(BoMGraphRecord(id = sgId, annotations = mutableMapOf()))
        memberships.saveAll(
            listOf(
                BoMGraphMembershipRecord(graphId = sgId, entityId = e1),
                BoMGraphMembershipRecord(graphId = sgId, entityId = e2),
            ),
        )
        val edgeId = UUID.randomUUID()
        edges.save(BoMEdgeRecord(id = edgeId, graphId = sgId, sourceId = e1, targetId = e2, role = "knows"))
        edges.flush()

        graphs.deleteById(sgId)
        graphs.flush()

        assertThat(edges.existsById(edgeId)).isFalse()
        assertThat(entities.existsById(e1)).isTrue()
        assertThat(entities.existsById(e2)).isTrue()
    }

    @Test
    fun shouldCascadeEdgeDelete_whenEndpointEntityDeleted() {
        val e1 = UUID.randomUUID()
        val e2 = UUID.randomUUID()
        entities.saveAll(
            listOf(
                BoMEntityRecord(id = e1, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
                BoMEntityRecord(id = e2, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
            ),
        )
        val sgId = UUID.randomUUID()
        graphs.save(BoMGraphRecord(id = sgId, annotations = mutableMapOf()))
        val edgeId = UUID.randomUUID()
        edges.save(BoMEdgeRecord(id = edgeId, graphId = sgId, sourceId = e1, targetId = e2, role = "knows"))

        edges.deleteById(edgeId)
        edges.flush()

        assertThat(edges.findByGraphId(sgId)).isEmpty()
        assertThat(entities.existsById(e1)).isTrue()
    }

    @Test
    fun shouldCascadeMembership_whenGraphDeleted_withoutDeletingGraphObjects() {
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
        graphs.save(BoMGraphRecord(id = sgId, annotations = mutableMapOf("x" to "y")))
        memberships.save(BoMGraphMembershipRecord(graphId = sgId, entityId = e1))

        graphs.deleteById(sgId)
        graphs.flush()

        assertThat(graphs.findById(sgId)).isEmpty
        assertThat(memberships.findByGraphId(sgId)).isEmpty()
        assertThat(entities.findById(e1)).isPresent
    }
}
