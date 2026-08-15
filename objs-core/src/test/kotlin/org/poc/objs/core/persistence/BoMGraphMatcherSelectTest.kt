package org.poc.objs.core.persistence

import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowableOfType
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
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.match.BoMGraphExprMatcher
import org.poc.objs.core.match.BoMGraphIdsMatcher
import org.poc.objs.core.match.BoMMatcherDsl
import org.poc.objs.core.match.BoMMatcherFormat
import org.poc.objs.core.match.BoMObjExprMatcher
import org.poc.objs.core.validation.BoMValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

/**
 * `graph-expr` / `obj-expr` selection through [BoMGraphStore.select] /
 * [BoMGraphStore.selectInGraph], including the G-G16 fail-closed guard.
 */
@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class, BoMNamedGraphStore::class, BoMPoolEntityReader::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-graph-match;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class BoMGraphMatcherSelectTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var graphStore: BoMGraphStore

    @Autowired
    lateinit var namedGraphs: BoMNamedGraphStore

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
        assertThat(
            graphStore.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                        BoMEntity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
                    ),
                ),
            ).isValid,
        ).isTrue()
        packId = namedGraphs.create(
            BoMGraphSpec(
                annotations = mapOf("decisionId" to "D-1"),
                entityIds = setOf(a, b),
            ),
        ).id
        edgeId = UUID.randomUUID()
        assertThat(
            graphStore.write(
                BoMGraph(
                    edges = mutableListOf(
                        BoMEdge(id = edgeId, graphId = packId, source = a, target = b, role = "knows"),
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
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = orphan, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Orphan")),
                    ),
                ),
            ).isValid,
        ).isTrue()
        val g2 = namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("decisionId" to "D-2"), entityIds = setOf(a)),
        ).id
        // Same entity a in two graphs; all must return a once.
        val sg = graphStore.select(dsl.decode("""{"all":true}""", BoMMatcherFormat.JSON))
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
            BoMMatcherFormat.JSON,
        )
        val sg = graphStore.select(chain)
        assertThat(sg.entities.map { it.id }).containsExactly(a)
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldSelectByGraphExprId() {
        val sg = graphStore.select(BoMGraphExprMatcher("id == '$packId'"))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(sg.edges.map { it.id }).containsExactly(edgeId)
    }

    @Test
    fun shouldReturnEmpty_whenGraphExprMatchesNoGraph() {
        val sg = graphStore.select(BoMGraphExprMatcher("id == '${UUID.randomUUID()}'"))
        assertThat(sg.entities).isEmpty()
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldSelectByGraphsInUnion() {
        val c = UUID.randomUUID()
        assertThat(
            graphStore.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = c, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "C")),
                    ),
                ),
            ).isValid,
        ).isTrue()
        val second = namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("decisionId" to "D-2"), entityIds = setOf(c)),
        ).id

        val sg = graphStore.select(BoMGraphIdsMatcher(listOf(packId, second)))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b, c))

        val empty = graphStore.select(BoMGraphIdsMatcher(emptyList()))
        assertThat(empty.entities).isEmpty()

        val chain = dsl.decode(
            """[{"graphs-in":["$packId"]},{"obj-expr":"p.name == 'A'"}]""",
            BoMMatcherFormat.JSON,
        )
        val filtered = graphStore.select(chain)
        assertThat(filtered.entities.map { it.id }).containsExactly(a)
    }

    @Test
    fun shouldSelectByGraphExprAnnotation() {
        val sg = graphStore.select(BoMGraphExprMatcher("a.decisionId == 'D-1'"))
        assertThat(sg.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(sg.edges).hasSize(1)
    }

    @Test
    fun shouldChainGraphExprThenObjExpr() {
        val chain = dsl.decode(
            """[{"graph-expr":"a.decisionId == 'D-1'"},{"obj-expr":"p.name == 'A'"}]""",
            BoMMatcherFormat.JSON,
        )
        val sg = graphStore.select(chain)
        assertThat(sg.entities.map { it.id }).containsExactly(a)
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldRejectBareObjExpr_forSelect() {
        val ex = catchThrowableOfType(BoMValidationException::class.java) {
            graphStore.select(BoMObjExprMatcher("type == 'Person'"))
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
            BoMMatcherFormat.JSON,
        )
        assertThatThrownBy { graphStore.select(chain) }
            .isInstanceOf(BoMValidationException::class.java)
    }

    @Test
    fun shouldSelectInGraph_filteringKnownGraphMembersByObjExpr() {
        val sg = graphStore.selectInGraph(packId, BoMObjExprMatcher("p.name == 'A'"))
        assertThat(sg.entities.map { it.id }).containsExactly(a)
        assertThat(sg.edges).isEmpty()
    }

    @Test
    fun shouldSelectInGraph_excludeEntitiesOutsideTheGraph() {
        // Orphan entity matching the same obj-expr predicate, but not a member of `packId`.
        val orphan = UUID.randomUUID()
        assertThat(
            graphStore.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = orphan, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val sg = graphStore.selectInGraph(packId, BoMObjExprMatcher("p.name == 'A'"))
        assertThat(sg.entities.map { it.id }).containsExactly(a)
    }

    @Test
    fun shouldFailSelectInGraph_whenGraphUnknown() {
        val ex = catchThrowableOfType(BoMValidationException::class.java) {
            graphStore.selectInGraph(UUID.randomUUID(), BoMObjExprMatcher("type == 'Person'"))
        }
        assertThat(ex).isNotNull()
        assertThat(ex.result.issues).anySatisfy { issue -> assertThat(issue.code).isEqualTo("GRAPH_NOT_FOUND") }
    }
}
