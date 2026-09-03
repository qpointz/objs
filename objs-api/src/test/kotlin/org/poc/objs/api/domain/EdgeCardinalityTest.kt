package org.poc.objs.api.domain

import org.poc.objs.api.domain.*

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EdgeCardinalityTest {
    @Test
    fun shouldParseWireAndEnumNames() {
        assertThat(EdgeCardinality.fromWire("UNSPECIFIED")).isEqualTo(EdgeCardinality.UNSPECIFIED)
        assertThat(EdgeCardinality.fromWire("1:1")).isEqualTo(EdgeCardinality.ONE_TO_ONE)
        assertThat(EdgeCardinality.fromWire("1:*")).isEqualTo(EdgeCardinality.ONE_TO_MANY)
        assertThat(EdgeCardinality.fromWire("ONE_TO_ONE")).isEqualTo(EdgeCardinality.ONE_TO_ONE)
        assertThat(EdgeCardinality.fromWire(" 1:* ")).isEqualTo(EdgeCardinality.ONE_TO_MANY)
    }

    @Test
    fun shouldRejectUnknownWireValue() {
        assertThatThrownBy { EdgeCardinality.fromWire("1:0..1") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown cardinality")
    }

    @Test
    fun shouldExposeSingularAndManyHelpers() {
        assertThat(EdgeCardinality.UNSPECIFIED.isSingular).isFalse()
        assertThat(EdgeCardinality.UNSPECIFIED.isMany).isFalse()
        assertThat(EdgeCardinality.ONE_TO_ONE.isSingular).isTrue()
        assertThat(EdgeCardinality.ONE_TO_ONE.isMany).isFalse()
        assertThat(EdgeCardinality.ONE_TO_MANY.isSingular).isFalse()
        assertThat(EdgeCardinality.ONE_TO_MANY.isMany).isTrue()
    }

    @Test
    fun shouldDefaultCardinalityOnAllowedEdgeRule() {
        val rule = AllowedEdgeRule("A", "r", "B")
        assertThat(rule.cardinality).isEqualTo(EdgeCardinality.UNSPECIFIED)
    }
}
