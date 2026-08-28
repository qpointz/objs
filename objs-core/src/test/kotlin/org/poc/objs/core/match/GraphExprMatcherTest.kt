package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class GraphExprMatcherTest {
    @Test
    fun shouldLowerEqualityAndTree() {
        val id = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val matcher = GraphExprMatcher(
            "a.app == 'payments-api' && a.appVersion == '2.3.1' && id == '$id'",
        )
        assertThat(matcher.localEvalOnly).isFalse()
        val plan = matcher.pushdown!!
        assertThat(plan.idEquals).isEqualTo(id)
        assertThat(plan.annotationEquals).isEqualTo(
            mapOf("app" to "payments-api", "appVersion" to "2.3.1"),
        )
    }

    @Test
    fun shouldLowerAnnotationOnly() {
        val matcher = GraphExprMatcher("a.env == 'prod'")
        assertThat(matcher.pushdown!!.annotationEquals).isEqualTo(mapOf("env" to "prod"))
        assertThat(matcher.pushdown!!.idEquals).isNull()
    }

    @Test
    fun shouldLowerOrAndNotEquals() {
        val matcher = GraphExprMatcher("a.env == 'prod' || a.env == 'test' || a.env != 'dev'")
        assertThat(matcher.localEvalOnly).isFalse()
        val plan = matcher.pushdown!!
        assertThat(plan.dnf).hasSize(3)
        assertThat(plan.dnf.map { it.annotationEquals["env"] }).containsExactlyInAnyOrder("prod", "test", null)
        val ne = plan.dnf.first { it.annotationEquals.isEmpty() }
        assertThat(ne.annotationNotEquals).isEqualTo(mapOf("env" to "dev"))
    }

    @Test
    fun shouldMatchHeaderLocally() {
        val matcher = GraphExprMatcher("a.app == 'acme' && a.appVersion == '1.0.0'")
        assertThat(
            matcher.matchesHeader(
                UUID.randomUUID(),
                mapOf("app" to "acme", "appVersion" to "1.0.0"),
            ),
        ).isTrue()
        assertThat(
            matcher.matchesHeader(
                UUID.randomUUID(),
                mapOf("app" to "acme", "appVersion" to "2.0.0"),
            ),
        ).isFalse()
    }
}
