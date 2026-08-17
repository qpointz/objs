package org.poc.objs.sbom.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.BoMResolvedGraph
import java.util.UUID

class BomUnionTest {
    @Test
    fun shouldCollapseDuplicateMembershipAndEdges() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val entityA = BoMEntity(id = a, type = "Component", schemaVersion = "1.0.0")
        val entityB = BoMEntity(id = b, type = "Component", schemaVersion = "1.0.0")
        val edge = BoMEdge(id = UUID.randomUUID(), source = a, target = b, role = "depends_on")
        val left =
            BoMResolvedGraph(
                id = UUID.randomUUID(),
                annotations = emptyMap(),
                contents = BoMGraphContents(entities = listOf(entityA, entityB), edges = listOf(edge)),
            )
        val right =
            BoMResolvedGraph(
                id = UUID.randomUUID(),
                annotations = emptyMap(),
                contents =
                    BoMGraphContents(
                        entities = listOf(entityA),
                        edges = listOf(BoMEdge(id = UUID.randomUUID(), source = a, target = b, role = "depends_on")),
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
