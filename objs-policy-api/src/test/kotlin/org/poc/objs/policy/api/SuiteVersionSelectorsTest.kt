package org.poc.objs.policy.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.SuiteVersionSelectors.Parsed

class SuiteVersionSelectorsTest {

    @Test
    fun shouldParseBlankAsLatest() {
        assertThat(SuiteVersionSelectors.parse(null)).isEqualTo(Parsed.Latest)
        assertThat(SuiteVersionSelectors.parse("")).isEqualTo(Parsed.Latest)
        assertThat(SuiteVersionSelectors.parse("  ")).isEqualTo(Parsed.Latest)
    }

    @Test
    fun shouldParseExactAndMajorWildcard() {
        assertThat(SuiteVersionSelectors.parse("1.2.99"))
            .isEqualTo(Parsed.Exact("1.2", 99L))
        assertThat(SuiteVersionSelectors.parse("3.*"))
            .isEqualTo(Parsed.MajorWildcard(3))
    }

    @Test
    fun shouldRejectAmbiguousForms() {
        assertThatThrownBy { SuiteVersionSelectors.parse("1.2") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { SuiteVersionSelectors.parse("latest") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun shouldMatchPolicyAgainstSelectors() {
        val p = Policy(
            id = java.util.UUID.randomUUID(),
            key = "p",
            name = "p",
            serial = 99L,
            engineKind = "CUSTOM",
            body = "PASS",
            categoryId = java.util.UUID.randomUUID(),
            tags = listOf("t"),
            version = "1.2",
        )
        assertThat(SuiteVersionSelectors.matches(p, Parsed.Latest)).isTrue()
        assertThat(SuiteVersionSelectors.matches(p, Parsed.Exact("1.2", 99L))).isTrue()
        assertThat(SuiteVersionSelectors.matches(p, Parsed.Exact("1.2", 100L))).isFalse()
        assertThat(SuiteVersionSelectors.matches(p, Parsed.MajorWildcard(1))).isTrue()
        assertThat(SuiteVersionSelectors.matches(p, Parsed.MajorWildcard(2))).isFalse()
    }
}
