package org.poc.objs.core.persistence

import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
import org.poc.objs.core.domain.BoMSubgraphException
import org.poc.objs.core.domain.BoMSubgraphSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class, BoMSubgraphStore::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-subgraph-store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class BoMSubgraphStoreTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var graphStore: BoMGraphStore

    @Autowired
    lateinit var subgraphStore: BoMSubgraphStore

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @Autowired
    lateinit var allowed: BoMAllowedEdgeCatalog

    @Autowired
    lateinit var entityRepository: BoMEntityRepository

    private lateinit var a: UUID
    private lateinit var b: UUID
    private lateinit var edgeId: UUID

    @BeforeEach
    fun seed() {
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

        a = UUID.randomUUID()
        b = UUID.randomUUID()
        edgeId = UUID.randomUUID()
        assertThat(
            graphStore.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(
                            id = a,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "A"),
                            annotations = mutableMapOf("t" to "1"),
                        ),
                        BoMEntity(
                            id = b,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "B"),
                            annotations = mutableMapOf(),
                        ),
                    ),
                    edges = mutableListOf(
                        BoMEdge(id = edgeId, source = a, target = b, role = "knows"),
                    ),
                ),
            ).isValid,
        ).isTrue()
    }

    @Test
    fun shouldRoundTripCreateAndGet_preservingIds() {
        val created = subgraphStore.create(
            BoMSubgraphSpec(
                annotations = mapOf("pack" to "p1"),
                entityIds = setOf(a, b),
                edgeIds = setOf(edgeId),
            ),
        )
        val got = subgraphStore.get(created.id)!!
        assertThat(got.annotations).containsEntry("pack", "p1")
        assertThat(got.subgraph.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(got.subgraph.edges.map { it.id }).containsExactly(edgeId)
        assertThat(got.subgraph.entities.find { it.id == a }!!.payload["name"]).isEqualTo("A")
    }

    @Test
    fun shouldResolveLatestPayload_withoutRewritingMembership() {
        val created = subgraphStore.create(
            BoMSubgraphSpec(entityIds = setOf(a, b), edgeIds = setOf(edgeId)),
        )
        assertThat(
            graphStore.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(
                            id = a,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "A-updated"),
                            annotations = mutableMapOf("t" to "1"),
                        ),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val got = subgraphStore.get(created.id)!!
        assertThat(got.subgraph.entities.find { it.id == a }!!.payload["name"]).isEqualTo("A-updated")
        assertThat(got.subgraph.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
    }

    @Test
    fun shouldRejectEdgeWithoutEndpointMembers() {
        assertThatThrownBy {
            subgraphStore.create(
                BoMSubgraphSpec(entityIds = setOf(a), edgeIds = setOf(edgeId)),
            )
        }.isInstanceOf(BoMSubgraphException::class.java)
            .extracting("code")
            .isEqualTo("SUBGRAPH_EDGE_ENDPOINTS")
    }

    @Test
    fun shouldRejectMissingEntityId() {
        val missing = UUID.randomUUID()
        assertThatThrownBy {
            subgraphStore.create(BoMSubgraphSpec(entityIds = setOf(missing)))
        }.isInstanceOf(BoMSubgraphException::class.java)
            .extracting("code")
            .isEqualTo("SUBGRAPH_ENTITY_MISSING")
    }

    @Test
    fun shouldDeleteSubgraph_leavingGraphObjects() {
        val created = subgraphStore.create(
            BoMSubgraphSpec(entityIds = setOf(a, b), edgeIds = setOf(edgeId)),
        )
        subgraphStore.delete(created.id)
        assertThat(subgraphStore.get(created.id)).isNull()
        assertThat(entityRepository.existsById(a)).isTrue()
        assertThat(entityRepository.existsById(b)).isTrue()
    }

    @Test
    fun shouldSnapshot_cloningMembersAndStampingAnnotations() {
        val source = subgraphStore.create(
            BoMSubgraphSpec(
                annotations = mapOf("live" to "true"),
                entityIds = setOf(a, b),
                edgeIds = setOf(edgeId),
            ),
        )
        val stamp = mapOf("decisionId" to "D-9", "t" to "snap")
        val hard = subgraphStore.snapshot(source.id, stamp)

        assertThat(hard.id).isNotEqualTo(source.id)
        assertThat(hard.annotations).isEqualTo(stamp)
        assertThat(hard.subgraph.entities).hasSize(2)
        assertThat(hard.subgraph.edges).hasSize(1)
        hard.subgraph.entities.forEach { entity ->
            assertThat(entity.id).isNotIn(a, b)
            assertThat(entity.annotations).containsEntry("decisionId", "D-9")
        }
        val cloneEdge = hard.subgraph.edges.single()
        assertThat(cloneEdge.id).isNotEqualTo(edgeId)
        assertThat(cloneEdge.source).isIn(hard.subgraph.entities.map { it.id })
        assertThat(cloneEdge.target).isIn(hard.subgraph.entities.map { it.id })

        val stillLive = subgraphStore.get(source.id)!!
        assertThat(stillLive.subgraph.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(stillLive.annotations).containsEntry("live", "true")
    }
}
