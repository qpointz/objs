package org.poc.objs.core.persistence

import java.time.Instant
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
import org.poc.objs.core.domain.BoMGraphDelete
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMGraphUpsert
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.BoMGraphException
import org.poc.objs.core.domain.FirstSeenGraphMergePolicy
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.match.BoMGraphExprMatcher
import org.poc.objs.core.match.BoMObjExprMatcher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class, BoMNamedGraphStore::class, BoMPoolEntityReader::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-named-graph-store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=false",
    ],
)
class BoMNamedGraphStoreTest {

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

    @Autowired
    lateinit var entityRepository: BoMEntityRepository

    @Autowired
    lateinit var membershipRepository: BoMGraphMembershipRepository

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var poolReader: BoMPoolEntityReader

    private lateinit var a: UUID
    private lateinit var b: UUID

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
                ),
            ).isValid,
        ).isTrue()
    }

    /** Edges require an existing graph (`graph_id` NOT NULL); write the edge after the header exists. */
    private fun addEdge(graphId: UUID, source: UUID, target: UUID): UUID {
        val edgeId = UUID.randomUUID()
        assertThat(
            graphStore.write(
                BoMGraph(
                    edges = mutableListOf(
                        BoMEdge(id = edgeId, graphId = graphId, source = source, target = target, role = "knows"),
                    ),
                ),
            ).isValid,
        ).isTrue()
        return edgeId
    }

    @Test
    fun shouldRoundTripCreateAndGet_preservingIds() {
        val created = namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("pack" to "p1"), entityIds = setOf(a, b)),
        )
        val edgeId = addEdge(created.id, a, b)

        val got = namedGraphs.get(created.id)!!
        assertThat(got.annotations).containsEntry("pack", "p1")
        assertThat(got.contents.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(got.contents.edges.map { it.id }).containsExactly(edgeId)
        assertThat(got.contents.entities.find { it.id == a }!!.payload["name"]).isEqualTo("A")
    }

    @Test
    fun shouldResolveLatestPayload_withoutRewritingMembership() {
        val created = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(created.id, a, b)

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

        val got = namedGraphs.get(created.id)!!
        assertThat(got.contents.entities.find { it.id == a }!!.payload["name"]).isEqualTo("A-updated")
        assertThat(got.contents.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
    }

    @Test
    fun shouldRejectEdgeWithoutEndpointMembers() {
        val holder = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        val edgeId = addEdge(holder.id, a, b)

        assertThatThrownBy {
            namedGraphs.create(
                BoMGraphSpec(entityIds = setOf(a), edgeIds = setOf(edgeId)),
            )
        }.isInstanceOf(BoMGraphException::class.java)
            .extracting("code")
            .isEqualTo("GRAPH_EDGE_ENDPOINTS")
    }

    @Test
    fun shouldRejectMissingEntityId() {
        val missing = UUID.randomUUID()
        assertThatThrownBy {
            namedGraphs.create(BoMGraphSpec(entityIds = setOf(missing)))
        }.isInstanceOf(BoMGraphException::class.java)
            .extracting("code")
            .isEqualTo("GRAPH_ENTITY_MISSING")
    }

    @Test
    fun shouldDeleteGraph_leavingGraphObjects() {
        val created = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(created.id, a, b)

        namedGraphs.delete(created.id)
        assertThat(namedGraphs.get(created.id)).isNull()
        assertThat(entityRepository.existsById(a)).isTrue()
        assertThat(entityRepository.existsById(b)).isTrue()
    }

    @Test
    fun shouldSnapshot_cloningMembersAndStampingAnnotations() {
        val source = namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("live" to "true"), entityIds = setOf(a, b)),
        )
        val edgeId = addEdge(source.id, a, b)

        val stamp = mapOf("decisionId" to "D-9", "t" to "snap")
        val hard = namedGraphs.clone(source.id, stamp)

        assertThat(hard.id).isNotEqualTo(source.id)
        assertThat(hard.annotations).isEqualTo(stamp)
        assertThat(hard.contents.entities).hasSize(2)
        assertThat(hard.contents.edges).hasSize(1)
        hard.contents.entities.forEach { entity ->
            assertThat(entity.id).isNotIn(a, b)
            assertThat(entity.annotations).containsEntry("decisionId", "D-9")
        }
        val cloneEdge = hard.contents.edges.single()
        assertThat(cloneEdge.id).isNotEqualTo(edgeId)
        assertThat(cloneEdge.source).isIn(hard.contents.entities.map { it.id })
        assertThat(cloneEdge.target).isIn(hard.contents.entities.map { it.id })

        val stillLive = namedGraphs.get(source.id)!!
        assertThat(stillLive.contents.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(stillLive.annotations).containsEntry("live", "true")
    }

    @Test
    fun shouldCloneAsAliasForSnapshot() {
        val source = namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("live" to "true"), entityIds = setOf(a, b)),
        )
        addEdge(source.id, a, b)

        val cloned = namedGraphs.clone(source.id, mapOf("decisionId" to "D-9"))

        assertThat(cloned.id).isNotEqualTo(source.id)
        assertThat(cloned.annotations).isEqualTo(mapOf("decisionId" to "D-9"))
        assertThat(cloned.contents.entities).hasSize(2)
        assertThat(cloned.contents.edges).hasSize(1)
        assertThat(namedGraphs.get(source.id)!!.contents.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
    }

    @Test
    fun shouldCopyGraph_sharingPoolIdsAndNewEdgeIds() {
        val source = namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("live" to "true"), entityIds = setOf(a, b)),
        )
        val edgeId = addEdge(source.id, a, b)
        val poolBefore = entityRepository.count()

        val copied = namedGraphs.copyGraph(source.id, mapOf("kind" to "copy"))

        assertThat(copied.id).isNotEqualTo(source.id)
        assertThat(copied.annotations).isEqualTo(mapOf("kind" to "copy"))
        assertThat(copied.contents.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(copied.contents.edges).hasSize(1)
        assertThat(copied.contents.edges.single().id).isNotEqualTo(edgeId)
        assertThat(copied.contents.edges.single().source).isEqualTo(a)
        assertThat(copied.contents.edges.single().target).isEqualTo(b)
        assertThat(entityRepository.count()).isEqualTo(poolBefore)
        val stillLive = namedGraphs.get(source.id)!!
        assertThat(stillLive.contents.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(stillLive.contents.edges.single().id).isEqualTo(edgeId)
    }

    @Test
    fun shouldFailCopyGraph_whenSourceMissing() {
        val missing = UUID.randomUUID()
        assertThatThrownBy { namedGraphs.copyGraph(missing) }
            .isInstanceOf(BoMGraphException::class.java)
            .extracting("code")
            .isEqualTo("GRAPH_NOT_FOUND")
    }

    @Test
    fun shouldMergeGraphs_firstSeenOnOverlappingMembersAndEdges() {
        val left = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(left.id, a, b)
        val right = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))
        addEdge(right.id, a, b)
        val poolBefore = entityRepository.count()

        val merged = namedGraphs.mergeGraph(listOf(left.id, right.id), mapOf("kind" to "union"))

        assertThat(merged.annotations).containsEntry("kind", "union")
        assertThat(merged.contents.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
        assertThat(merged.contents.edges).hasSize(1)
        assertThat(merged.contents.edges.single().source).isEqualTo(a)
        assertThat(merged.contents.edges.single().target).isEqualTo(b)
        assertThat(entityRepository.count()).isEqualTo(poolBefore)
    }

    @Test
    fun shouldMergeGraphs_withCustomPolicyPreferringIncomingEdge() {
        val left = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(left.id, a, b)
        val right = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(right.id, a, b)
        val incomingWins = object : FirstSeenGraphMergePolicy() {
            override fun onDuplicateEdge(kept: BoMEdge, incoming: BoMEdge): BoMEdge = incoming
        }

        val merged = namedGraphs.mergeGraph(listOf(left.id, right.id), emptyMap(), incomingWins)

        assertThat(merged.contents.edges).hasSize(1)
        assertThat(merged.contents.entities.map { it.id }.toSet()).isEqualTo(setOf(a, b))
    }

    @Test
    fun shouldFailMergeGraph_whenSourceListEmpty() {
        assertThatThrownBy { namedGraphs.mergeGraph(emptyList()) }
            .isInstanceOf(BoMGraphException::class.java)
            .extracting("code")
            .isEqualTo("GRAPH_MERGE_EMPTY")
    }

    @Test
    fun shouldFailMergeGraph_whenAnySourceMissing() {
        val left = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))
        val missing = UUID.randomUUID()
        val graphsBefore = namedGraphs.list().size
        assertThatThrownBy { namedGraphs.mergeGraph(listOf(left.id, missing)) }
            .isInstanceOf(BoMGraphException::class.java)
            .extracting("code")
            .isEqualTo("GRAPH_NOT_FOUND")
        assertThat(namedGraphs.list()).hasSize(graphsBefore)
    }

    @Test
    fun shouldMatchCopyMembership_whenMergingOneSource() {
        val source = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(source.id, a, b)

        val copied = namedGraphs.copyGraph(source.id)
        val merged = namedGraphs.mergeGraph(listOf(source.id))

        assertThat(merged.contents.entities.map { it.id }.toSet())
            .isEqualTo(copied.contents.entities.map { it.id }.toSet())
        assertThat(merged.contents.edges.map { Triple(it.source, it.role, it.target) }.toSet())
            .isEqualTo(copied.contents.edges.map { Triple(it.source, it.role, it.target) }.toSet())
    }

    /** G-G2: the same entity may sit in 0..n graphs — membership is a plain M2M row per graph. */
    @Test
    fun shouldAllowSameEntityInTwoGraphs_asTwoMembershipRows() {
        val g1 = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))
        val g2 = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))

        assertThat(membershipRepository.findByGraphId(g1.id).map { it.entityId }).containsExactly(a)
        assertThat(membershipRepository.findByGraphId(g2.id).map { it.entityId }).containsExactly(a)
        assertThat(namedGraphs.get(g1.id)!!.contents.entities.map { it.id }).containsExactly(a)
        assertThat(namedGraphs.get(g2.id)!!.contents.entities.map { it.id }).containsExactly(a)
    }

    // --- WI-004: graph-scoped mutate / attach / detach ---

    @Test
    fun shouldAttachAndDetachMember() {
        val graph = namedGraphs.create(BoMGraphSpec())

        namedGraphs.attach(graph.id, a)
        assertThat(namedGraphs.get(graph.id)!!.contents.entities.map { it.id }).containsExactly(a)

        namedGraphs.detach(graph.id, a)
        assertThat(namedGraphs.get(graph.id)!!.contents.entities).isEmpty()
        assertThat(entityRepository.existsById(a)).isTrue()
    }

    @Test
    fun shouldDetach_droppingIncidentGraphEdges() {
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        val edgeId = addEdge(graph.id, a, b)

        namedGraphs.detach(graph.id, a)

        val resolved = namedGraphs.get(graph.id)!!
        assertThat(resolved.contents.entities.map { it.id }).containsExactly(b)
        assertThat(resolved.contents.edges.map { it.id }).doesNotContain(edgeId)
        assertThat(entityRepository.existsById(a)).isTrue()
    }

    @Test
    fun shouldFailAttach_whenGraphMissing() {
        val missing = UUID.randomUUID()
        assertThatThrownBy { namedGraphs.attach(missing, a) }
            .isInstanceOf(BoMGraphException::class.java)
            .extracting("code")
            .isEqualTo("GRAPH_NOT_FOUND")
    }

    @Test
    fun shouldFailAttach_whenEntityMissing() {
        val graph = namedGraphs.create(BoMGraphSpec())
        val missing = UUID.randomUUID()
        assertThatThrownBy { namedGraphs.attach(graph.id, missing) }
            .isInstanceOf(BoMGraphException::class.java)
            .extracting("code")
            .isEqualTo("GRAPH_ENTITY_MISSING")
    }

    @Test
    fun shouldMutate_upsertingEntityAndAutoMembership() {
        val graph = namedGraphs.create(BoMGraphSpec())
        val neu = UUID.randomUUID()

        val result = namedGraphs.mutate(
            graph.id,
            BoMGraphMutation(
                upsert = BoMGraphUpsert(
                    entities = mutableListOf(
                        BoMEntity(id = neu, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "New")),
                    ),
                ),
            ),
        )

        assertThat(result.isValid).isTrue()
        val resolved = namedGraphs.get(graph.id)!!
        assertThat(resolved.contents.entities.map { it.id }).containsExactly(neu)
        assertThat(entityRepository.existsById(neu)).isTrue()
    }

    @Test
    fun shouldMutate_upsertingEdge_stampingGraphId() {
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))

        val result = namedGraphs.mutate(
            graph.id,
            BoMGraphMutation(
                upsert = BoMGraphUpsert(
                    edges = mutableListOf(BoMEdge(source = a, target = b, role = "knows")),
                ),
            ),
        )

        assertThat(result.isValid).isTrue()
        val resolved = namedGraphs.get(graph.id)!!
        assertThat(resolved.contents.edges).hasSize(1)
        assertThat(resolved.contents.edges.single().graphId).isEqualTo(graph.id)
    }

    @Test
    fun shouldRejectMutate_whenEdgeEndpointNotMember() {
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))

        val result = namedGraphs.mutate(
            graph.id,
            BoMGraphMutation(
                upsert = BoMGraphUpsert(
                    edges = mutableListOf(BoMEdge(source = a, target = b, role = "knows")),
                ),
            ),
        )

        assertThat(result.isValid).isFalse()
        assertThat(result.issues).anySatisfy { issue ->
            assertThat(issue.code).isEqualTo("EDGE_ENDPOINT_NOT_MEMBER")
        }
        assertThat(namedGraphs.get(graph.id)!!.contents.edges).isEmpty()
    }

    @Test
    fun shouldAllowMutate_edgeToEntityUpsertedInSameMutation() {
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))
        val neu = UUID.randomUUID()

        val result = namedGraphs.mutate(
            graph.id,
            BoMGraphMutation(
                upsert = BoMGraphUpsert(
                    entities = mutableListOf(
                        BoMEntity(id = neu, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "New")),
                    ),
                    edges = mutableListOf(BoMEdge(source = a, target = neu, role = "knows")),
                ),
            ),
        )

        assertThat(result.isValid).isTrue()
        assertThat(namedGraphs.get(graph.id)!!.contents.edges).hasSize(1)
    }

    @Test
    fun shouldMutate_deleteEntityDetachingMembershipOnly_keepingPoolEntity() {
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(graph.id, a, b)

        val result = namedGraphs.mutate(
            graph.id,
            BoMGraphMutation(delete = BoMGraphDelete(entities = mutableListOf(a))),
        )

        assertThat(result.isValid).isTrue()
        val resolved = namedGraphs.get(graph.id)!!
        assertThat(resolved.contents.entities.map { it.id }).containsExactly(b)
        assertThat(resolved.contents.edges).isEmpty()
        assertThat(entityRepository.existsById(a)).isTrue()
    }

    @Test
    fun shouldMutate_deleteEdgeBelongingToThisGraphOnly() {
        val g1 = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        val g2 = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        val edgeInG1 = addEdge(g1.id, a, b)
        val edgeInG2 = addEdge(g2.id, a, b)

        val result = namedGraphs.mutate(
            g1.id,
            BoMGraphMutation(delete = BoMGraphDelete(edges = mutableListOf(edgeInG2))),
        )

        assertThat(result.isValid).isTrue()
        assertThat(namedGraphs.get(g1.id)!!.contents.edges.map { it.id }).containsExactly(edgeInG1)
        assertThat(namedGraphs.get(g2.id)!!.contents.edges.map { it.id }).containsExactly(edgeInG2)
    }

    @Test
    fun shouldFailMutate_whenGraphMissing() {
        val missing = UUID.randomUUID()
        assertThatThrownBy {
            namedGraphs.mutate(missing, BoMGraphMutation())
        }.isInstanceOf(BoMGraphException::class.java)
            .extracting("code")
            .isEqualTo("GRAPH_NOT_FOUND")
    }

    @Test
    fun shouldSearch_returnEmpty_whenNoQAndNoExpr() {
        namedGraphs.create(BoMGraphSpec(annotations = mapOf("env" to "prod"), entityIds = setOf(a)))
        assertThat(namedGraphs.search(q = null, expr = null)).isEmpty()
        assertThat(namedGraphs.search(q = "  ", expr = "")).isEmpty()
    }

    @Test
    fun shouldSearch_byAnnotationSubstring_caseInsensitive() {
        val hit = namedGraphs.create(BoMGraphSpec(annotations = mapOf("env" to "Production"), entityIds = setOf(a)))
        namedGraphs.create(BoMGraphSpec(annotations = mapOf("env" to "test"), entityIds = setOf(b)))

        val items = namedGraphs.search(q = "prod")
        assertThat(items).hasSize(1)
        assertThat(items[0].id).isEqualTo(hit.id)
        assertThat(items[0].annotations).containsEntry("env", "Production")
    }

    @Test
    fun shouldSearch_byUuidPrefix() {
        val hit = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))
        namedGraphs.create(BoMGraphSpec(entityIds = setOf(b)))
        val prefix = hit.id.toString().substring(0, 8)

        val items = namedGraphs.search(q = prefix)
        assertThat(items.map { it.id }).contains(hit.id)
        assertThat(items).allMatch { it.id.toString().startsWith(prefix, ignoreCase = true) ||
            it.id.toString().contains(prefix, ignoreCase = true) ||
            it.annotations.any { (k, v) -> k.contains(prefix, ignoreCase = true) || v.contains(prefix, ignoreCase = true) } }
    }

    @Test
    fun shouldSearch_andQWithExpr() {
        val hit = namedGraphs.create(BoMGraphSpec(annotations = mapOf("env" to "prod", "app" to "acme"), entityIds = setOf(a)))
        namedGraphs.create(BoMGraphSpec(annotations = mapOf("env" to "prod", "app" to "other"), entityIds = setOf(b)))
        namedGraphs.create(BoMGraphSpec(annotations = mapOf("env" to "test", "app" to "acme"), entityIds = setOf(a)))

        val items = namedGraphs.search(q = "acme", expr = "a.env == 'prod'")
        assertThat(items).hasSize(1)
        assertThat(items[0].id).isEqualTo(hit.id)
    }

    @Test
    fun shouldMatchingHeaders_byAnnotationEquality() {
        val hit = namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("app" to "payments", "appVersion" to "1.0.0"), entityIds = setOf(a)),
        )
        namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("app" to "payments", "appVersion" to "2.0.0"), entityIds = setOf(b)),
        )
        namedGraphs.create(
            BoMGraphSpec(annotations = mapOf("app" to "billing", "appVersion" to "1.0.0"), entityIds = setOf(a)),
        )

        val headers = namedGraphs.matchingHeaders(
            BoMGraphExprMatcher("a.app == 'payments' && a.appVersion == '1.0.0'"),
        )
        assertThat(headers).hasSize(1)
        assertThat(headers[0].id).isEqualTo(hit.id)
    }

    @Test
    fun shouldSearch_respectLimitAndStableOrder() {
        repeat(5) { i ->
            namedGraphs.create(BoMGraphSpec(annotations = mapOf("tag" to "shared-$i"), entityIds = setOf(a)))
        }
        val limited = namedGraphs.search(q = "shared", limit = 2)
        assertThat(limited).hasSize(2)
        assertThat(limited.map { it.id }).isSorted
    }

    @Test
    fun shouldListGraphIdsAndIncidentEdges_forReverseLookup() {
        val g1 = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        val g2 = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))
        addEdge(g1.id, a, b)
        val orphan = UUID.randomUUID()
        graphStore.write(
            BoMGraph(
                entities = mutableListOf(
                    BoMEntity(id = orphan, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "O")),
                ),
            ),
        )

        assertThat(namedGraphs.listGraphIdsForEntity(a)).containsExactlyInAnyOrder(g1.id, g2.id)
        assertThat(namedGraphs.listGraphIdsForEntity(orphan)).isEmpty()
        assertThat(namedGraphs.listIncidentEdges(a)).hasSize(1)
        assertThat(namedGraphs.listIncidentEdges(a, g1.id).single().target).isEqualTo(b)
        assertThat(namedGraphs.listIncidentEdges(a, g2.id)).isEmpty()
        assertThat(namedGraphs.listEntityIdsInGraph(g1.id)).containsExactlyInAnyOrder(a, b)
    }

    @Test
    fun shouldListGraphIdsForEntity_includingPinAfterLiveDetach() {
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        namedGraphs.createDeepGraphVersion(graph.id, mapOf("label" to "snap"))
        namedGraphs.detach(graph.id, a)

        assertThat(membershipRepository.findByEntityId(a)).isEmpty()
        assertThat(namedGraphs.listGraphIdsForEntity(a)).containsExactly(graph.id)
        assertThat(namedGraphs.listGraphIdsForEntity(b)).containsExactly(graph.id)
    }

    @Test
    fun shouldSelectFromPool_withCompareAndPrefixPushdown() {
        assertThat(poolReader.isPostgres).isFalse()
        graphStore.write(
            BoMGraph(
                entities =
                    mutableListOf(
                        BoMEntity(
                            id = UUID.randomUUID(),
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "Amy"),
                        ),
                        BoMEntity(
                            id = UUID.randomUUID(),
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "Bob"),
                        ),
                    ),
            ),
        )
        val prefix =
            graphStore.selectFromPool(
                BoMObjExprMatcher("type == 'Person' && p.name =~ '^Bo'"),
            )
        assertThat(prefix.entities.map { it.payload["name"] }).containsExactly("Bob")

        val compare =
            graphStore.selectFromPool(
                BoMObjExprMatcher("type == 'Person' && p.name > 'B'"),
            )
        assertThat(compare.entities.map { it.payload["name"] }).containsExactly("Bob")
    }

    @Test
    fun shouldStampClocks_onInsertAndKeepCreatedAtOnUpdate() {
        val loaded = graphStore.getEntity(a)!!
        assertThat(loaded.createdAt).isNotNull
        assertThat(loaded.updatedAt).isNotNull
        val created = loaded.createdAt!!
        Thread.sleep(15)
        loaded.payload["name"] = "A2"
        loaded.createdAt = Instant.parse("2000-01-01T00:00:00Z")
        loaded.updatedAt = Instant.parse("2000-01-01T00:00:00Z")
        assertThat(graphStore.write(BoMGraph(entities = mutableListOf(loaded))).isValid).isTrue()
        val after = graphStore.getEntity(a)!!
        assertThat(after.createdAt).isEqualTo(created)
        assertThat(after.updatedAt).isAfter(created)
        assertThat(after.createdAt).isNotEqualTo(Instant.parse("2000-01-01T00:00:00Z"))
    }

    @Test
    fun shouldPreserveEntityClocks_whenCopyGraph() {
        val before = graphStore.getEntity(a)!!
        val source = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        Thread.sleep(15)
        val copied = namedGraphs.copyGraph(source.id, mapOf("k" to "v"))
        val after = graphStore.getEntity(a)!!
        assertThat(after.createdAt).isEqualTo(before.createdAt)
        assertThat(after.updatedAt).isEqualTo(before.updatedAt)
        assertThat(copied.createdAt).isNotNull
        assertThat(copied.updatedAt).isNotNull
        assertThat(copied.createdAt).isNotEqualTo(source.createdAt)
    }

    @Test
    fun shouldBumpGraphUpdatedAt_whenMembershipChanges() {
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a)))
        val created = graph.createdAt!!
        val updated = graph.updatedAt!!
        Thread.sleep(15)
        namedGraphs.attach(graph.id, b)
        val after = namedGraphs.get(graph.id)!!
        assertThat(after.createdAt).isEqualTo(created)
        assertThat(after.updatedAt).isAfter(updated)
    }

    @Test
    fun shouldNotWriteVersionRows_onOrdinaryPersist() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_entity_version", Int::class.java)).isZero()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_graph_version", Int::class.java)).isZero()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_graph_edge_version", Int::class.java)).isZero()
        val loaded = graphStore.getEntity(a)!!
        assertThat(entityRepository.findById(a).orElseThrow().headVersion).isNull()
        loaded.payload["name"] = "A3"
        assertThat(graphStore.write(BoMGraph(entities = mutableListOf(loaded))).isValid).isTrue()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_entity_version", Int::class.java)).isZero()
        assertThat(entityRepository.findById(a).orElseThrow().headVersion).isNull()

        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(graph.id, a, b)
        namedGraphs.clone(graph.id)
        namedGraphs.copyGraph(graph.id)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_entity_version", Int::class.java)).isZero()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_graph_version", Int::class.java)).isZero()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_graph_edge_version", Int::class.java)).isZero()
    }

    @Test
    fun shouldFreezeDeepGraphVersion_withoutChangingLiveHeadUntilEdit() {
        val graph = namedGraphs.create(BoMGraphSpec(entityIds = setOf(a, b)))
        addEdge(graph.id, a, b)
        val beforeCount = entityRepository.count()
        val freeze = namedGraphs.createDeepGraphVersion(graph.id, mapOf("label" to "v1"))
        assertThat(namedGraphs.listGraphVersions(graph.id).map { it.version }).containsExactly(freeze.version)
        assertThat(entityRepository.count()).isEqualTo(beforeCount)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_entity_version", Int::class.java)).isEqualTo(2)

        val pin = namedGraphs.getGraphVersion(graph.id, freeze.version)
        val liveEntity = graphStore.getEntity(a)!!
        liveEntity.payload["name"] = "A-live"
        assertThat(graphStore.write(BoMGraph(entities = mutableListOf(liveEntity))).isValid).isTrue()
        val pinAgain = namedGraphs.getGraphVersion(graph.id, freeze.version)
        assertThat(pinAgain.contents.entities.single { it.id == a }.payload["name"]).isEqualTo(pin.contents.entities.single { it.id == a }.payload["name"])
        assertThat(graphStore.getEntity(a)!!.payload["name"]).isEqualTo("A-live")
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_graph_version", Int::class.java)).isEqualTo(1)

        val cloned = namedGraphs.clone(graph.id)
        assertThat(cloned.contents.entities.map { it.payload["name"] }).contains("A-live")
        assertThat(namedGraphs.listGraphVersions(cloned.id)).isEmpty()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_graph_version", Int::class.java)).isEqualTo(1)

        namedGraphs.delete(graph.id)
        val afterDelete = namedGraphs.getGraphVersion(graph.id, freeze.version)
        assertThat(afterDelete.contents.entities).hasSize(2)
        assertThat(afterDelete.contents.edges).hasSize(1)
    }
}
