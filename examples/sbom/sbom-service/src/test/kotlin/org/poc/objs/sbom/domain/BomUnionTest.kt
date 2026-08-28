package org.poc.objs.sbom.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.core.domain.ResolvedGraph
import java.util.UUID

class UnionTest {
    @Test
    fun shouldCollapseDuplicateMembershipAndEdges() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val entityA = Entity(id = a, type = "Component", schemaVersion = "1.0.0")
        val entityB = Entity(id = b, type = "Component", schemaVersion = "1.0.0")
        val edge = Edge(id = UUID.randomUUID(), source = a, target = b, role = "depends_on")
        val left =
            ResolvedGraph(
                id = UUID.randomUUID(),
                annotations = emptyMap(),
                contents = GraphContents(entities = listOf(entityA, entityB), edges = listOf(edge)),
            )
        val right =
            ResolvedGraph(
                id = UUID.randomUUID(),
                annotations = emptyMap(),
                contents =
                    GraphContents(
                        entities = listOf(entityA),
                        edges = listOf(Edge(id = UUID.randomUUID(), source = a, target = b, role = "depends_on")),
                    ),
            )
        val union = BomUnion.of(listOf(left, right))
        assertThat(union.entities.map { it.id }).containsExactly(a, b)
        assertThat(union.edges).hasSize(1)
    }

    @Test
    fun shouldSanitizeAndCombineTagsCaseSensitively() {
        val tags = BomUnion.sanitizeTags(listOf(" Alpha ", "", "Alpha", "beta"))
        assertThat(tags.toList()).containsExactly("Alpha", "beta")
        assertThat(
            BomUnion.combinedTags(
                arrayOf("app"),
                arrayOf("ver"),
                listOf(arrayOf("bom"), arrayOf("app")),
            ),
        ).containsExactly("app", "ver", "bom")
    }
}
