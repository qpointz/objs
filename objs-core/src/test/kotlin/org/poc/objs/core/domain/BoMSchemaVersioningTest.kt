package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BoMSchemaVersioningTest {
    @Test
    fun shouldIncrementSimpleMajor() {
        assertThat(BoMSchemaVersioning.nextMajor(listOf("1", "4"))).isEqualTo("5")
    }

    @Test
    fun shouldIncrementDottedMajorAsSemver() {
        assertThat(BoMSchemaVersioning.nextMajor(listOf("1.0.0", "4.2.1"))).isEqualTo("5.0.0")
    }

    @Test
    fun shouldDefaultWhenEmpty() {
        assertThat(BoMSchemaVersioning.nextMajor(emptyList())).isEqualTo("1.0.0")
    }

    @Test
    fun shouldRejectNonNumericVersions() {
        assertThatThrownBy { BoMSchemaVersioning.nextMajor(listOf("alpha")) }
            .isInstanceOf(BoMSchemaDefinitionException::class.java)
    }
}
