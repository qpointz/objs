package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.*
import java.util.UUID

class BoDomainTypesTest {
    @Test
    fun shouldBuildEntityAndEdgeInMemory() {
        val id = UUID.randomUUID()
        val entity = Entity(
            id = id,
            type = "Person",
            schemaVersion = "1",
            payload = mutableMapOf("name" to "Ada"),
            annotations = mutableMapOf("item" to "X"),
        )
        val edge = Edge(
            source = id,
            target = UUID.randomUUID(),
            role = "knows",
        )
        val graph = Graph(mutableListOf(entity), mutableListOf(edge))
        assertThat(graph.entityById(id)).isEqualTo(entity)
        assertThat(edge.role).isEqualTo("knows")
    }
}
