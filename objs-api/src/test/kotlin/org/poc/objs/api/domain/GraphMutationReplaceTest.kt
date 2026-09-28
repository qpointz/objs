package org.poc.objs.api.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class GraphMutationReplaceTest {
    private fun entity(id: UUID? = UUID.randomUUID(), name: String = "x") =
        Entity(id = id, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to name))

    @Test
    fun shouldSwapEntityAndRedirectEdges_whenReplacementGiven() {
        val old = entity(name = "migrated")
        val other = entity()
        val existing = entity(name = "stored")
        val out = Edge(source = old.id!!, target = other.id!!, role = "knows")
        val incoming = Edge(source = other.id!!, target = old.id!!, role = "knows")
        val m = graphMutation {
            entities { set(old, other) }
            edges { set(out, incoming) }
        }

        val result = m.replace(old, existing)

        assertThat(result.entities.set).containsExactly(existing, other)
        assertThat(result.edges.set.map { it.source to it.target })
            .containsExactly(existing.id!! to other.id!!, other.id!! to existing.id!!)
        assertThat(m.entities.set).containsExactly(old, other)
        assertThat(out.source).isEqualTo(old.id)
    }

    @Test
    fun shouldDropReplacedEntity_whenReplacementAlreadyInMutation() {
        val old = entity()
        val existing = entity()
        val m = graphMutation { entities { set(existing, old) } }

        val result = m.replace(old, existing)

        assertThat(result.entities.set).containsExactly(existing)
    }

    @Test
    fun shouldMatchByReference_whenReplacedEntityHasNoId() {
        val old = entity(id = null)
        val existing = entity()
        val m = graphMutation { entities { set(old) } }

        val result = m.replace(old, existing)

        assertThat(result.entities.set).containsExactly(existing)
    }

    @Test
    fun shouldReplaceAllPayloadMatches_whenExternalIdEqual() {
        val a = entity().apply { payload["externalId"] = "EXT-1" }
        val b = entity().apply { payload["externalId"] = "EXT-1" }
        val unrelated = entity().apply { payload["externalId"] = "EXT-2" }
        val otherType = entity().apply { type = "Team"; payload["externalId"] = "EXT-1" }
        val existing = entity().apply { payload["externalId"] = "EXT-1" }
        val m = graphMutation {
            entities { set(a, b, unrelated, otherType) }
            edges { set(Edge(source = a.id!!, target = b.id!!, role = "knows")) }
        }

        val result = m.replaceByPayload(existing)

        assertThat(result.entities.set).containsExactly(existing, unrelated, otherType)
        assertThat(result.edges.set.single().let { it.source to it.target })
            .isEqualTo(existing.id!! to existing.id!!)
    }

    @Test
    fun shouldFail_whenReplacementHasNoId() {
        val old = entity()
        val m = graphMutation { entities { set(old) } }

        assertThatThrownBy { m.replace(old, entity(id = null)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
