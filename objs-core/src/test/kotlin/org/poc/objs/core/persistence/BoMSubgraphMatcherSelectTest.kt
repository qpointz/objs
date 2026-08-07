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
import org.poc.objs.core.domain.BoMSubgraphSpec
import org.poc.objs.core.match.BoMMatcherDsl
import org.poc.objs.core.match.BoMMatcherFormat
import org.poc.objs.core.match.BoMObjExprMatcher
import org.poc.objs.core.match.BoMSubgExprMatcher
import org.poc.objs.core.match.BoMSubgraphIdMatcher
import org.poc.objs.core.validation.BoMValidationException
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
        "spring.datasource.url=jdbc:h2:mem:objs-subg-match;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class BoMSubgraphMatcherSelectTest {

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

    private val dsl = BoMMatcherDsl.create()

    private lateinit var a: UUID
    private lateinit var b: UUID
    private lateinit var edgeId: UUID
    private lateinit var packId: UUID

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
                    "Person",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "n"))),
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
                        BoMEntity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                        BoMEntity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
                    ),
                    edges = mutableListOf(BoMEdge(id = edgeId, source = a, target = b, role = "knows")),
                ),
            ).isValid,
        ).isTrue()
        packId = subgraphStore.create(
            BoMSubgraphSpec(
                annotations = mapOf("decisionId" to "D-1"),
                entityIds = setOf(a, b),
                edgeIds = setOf(edgeId),
            ),
        ).id
    }

    @Test
    fun shouldSelectBySubgraphId() {
        val sg = graphStore.selectSubgraph(BoMSubgraphIdMatcher(packId))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(sg.edges.map { it.id }).containsExactly(edgeId)
    }

    @Test
    fun shouldFail_whenSubgraphIdUnknown() {
        assertThatThrownBy {
            graphStore.selectSubgraph(BoMSubgraphIdMatcher(UUID.randomUUID()))
        }.isInstanceOf(BoMValidationException::class.java)
    }

    @Test
    fun shouldSelectBySubgExpr() {
        val sg = graphStore.selectSubgraph(BoMSubgExprMatcher("a.decisionId == 'D-1'"))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(sg.edges).hasSize(1)
    }

    @Test
    fun shouldChainSubgExprThenObjExpr() {
        val chain = dsl.decode(
            """[{"subg-expr":"a.decisionId == 'D-1'"},{"obj-expr":"p.name == 'A'"}]""",
            BoMMatcherFormat.JSON,
        )
        val sg = graphStore.selectSubgraph(chain)
        assertThat(sg.entities.map { it.id }).containsExactly(a)
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldDecodeSubgraphIdSugar() {
        val matcher = dsl.decode("""{"subgraph":{"id":"$packId"}}""", BoMMatcherFormat.JSON)
        assertThat(matcher).isInstanceOf(BoMSubgraphIdMatcher::class.java)
        assertThat((matcher as BoMSubgraphIdMatcher).id).isEqualTo(packId)
    }
}
