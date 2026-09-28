package org.poc.objs.api.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class GraphMutationRewriterTest {
    private fun entity(ext: String? = null) = Entity(
        id = UUID.randomUUID(),
        type = "Person",
        schemaVersion = "1",
        payload = mutableMapOf<String, Any?>().apply { ext?.let { put("externalId", it) } },
    )

    private fun edge(from: Entity, to: Entity, role: String = "knows") =
        Edge(source = from.id!!, target = to.id!!, role = role)

    private fun rewriter(
        prepare: (GraphMutation) -> Unit = {},
        edge: (Edge) -> Edge? = { it },
        decide: (Entity) -> EntityDecision,
    ) = object : MutationRewriter<Unit> {
        override fun prepare(mutation: GraphMutation) = prepare(mutation)
        override fun entity(entity: Entity, ctx: Unit) = decide(entity)
        override fun edge(edge: Edge, ctx: Unit) = edge(edge)
    }

    @Test
    fun shouldResolveFromBatchContext_whenPrepareLooksUpExternalIds() {
        val a = entity("EXT-1")
        val b = entity("EXT-2")
        val stored = entity("EXT-1")
        val lookups = mutableListOf<Collection<String>>()
        val resolver = object : MutationRewriter<Map<String, Entity>> {
            override fun prepare(mutation: GraphMutation): Map<String, Entity> {
                val ids = mutation.entities.set.mapNotNull { it.payload["externalId"] as? String }
                lookups += ids
                return mapOf("EXT-1" to stored).filterKeys { it in ids }
            }

            override fun entity(entity: Entity, ctx: Map<String, Entity>) =
                ctx[entity.payload["externalId"]]?.let { EntityDecision.ReplaceWith(it) } ?: EntityDecision.Keep
        }
        val m = graphMutation {
            entities { set(a, b) }
            edges { set(edge(a, b)) }
        }

        val result = m.rewrite(resolver)

        assertThat(lookups).containsExactly(listOf("EXT-1", "EXT-2"))
        assertThat(result.entities.set).containsExactly(stored, b)
        assertThat(result.edges.set.single().source).isEqualTo(stored.id)
        assertThat(m.entities.set).containsExactly(a, b)
        assertThat(m.edges.set.single().source).isEqualTo(a.id)
    }

    @Test
    fun shouldRedirectEdges_whenDroppedWithRedirect() {
        val dup = entity()
        val keep = entity()
        val other = entity()
        val m = graphMutation {
            entities { set(keep, dup, other) }
            edges { set(edge(dup, other)) }
        }

        val result = m.rewrite(rewriter { if (it === dup) EntityDecision.Drop(keep.id) else EntityDecision.Keep })

        assertThat(result.entities.set).containsExactly(keep, other)
        assertThat(result.edges.set.single().let { it.source to it.target }).isEqualTo(keep.id!! to other.id!!)
    }

    @Test
    fun shouldDropIncidentEdges_whenDroppedWithoutRedirect() {
        val gone = entity()
        val a = entity()
        val b = entity()
        val m = graphMutation {
            entities { set(gone, a, b) }
            edges { set(edge(a, gone), edge(gone, b), edge(a, b)) }
        }

        val result = m.rewrite(rewriter { if (it === gone) EntityDecision.Drop() else EntityDecision.Keep })

        assertThat(result.entities.set).containsExactly(a, b)
        assertThat(result.edges.set.map { it.source to it.target }).containsExactly(a.id!! to b.id!!)
    }

    @Test
    fun shouldFollowRemapTransitively_whenRedirectTargetIsReplaced() {
        val x = entity()
        val y = entity()
        val stored = entity()
        val other = entity()
        val m = graphMutation {
            entities { set(x, y, other) }
            edges { set(edge(x, other)) }
        }

        val result = m.rewrite(
            rewriter {
                when (it) {
                    x -> EntityDecision.Drop(y.id)
                    y -> EntityDecision.ReplaceWith(stored)
                    else -> EntityDecision.Keep
                }
            },
        )

        assertThat(result.entities.set).containsExactly(stored, other)
        assertThat(result.edges.set.single().source).isEqualTo(stored.id)
    }

    @Test
    fun shouldDedupeEdgesAndLetHookDropSelfLoops_whenEntitiesMerged() {
        val a = entity()
        val b = entity()
        val c = entity()
        val stored = entity()
        val m = graphMutation {
            entities { set(a, b, c) }
            edges { set(edge(a, c), edge(b, c), edge(a, b)) }
        }

        val result = m.rewrite(
            rewriter(edge = { e -> e.takeIf { it.source != it.target } }) {
                if (it === a || it === b) EntityDecision.ReplaceWith(stored) else EntityDecision.Keep
            },
        )

        assertThat(result.entities.set).containsExactly(stored, c)
        assertThat(result.edges.set.map { it.source to it.target }).containsExactly(stored.id!! to c.id!!)
    }

    @Test
    fun shouldKeepUnsetsUnchanged_whenRewriting() {
        val a = entity()
        val unsetId = UUID.randomUUID()
        val m = graphMutation {
            entities {
                set(a)
                unset(unsetId)
            }
        }

        val result = m.rewrite(rewriter { EntityDecision.ReplaceWith(entity()) })

        assertThat(result.entities.unset).containsExactly(unsetId)
        assertThat(result.entities.unset).isNotSameAs(m.entities.unset)
    }

    @Test
    fun shouldFail_whenRemapIsCyclic() {
        val a = entity()
        val b = entity()
        val m = graphMutation {
            entities { set(a, b) }
            edges { set(edge(a, b)) }
        }

        assertThatThrownBy {
            m.rewrite(rewriter { if (it === a) EntityDecision.Drop(b.id) else EntityDecision.Drop(a.id) })
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
