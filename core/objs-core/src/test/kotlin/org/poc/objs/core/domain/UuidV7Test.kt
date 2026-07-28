package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UuidV7Test {
    @Test
    fun shouldGenerateVersion7Uuid() {
        val id = UuidV7.generate()
        assertThat(id.version()).isEqualTo(7)
        assertThat(id.variant()).isEqualTo(2)
    }

    @Test
    fun shouldBeTimeOrdered() {
        val a = UuidV7.generate(1_700_000_000_000L)
        val b = UuidV7.generate(1_700_000_000_100L)
        assertThat(a).isLessThan(b)
    }
}

class BoDomainTypesTest {
    @Test
    fun shouldBuildEntityAndEdgeInMemory() {
        val id = UuidV7.generate()
        val entity = BoEntity(
            id = id,
            type = "Person",
            version = "1",
            payload = mutableMapOf("name" to "Ada"),
            annotations = mutableMapOf("item" to "X"),
        )
        val edge = BoEdge(
            source = id,
            target = UuidV7.generate(),
            role = "knows",
        )
        val graph = BoGraph(mutableListOf(entity), mutableListOf(edge))
        assertThat(graph.entityById(id)).isEqualTo(entity)
        assertThat(edge.role).isEqualTo("knows")
    }
}
