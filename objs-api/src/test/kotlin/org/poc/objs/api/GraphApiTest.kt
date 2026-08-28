package org.poc.objs.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.GraphMutation
import org.poc.objs.api.domain.MutationMode
import org.poc.objs.api.domain.graphMutation
import org.poc.objs.api.typed.EntityTypeMeta
import org.poc.objs.api.typed.GraphBuilder
import org.poc.objs.api.typed.PayloadMapper
import org.poc.objs.api.typed.RelationDirection
import org.poc.objs.api.typed.TypedEntity
import org.poc.objs.api.typed.TypedGraphView
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class GraphApiTest {
    private val mapper = PayloadMapper(JsonMapper.builder().build())

    @Test
    fun shouldBuildMergeAndReplaceMutations() {
        val entity = Entity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Ada"))
        val merge = graphMutation {
            entities { set(entity) }
        }
        val replace = GraphMutation.of(Graph(mutableListOf(entity)), MutationMode.REPLACE)

        assertThat(merge.mode).isEqualTo(MutationMode.MERGE)
        assertThat(merge.entities.set).containsExactly(entity)
        assertThat(replace.mode).isEqualTo(MutationMode.REPLACE)
    }

    @Test
    fun shouldRejectDuplicateEntityAndEdgeUuids() {
        val entityId = UUID.randomUUID()
        val builder = GraphBuilder(mapper)
        builder.add(Entity(entityId, "Person", "1"))

        assertThatThrownBy {
            builder.add(Entity(entityId, "Person", "2"))
        }.isInstanceOf(IllegalStateException::class.java)

        val first = builder.ref(builder.add(Entity(type = "Person", schemaVersion = "1"), "second").key!!)
        val second = builder.ref(builder.add(Entity(type = "Person", schemaVersion = "1"), "third").key!!)
        val edgeId = UUID.randomUUID()
        val duplicate = Edge(edgeId, source = first.id, target = second.id, role = "knows")
        builder.add(duplicate)
        assertThatThrownBy { builder.add(duplicate.copy()) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun shouldNavigateSnapshotWithoutDroppingDanglingEdges() {
        val person = Entity(UUID.randomUUID(), "Person", "1")
        val other = Entity(UUID.randomUUID(), "Person", "1")
        val dangling = UUID.randomUUID()
        val edge = Edge(UUID.randomUUID(), source = person.id!!, target = dangling, role = "knows")
        val view = TypedGraphView.from(Graph(mutableListOf(person, other), mutableListOf(edge)))

        val node = view.node(person.id!!)!!
        val relation = node.edges("knows", RelationDirection.OUTBOUND)[0]

        assertThat(view.allEdges().size).isEqualTo(1)
        assertThat(relation.target).isNull()
        assertThat(relation.targetId).isEqualTo(dangling)
        assertThat(view.nodes("Person").size).isEqualTo(2)
    }

    @Test
    fun shouldThrowForAmbiguousSingularRelation() {
        val source = Entity(UUID.randomUUID(), "Person", "1")
        val targetA = Entity(UUID.randomUUID(), "Person", "1")
        val targetB = Entity(UUID.randomUUID(), "Person", "1")
        val edges = mutableListOf(
            Edge(UUID.randomUUID(), source = source.id!!, target = targetA.id!!, role = "manager"),
            Edge(UUID.randomUUID(), source = source.id!!, target = targetB.id!!, role = "manager"),
        )

        assertThatThrownBy {
            TypedGraphView.from(Graph(mutableListOf(source, targetA, targetB), edges))
                .node(source.id!!)!!
                .singular("manager")
        }.isInstanceOf(AmbiguousRelationException::class.java)
    }

    @Test
    fun shouldHydrateOnlyWhenExactBindingAndMapperAreSupplied() {
        val raw = Entity(
            UUID.randomUUID(),
            "Person",
            "1",
            mutableMapOf("name" to "Ada"),
        )
        val typed = TypedEntity(
            EntityTypeMeta("Person", "1"),
            LinkedHashMap::class.java,
            id = raw.id,
            payload = linkedMapOf("name" to "Ada"),
        )
        val roundTrip = typed.toEntity(mapper)

        val view = TypedGraphView.from(
            Graph(mutableListOf(roundTrip)),
            { type, version ->
                if (type == "Person" && version == "1") {
                    org.poc.objs.api.typed.TypedEntityBinding { entity, supplied ->
                        supplied.fromMap(entity.payload, LinkedHashMap::class.java)
                    }
                } else {
                    null
                }
            },
            mapper,
        )

        assertThat(view.node(raw.id!!)!!.hydratedPayload).isEqualTo(mapOf("name" to "Ada"))
    }
}
