package org.poc.objs.core.persistence

import java.util.UUID

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.domain.bomMutation
import org.poc.objs.core.domain.BoMPageRequest
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.match.BoMAllGraphsMatcher
import org.poc.objs.core.match.BoMObjExprMatcher
import org.poc.objs.core.validation.BoMValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class, BoMNamedGraphStore::class, BoMPoolEntityReader::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=false",
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

    @Autowired
    lateinit var graphRepository: BoMGraphRepository

    @Autowired
    lateinit var namedGraphs: BoMNamedGraphStore

    /** Edges require an owning graph (`graph_id` NOT NULL); every edge in this file shares [graphId]. */
    private lateinit var graphId: UUID

    @BeforeEach
    fun catalogs() {
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
        graphId = UUID.randomUUID()
        graphRepository.save(BoMGraphRecord(id = graphId))
    }

    @Test
    fun shouldRoundTripBatchWriteAndLoadAll() {
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
                BoMEdge(graphId = graphId, source = neu, target = existingId, role = "knows"),
            ),
        )
        assertThat(store.write(batch).isValid).isTrue()

        val loaded = store.loadAll()
        assertThat(loaded.entities).hasSize(2)
        assertThat(loaded.edges).hasSize(1)
    }

    /**
     * Pool ops (G-G3): entity CRUD requires no membership — an entity with zero
     * `bom_graph_entity` rows ("orphan") is a normal, fully-writable/readable pool member.
     */
    @Test
    fun shouldAllowOrphanEntity_withNoGraphMembership() {
        val orphan = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = orphan, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Orphan")),
                    ),
                ),
            ).isValid,
        ).isTrue()
        assertThat(store.loadAll().entities.map { it.id }).contains(orphan)
    }

    /**
     * G-G16: there is no global graph, so a bare `obj-expr` (no `graph-expr` stage-0, no fixed
     * graph scope) must not silently scan the whole pool as if it were one graph.
     */
    @Test
    fun shouldFailSelect_whenNoGraphScope() {
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val ex = catchThrowableOfType(BoMValidationException::class.java) {
            store.select(BoMObjExprMatcher("type == 'Person'"))
        }
        assertThat(ex.result.issues.map { it.code }).contains("MATCHER_GRAPH_SCOPE_REQUIRED")
    }

    @Test
    fun shouldSelectFromPool_includingOrphans_byType() {
        val orphan = UUID.randomUUID()
        val member = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = orphan, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Orphan")),
                        BoMEntity(id = member, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Member")),
                    ),
                ),
            ).isValid,
        ).isTrue()
        namedGraphs.attach(graphId, member)

        val hit = store.selectFromPool(BoMObjExprMatcher("type == 'Person'"))
        assertThat(hit.entities.map { it.id }).containsExactlyInAnyOrder(orphan, member)
        assertThat(hit.edges).isEmpty()
    }

    @Test
    fun shouldRejectSelectFromPool_whenGraphScopeMatcher() {
        val ex = catchThrowableOfType(BoMValidationException::class.java) {
            store.selectFromPool(BoMAllGraphsMatcher)
        }
        assertThat(ex.result.issues.map { it.code }).contains("MATCHER_POOL_OBJ_EXPR_ONLY")
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
                BoMEdge(graphId = graphId, source = a, target = b, role = "knows"),
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

    @Test
    fun shouldMutate_upsertAndDeleteInOneTransaction() {
        val keep = UUID.randomUUID()
        val remove = UUID.randomUUID()
        val neu = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = keep, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Keep")),
                        BoMEntity(id = remove, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Remove")),
                    ),
                    edges = mutableListOf(
                        BoMEdge(graphId = graphId, source = keep, target = remove, role = "knows"),
                    ),
                ),
            ).isValid,
        ).isTrue()
        val oldEdgeId = store.loadAll().edges.single().id!!

        val mutation = bomMutation {
            entities {
                set(BoMEntity(id = neu, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "New")))
                unset(remove)
            }
            edges {
                set(BoMEdge(graphId = graphId, source = keep, target = neu, role = "knows"))
                unset(oldEdgeId)
            }
        }
        assertThat(store.mutate(mutation).isValid).isTrue()

        val loaded = store.loadAll()
        assertThat(loaded.entities.map { it.id }).containsExactlyInAnyOrder(keep, neu)
        assertThat(loaded.edges).hasSize(1)
        assertThat(loaded.edges.single().target).isEqualTo(neu)
        assertThat(loaded.edges.none { it.id == oldEdgeId }).isTrue()
    }

    @Test
    fun shouldValidateMutation_rejectEdgeToDeletedEntity() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                        BoMEntity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val result = store.validateMutation(
            bomMutation {
                entities { unset(b) }
                edges { set(BoMEdge(graphId = graphId, source = a, target = b, role = "knows")) }
            },
        )
        assertThat(result.isValid).isFalse()
        assertThat(store.loadAll().entities).hasSize(2)
        assertThat(store.loadAll().edges).isEmpty()
    }

    @Test
    fun shouldValidateMutation_allowEdgeWhenDeletedEntityAlsoUpserted() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                        BoMEntity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val result = store.validateMutation(
            bomMutation {
                entities {
                    set(BoMEntity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B2")))
                    unset(b)
                }
                edges { set(BoMEdge(graphId = graphId, source = a, target = b, role = "knows")) }
            },
        )
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun shouldGetEntity_orNullWhenMissing() {
        val id = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = id, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        assertThat(store.getEntity(id)?.payload?.get("name")).isEqualTo("A")
        assertThat(store.getEntity(UUID.randomUUID())).isNull()
    }

    @Test
    fun shouldListEntities_ungroupedByGraph() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = a, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "A")),
                        BoMEntity(id = b, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "B")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        assertThat(store.listEntities().map { it.id }).containsExactlyInAnyOrder(a, b)
    }

    @Test
    fun shouldRejectUpdate_whenIdentifierFieldChanges() {
        schemas.clear()
        schemas.register(
            BoMSchema(
                "Person",
                "1",
                BoMSchemaDsl.obj(
                    "Person",
                    "Person payload",
                    listOf(
                        BoMSchemaDsl.field(
                            "name",
                            BoMSchemaDsl.string("Name", "Person name"),
                            identifier = true,
                        ),
                        BoMSchemaDsl.field(
                            "nickname",
                            BoMSchemaDsl.string("Nickname", "Optional nickname"),
                            required = false,
                        ),
                    ),
                ),
            ),
        )
        val id = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(
                            id = id,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "Alice", "nickname" to "A"),
                        ),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val renamed = store.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(
                        id = id,
                        type = "Person",
                        schemaVersion = "1",
                        payload = mutableMapOf("name" to "Bob", "nickname" to "A"),
                    ),
                ),
            ),
        )
        assertThat(renamed.isValid).isFalse()
        assertThat(renamed.issues.map { it.code }).contains("IDENTIFIER_IMMUTABLE")

        val nickOnly = store.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(
                        id = id,
                        type = "Person",
                        schemaVersion = "1",
                        payload = mutableMapOf("name" to "Alice", "nickname" to "Ally"),
                    ),
                ),
            ),
        )
        assertThat(nickOnly.isValid).isTrue()
        assertThat(store.getEntity(id)?.payload?.get("nickname")).isEqualTo("Ally")
    }

    @Test
    fun shouldAllowUpdate_whenSchemaVersionIntroducesIdentifierFields() {
        schemas.clear()
        schemas.register(
            BoMSchema(
                "Person",
                "1",
                BoMSchemaDsl.obj(
                    "Person",
                    "Person payload v1",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Person name"))),
                ),
            ),
        )
        schemas.register(
            BoMSchema(
                "Person",
                "2",
                BoMSchemaDsl.obj(
                    "Person",
                    "Person payload v2",
                    listOf(
                        BoMSchemaDsl.field(
                            "name",
                            BoMSchemaDsl.string("Name", "Person name"),
                            identifier = true,
                        ),
                    ),
                ),
            ),
        )
        val id = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(
                            id = id,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "Alice"),
                        ),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val migrate = store.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(
                        id = id,
                        type = "Person",
                        schemaVersion = "2",
                        payload = mutableMapOf("name" to "Alice"),
                    ),
                ),
            ),
        )
        assertThat(migrate.isValid).isTrue()
        assertThat(store.getEntity(id)?.schemaVersion).isEqualTo("2")

        val renameAfterIdentityExists = store.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(
                        id = id,
                        type = "Person",
                        schemaVersion = "2",
                        payload = mutableMapOf("name" to "Bob"),
                    ),
                ),
            ),
        )
        assertThat(renameAfterIdentityExists.isValid).isFalse()
        assertThat(renameAfterIdentityExists.issues.map { it.code }).contains("IDENTIFIER_IMMUTABLE")
    }

    @Test
    fun shouldAllowFill_whenStoredIdentifierIsBlank() {
        schemas.clear()
        schemas.register(
            BoMSchema(
                "Person",
                "1",
                BoMSchemaDsl.obj(
                    "Person",
                    "Person payload",
                    listOf(
                        BoMSchemaDsl.field(
                            "name",
                            BoMSchemaDsl.string("Name", "Person name"),
                            identifier = true,
                        ),
                        BoMSchemaDsl.field(
                            "code",
                            BoMSchemaDsl.string("Code", "Optional code"),
                            required = false,
                            identifier = true,
                        ),
                    ),
                ),
            ),
        )
        val id = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(
                            id = id,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "Alice", "code" to "  "),
                        ),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val fillBlank = store.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(
                        id = id,
                        type = "Person",
                        schemaVersion = "1",
                        payload = mutableMapOf("name" to "Alice", "code" to "P-1"),
                    ),
                ),
            ),
        )
        assertThat(fillBlank.isValid).isTrue()
        assertThat(store.getEntity(id)?.payload?.get("code")).isEqualTo("P-1")

        val changeFilled = store.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(
                        id = id,
                        type = "Person",
                        schemaVersion = "1",
                        payload = mutableMapOf("name" to "Alice", "code" to "P-2"),
                    ),
                ),
            ),
        )
        assertThat(changeFilled.isValid).isFalse()
        assertThat(changeFilled.issues.map { it.code }).contains("IDENTIFIER_IMMUTABLE")
    }

    @Test
    fun shouldAllowUpdate_whenSchemaVersionDropsIdentifierFields() {
        schemas.clear()
        schemas.register(
            BoMSchema(
                "Person",
                "1.0.0",
                BoMSchemaDsl.obj(
                    "Person",
                    "Person payload v1",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Person name"))),
                ),
            ),
        )
        schemas.register(
            BoMSchema(
                "Person",
                "2.0.0",
                BoMSchemaDsl.obj(
                    "Person",
                    "Person payload v2",
                    listOf(
                        BoMSchemaDsl.field(
                            "name",
                            BoMSchemaDsl.string("Name", "Person name"),
                            identifier = true,
                        ),
                    ),
                ),
            ),
        )
        val id = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(
                            id = id,
                            type = "Person",
                            schemaVersion = "2.0.0",
                            payload = mutableMapOf("name" to "Alice"),
                        ),
                    ),
                ),
            ).isValid,
        ).isTrue()

        val downgrade = store.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(
                        id = id,
                        type = "Person",
                        schemaVersion = "1.0.0",
                        payload = mutableMapOf("name" to "Alice"),
                    ),
                ),
            ),
        )
        assertThat(downgrade.isValid).isTrue()
        assertThat(store.getEntity(id)?.schemaVersion).isEqualTo("1.0.0")

        val renameOnV1 = store.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(
                        id = id,
                        type = "Person",
                        schemaVersion = "1.0.0",
                        payload = mutableMapOf("name" to "Bob"),
                    ),
                ),
            ),
        )
        assertThat(renameOnV1.isValid).isTrue()
        assertThat(store.getEntity(id)?.payload?.get("name")).isEqualTo("Bob")
    }

    @Test
    fun shouldPagePoolSelectAndCountByType() {
        repeat(5) { i ->
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "p$i"),
                        ),
                    ),
                ),
            )
        }
        val page1 = store.selectFromPool(BoMObjExprMatcher("type == 'Person'"), BoMPageRequest.of(1, 2))
        assertThat(page1.total).isEqualTo(5)
        assertThat(page1.items).hasSize(2)
        assertThat(page1.page).isEqualTo(1)
        val page3 = store.selectFromPool(BoMObjExprMatcher("type == 'Person'"), BoMPageRequest.of(3, 2))
        assertThat(page3.items).hasSize(1)
        val over = store.selectFromPool(BoMObjExprMatcher("type == 'Person'"), BoMPageRequest.of(9, 20))
        assertThat(over.items).isEmpty()
        assertThat(store.countByType()["Person"]).isEqualTo(5L)
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(page1.items[0].id!!)))
        assertThat(store.countByType(graph.id)["Person"]).isEqualTo(1L)
    }

    @Test
    fun shouldFindEntitiesByIdentityAndDuplicateGroups() {
        schemas.register(
            BoMSchema(
                "Agent",
                "1",
                BoMSchemaDsl.obj(
                    "Agent",
                    "Agent payload",
                    listOf(
                        BoMSchemaDsl.field(
                            "name",
                            BoMSchemaDsl.string("Name", "Agent name"),
                            identifier = true,
                        ),
                    ),
                ),
            ),
        )
        val a1 = UUID.randomUUID()
        val a2 = UUID.randomUUID()
        val b1 = UUID.randomUUID()
        val b2 = UUID.randomUUID()
        val unique = UUID.randomUUID()
        val blank = UUID.randomUUID()
        assertThat(
            store.write(
                BoMGraph(
                    entities = mutableListOf(
                        BoMEntity(id = a1, type = "Agent", schemaVersion = "1", payload = mutableMapOf("name" to "dup")),
                        BoMEntity(id = a2, type = "Agent", schemaVersion = "1", payload = mutableMapOf("name" to "dup")),
                        BoMEntity(id = b1, type = "Agent", schemaVersion = "1", payload = mutableMapOf("name" to "pair")),
                        BoMEntity(id = b2, type = "Agent", schemaVersion = "1", payload = mutableMapOf("name" to "pair")),
                        BoMEntity(id = unique, type = "Agent", schemaVersion = "1", payload = mutableMapOf("name" to "solo")),
                        BoMEntity(id = blank, type = "Agent", schemaVersion = "1", payload = mutableMapOf("name" to "  ")),
                    ),
                ),
            ).isValid,
        ).isTrue()

        assertThat(store.findEntitiesByIdentity("Agent", emptyMap())).isEmpty()
        assertThat(store.findEntitiesByIdentity("Agent", mapOf("name" to "  "))).isEmpty()
        assertThat(store.findEntitiesByIdentity("Agent", mapOf("name" to "missing"))).isEmpty()
        assertThat(store.findEntitiesByIdentity("Agent", mapOf("name" to "solo")).map { it.id })
            .containsExactly(unique)
        val groups = store.findDuplicateGroups("Agent")
        assertThat(groups).hasSize(2)
        assertThat(groups.map { it.identity["name"] }).containsExactlyInAnyOrder("dup", "pair")
        assertThat(groups.single { it.identity["name"] == "dup" }.entities.map { it.id })
            .containsExactlyInAnyOrder(a1, a2)
        assertThat(groups.single { it.identity["name"] == "pair" }.entities.map { it.id })
            .containsExactlyInAnyOrder(b1, b2)
    }
}
