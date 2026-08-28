package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SchemaVersioningTest {
    @Test
    fun shouldIncrementSimpleMajor() {
        assertThat(SchemaVersioning.nextMajor(listOf("1", "4"))).isEqualTo("5")
    }

    @Test
    fun shouldIncrementDottedMajorAsSemver() {
        assertThat(SchemaVersioning.nextMajor(listOf("1.0.0", "4.2.1"))).isEqualTo("5.0.0")
    }

    @Test
    fun shouldDefaultWhenEmpty() {
        assertThat(SchemaVersioning.nextMajor(emptyList())).isEqualTo("1.0.0")
    }

    @Test
    fun shouldRejectNonNumericVersions() {
        assertThatThrownBy { SchemaVersioning.nextMajor(listOf("alpha")) }
            .isInstanceOf(SchemaDefinitionException::class.java)
    }
}
