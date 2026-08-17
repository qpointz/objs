package org.poc.objs.sbom.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SemVerVersionComparerTest {
    private val comparer = SemVerVersionComparer()

    @Test
    fun shouldOrderReleaseAheadOfPrerelease() {
        assertThat(comparer.compare("1.0.0-rc.1", "1.0.0")).isNegative()
        assertThat(comparer.compare("1.0.0", "1.0.1")).isNegative()
        assertThat(comparer.compare("2.0.0", "1.9.9")).isPositive()
    }

    @Test
    fun shouldEncodeSerialMatchingCompare() {
        val left = comparer.toSerial("1.0.0-rc.1")
        val right = comparer.toSerial("1.0.0")
        assertThat(left).isLessThan(right)
        assertThat(comparer.toSerial("1.2.3")).isGreaterThan(comparer.toSerial("1.2.2"))
    }

    @Test
    fun shouldSortInvalidSerialLast() {
        assertThat(comparer.toSerial("not-a-version")).isEqualByComparingTo(BigDecimal("-1"))
        assertThat(comparer.toSerial("2.0")).isEqualByComparingTo(SemVerVersionComparer.INVALID_SERIAL)
        assertThat(comparer.compare("not-a-version", "1.0.0")).isNegative()
    }
}
