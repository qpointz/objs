package org.poc.objs.api.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class UuidV5Test {
    @Test
    fun shouldProduceStableVersion5Uuids() {
        val a = UuidV5.entityId("demo", "product")
        val b = UuidV5.entityId("demo", "product")
        val c = UuidV5.entityId("demo", "other")
        assertThat(a).isEqualTo(b)
        assertThat(a).isNotEqualTo(c)
        assertThat(a.version()).isEqualTo(5)
        assertThat(a).isNotEqualTo(UUID.nameUUIDFromBytes("demo/entity/product".toByteArray()))
    }

    @Test
    fun shouldDifferentiateEntityAndEdgeNamespaces() {
        assertThat(UuidV5.entityId("demo", "x")).isNotEqualTo(UuidV5.edgeId("demo", "x"))
    }
}
