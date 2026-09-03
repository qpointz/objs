package org.poc.objs.core.persistence

import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GraphPersistenceTest : ObjsPersistenceFixture() {

    @Test
    fun shouldCascadeEntityMembership_whenEntityDeleted() {
        uow.write {
        val e1 = UUID.randomUUID()
        val e2 = UUID.randomUUID()
        entities.saveAll(
            listOf(
                EntityRecord(
                    id = e1,
                    type = "Person",
                    schemaVersion = "1",
                    payload = mutableMapOf("name" to "A"),
                    annotations = mutableMapOf("k" to "v"),
                ),
                EntityRecord(
                    id = e2,
                    type = "Person",
                    schemaVersion = "1",
                    payload = mutableMapOf("name" to "B"),
                    annotations = mutableMapOf(),
                ),
            ),
        )

        val sgId = UUID.randomUUID()
        graphs.save(GraphRecord(id = sgId, annotations = mutableMapOf("pack" to "demo")))
        memberships.saveAll(
            listOf(
                GraphMembershipRecord(graphId = sgId, entityId = e1),
                GraphMembershipRecord(graphId = sgId, entityId = e2),
            ),
        )

        entities.deleteById(e1)
        entities.flush()
        uow.entityManager().clear()

        assertThat(memberships.findByGraphId(sgId).map { it.entityId }).containsExactly(e2)
        assertThat(graphs.findById(sgId)).isNotNull
        }
    }

    @Test
    fun shouldCascadeEdgeDelete_whenGraphDeleted() {
        uow.write {
        val e1 = UUID.randomUUID()
        val e2 = UUID.randomUUID()
        entities.saveAll(
            listOf(
                EntityRecord(id = e1, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
                EntityRecord(id = e2, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
            ),
        )
        val sgId = UUID.randomUUID()
        graphs.save(GraphRecord(id = sgId, annotations = mutableMapOf()))
        memberships.saveAll(
            listOf(
                GraphMembershipRecord(graphId = sgId, entityId = e1),
                GraphMembershipRecord(graphId = sgId, entityId = e2),
            ),
        )
        val edgeId = UUID.randomUUID()
        edges.save(EdgeRecord(id = edgeId, graphId = sgId, sourceId = e1, targetId = e2, role = "knows"))
        edges.flush()

        graphs.deleteById(sgId)
        graphs.flush()
        uow.entityManager().clear()

        assertThat(edges.existsById(edgeId)).isFalse()
        assertThat(entities.existsById(e1)).isTrue()
        assertThat(entities.existsById(e2)).isTrue()
        }
    }

    @Test
    fun shouldCascadeEdgeDelete_whenEndpointEntityDeleted() {
        uow.write {
        val e1 = UUID.randomUUID()
        val e2 = UUID.randomUUID()
        entities.saveAll(
            listOf(
                EntityRecord(id = e1, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
                EntityRecord(id = e2, type = "Person", schemaVersion = "1", payload = mutableMapOf(), annotations = mutableMapOf()),
            ),
        )
        val sgId = UUID.randomUUID()
        graphs.save(GraphRecord(id = sgId, annotations = mutableMapOf()))
        val edgeId = UUID.randomUUID()
        edges.save(EdgeRecord(id = edgeId, graphId = sgId, sourceId = e1, targetId = e2, role = "knows"))

        edges.deleteById(edgeId)
        edges.flush()
        uow.entityManager().clear()

        assertThat(edges.findByGraphId(sgId)).isEmpty()
        assertThat(entities.existsById(e1)).isTrue()
        }
    }

    @Test
    fun shouldCascadeMembership_whenGraphDeleted_withoutDeletingGraphObjects() {
        uow.write {
        val e1 = UUID.randomUUID()
        entities.save(
            EntityRecord(
                id = e1,
                type = "Person",
                schemaVersion = "1",
                payload = mutableMapOf("name" to "A"),
                annotations = mutableMapOf(),
            ),
        )
        val sgId = UUID.randomUUID()
        graphs.save(GraphRecord(id = sgId, annotations = mutableMapOf("x" to "y")))
        memberships.save(GraphMembershipRecord(graphId = sgId, entityId = e1))

        graphs.deleteById(sgId)
        graphs.flush()
        uow.entityManager().clear()

        assertThat(graphs.findById(sgId)).isNull()
        assertThat(memberships.findByGraphId(sgId)).isEmpty()
        assertThat(entities.findById(e1)).isNotNull
        }
    }
}
