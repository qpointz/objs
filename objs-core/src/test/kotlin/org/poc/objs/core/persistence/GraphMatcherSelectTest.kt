package org.poc.objs.core.persistence

import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.PropertiesPolicy
import org.poc.objs.core.domain.Schema
import org.poc.objs.core.domain.SchemaCatalog
import org.poc.objs.core.domain.SchemaDsl
import org.poc.objs.core.domain.GraphSpec
import org.poc.objs.core.match.GraphExprMatcher
import org.poc.objs.core.match.GraphIdsMatcher
import org.poc.objs.core.match.MatcherDsl
import org.poc.objs.core.match.MatcherFormat
import org.poc.objs.core.match.ObjExprMatcher
import org.poc.objs.core.validation.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

/**
 * `graph-expr` / `obj-expr` selection through [GraphStore.select] /
 * [GraphStore.selectInGraph], including the G-G16 fail-closed guard.
 */
@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(GraphStore::class, NamedGraphStore::class, PoolEntityReader::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-graph-match;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=false",
    ],
)
class GraphMatcherSelectTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var graphStore: GraphStore

    @Autowired
    lateinit var namedGraphs: NamedGraphStore

    @Autowired
    lateinit var schemas: SchemaCatalog

    @Autowired
    lateinit var allowed: AllowedEdgeCatalog

    private val dsl = MatcherDsl.create()

    private lateinit var a: UUID
    private lateinit var b: UUID
    private lateinit var edgeId: UUID
    private lateinit var packId: UUID

    @BeforeEach
    fun seed() {
        schemas.clear()
        allowed.clear()
        schemas.register(
            Schema(
                "Person",
                "1",
                SchemaDsl.obj(
                    "Person",
                    "Person",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "n"))),
                ),
            ),
        )
        allowed.register(AllowedEdgeRule("Person", "knows", "Person", PropertiesPolicy.NONE))
        a = UUID.randomUUID()
        b = UUID.randomUUID()
        assertThat(
            graphStore.write(
                Graph(
                    entities = mutableListOf(
                        Entity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                        Entity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
                    ),
                ),
            ).isValid,
        ).isTrue()
        packId = namedGraphs.create(
            GraphSpec(
                annotations = mapOf("decisionId" to "D-1"),
                entityIds = setOf(a, b),
            ),
        ).id
        edgeId = UUID.randomUUID()
        assertThat(
            graphStore.write(
                Graph(
                    edges = mutableListOf(
                        Edge(id = edgeId, graphId = packId, source = a, target = b, role = "knows"),
                    ),
                ),
            ).isValid,
        ).isTrue()
    }

    @Test
    fun shouldSelectAllGraphs_unionDistinctById() {
        val orphan = UUID.randomUUID()
        assertThat(
            graphStore.write(
                Graph(
                    entities = mutableListOf(
                        Entity(id = orphan, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Orphan")),
                    ),
                ),
            ).isValid,
        ).isTrue()
        val g2 = namedGraphs.create(
            GraphSpec(annotations = mapOf("decisionId" to "D-2"), entityIds = setOf(a)),
        ).id
        // Same entity a in two graphs; all must return a once.
        val sg = graphStore.select(dsl.decode("""{"all":true}""", MatcherFormat.JSON))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(sg.entities.map { it.id }).hasSize(2)
        assertThat(sg.edges.map { it.id }).containsExactly(edgeId)
        assertThat(sg.entities.map { it.id }).doesNotContain(orphan)
        assertThat(namedGraphs.get(g2)).isNotNull()
    }

    @Test
    fun shouldChainAllThenObjExpr() {
        val chain = dsl.decode(
            """[{"all":true},{"obj-expr":"p.name == 'A'"}]""",
            MatcherFormat.JSON,
        )
        val sg = graphStore.select(chain)
        assertThat(sg.entities.map { it.id }).containsExactly(a)
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldSelectByGraphExprId() {
        val sg = graphStore.select(GraphExprMatcher("id == '$packId'"))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(sg.edges.map { it.id }).containsExactly(edgeId)
    }

    @Test
    fun shouldReturnEmpty_whenGraphExprMatchesNoGraph() {
        val sg = graphStore.select(GraphExprMatcher("id == '${UUID.randomUUID()}'"))
        assertThat(sg.entities).isEmpty()
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldSelectByGraphsInUnion() {
        val c = UUID.randomUUID()
        assertThat(
            graphStore.write(
                Graph(
                    entities = mutableListOf(
                        Entity(id = c, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "C")),
                    ),
                ),
            ).isValid,
        ).isTrue()
        val second = namedGraphs.create(
            GraphSpec(annotations = mapOf("decisionId" to "D-2"), entityIds = setOf(c)),
        ).id

        val sg = graphStore.select(GraphIdsMatcher(listOf(packId, second)))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b, c))

        val empty = graphStore.select(GraphIdsMatcher(emptyList()))
        assertThat(empty.entities).isEmpty()

        val chain = dsl.decode(
            """[{"graphs-in":["$packId"]},{"obj-expr":"p.name == 'A'"}]""",
            MatcherFormat.JSON,
        )
        val filtered = graphStore.select(chain)
        assertThat(filtered.entities.map { it.id }).containsExactly(a)
    }

    @Test
    fun shouldSelectByGraphExprAnnotation() {
        val sg = graphStore.select(GraphExprMatcher("a.decisionId == 'D-1'"))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(sg.edges).hasSize(1)
    }

    @Test
    fun shouldChainGraphExprThenObjExpr() {
        val chain = dsl.decode(
            """[{"graph-expr":"a.decisionId == 'D-1'"},{"obj-expr":"p.name == 'A'"}]""",
            MatcherFormat.JSON,
        )
        val sg = graphStore.select(chain)
        assertThat(sg.entities.map { it.id }).containsExactly(a)
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldRejectBareObjExpr_forSelect() {
        val ex = catchThrowableOfType(ValidationException::class.java) {
            graphStore.select(ObjExprMatcher("type == 'Person'"))
        }
        assertThat(ex).isNotNull()
        assertThat(ex.result.issues).anySatisfy { issue ->
            assertThat(issue.code).isEqualTo("MATCHER_GRAPH_SCOPE_REQUIRED")
        }
    }

    @Test
    fun shouldRejectChainStartingWithObjExpr_forSelect() {
        val chain = dsl.decode(
            """[{"obj-expr":"p.name == 'A'"},{"graph-expr":"a.decisionId == 'D-1'"}]""",
            MatcherFormat.JSON,
        )
        assertThatThrownBy { graphStore.select(chain) }
            .isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun shouldSelectInGraph_filteringKnownGraphMembersByObjExpr() {
        val sg = graphStore.selectInGraph(packId, ObjExprMatcher("p.name == 'A'"))
        assertThat(sg.entities.map { it.id }).containsExactly(a)
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldSelectInGraph_excludeEntitiesOutsideTheGraph() {
        // Orphan entity matching the same obj-expr predicate, but not a member of `packId`.
        val orphan = UUID.randomUUID()
        assertThat(
            graphStore.write(
                Graph(
                    entities = mutableListOf(
                        Entity(id = orphan, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val sg = graphStore.selectInGraph(packId, ObjExprMatcher("p.name == 'A'"))
        assertThat(sg.entities.map { it.id }).containsExactly(a)
    }

    @Test
    fun shouldFailSelectInGraph_whenGraphUnknown() {
        val ex = catchThrowableOfType(ValidationException::class.java) {
            graphStore.selectInGraph(UUID.randomUUID(), ObjExprMatcher("type == 'Person'"))
        }
        assertThat(ex).isNotNull()
        assertThat(ex.result.issues).anySatisfy { issue -> assertThat(issue.code).isEqualTo("GRAPH_NOT_FOUND") }
    }
}
