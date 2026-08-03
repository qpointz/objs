package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BoMEdgeCardinalityTest {
    @Test
    fun shouldParseWireAndEnumNames() {
        assertThat(BoMEdgeCardinality.fromWire("UNSPECIFIED")).isEqualTo(BoMEdgeCardinality.UNSPECIFIED)
        assertThat(BoMEdgeCardinality.fromWire("1:1")).isEqualTo(BoMEdgeCardinality.ONE_TO_ONE)
        assertThat(BoMEdgeCardinality.fromWire("1:*")).isEqualTo(BoMEdgeCardinality.ONE_TO_MANY)
        assertThat(BoMEdgeCardinality.fromWire("ONE_TO_ONE")).isEqualTo(BoMEdgeCardinality.ONE_TO_ONE)
        assertThat(BoMEdgeCardinality.fromWire(" 1:* ")).isEqualTo(BoMEdgeCardinality.ONE_TO_MANY)
    }

    @Test
    fun shouldRejectUnknownWireValue() {
        assertThatThrownBy { BoMEdgeCardinality.fromWire("1:0..1") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown cardinality")
    }

    @Test
    fun shouldExposeSingularAndManyHelpers() {
        assertThat(BoMEdgeCardinality.UNSPECIFIED.isSingular).isFalse()
        assertThat(BoMEdgeCardinality.UNSPECIFIED.isMany).isFalse()
        assertThat(BoMEdgeCardinality.ONE_TO_ONE.isSingular).isTrue()
        assertThat(BoMEdgeCardinality.ONE_TO_ONE.isMany).isFalse()
        assertThat(BoMEdgeCardinality.ONE_TO_MANY.isSingular).isFalse()
        assertThat(BoMEdgeCardinality.ONE_TO_MANY.isMany).isTrue()
    }

    @Test
    fun shouldDefaultCardinalityOnAllowedEdgeRule() {
        val rule = BoMAllowedEdgeRule("A", "r", "B")
        assertThat(rule.cardinality).isEqualTo(BoMEdgeCardinality.UNSPECIFIED)
    }
}
