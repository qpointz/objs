package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.validation.BoMValidationException
import java.util.UUID

class BoMAnnoExprMatcherTest {

    @Test
    fun shouldEvaluateDirectAnnotationVariables() {
        val matcher = BoMAnnoExprMatcher("version == '1.0.0' && app == 'aapp-lala'")
        assertThat(matcher).isInstanceOf(BoMNonPushableMatcher::class.java)
        assertThat(
            matcher.matches(
                candidate("version" to "1.0.0", "app" to "aapp-lala"),
            ),
        ).isTrue()
        assertThat(
            matcher.matches(
                candidate("version" to "2.0.0", "app" to "aapp-lala"),
            ),
        ).isFalse()
    }

    @Test
    fun shouldTreatMissingAnnotationAsNull() {
        val matcher = BoMAnnoExprMatcher("team != null")
        assertThat(matcher.matches(candidate("app" to "x"))).isFalse()
        assertThat(matcher.matches(candidate("team" to "core"))).isTrue()
    }

    @Test
    fun shouldRejectUnsafeConstructsAndInvalidSyntax() {
        assertThatThrownBy { BoMAnnoExprMatcher("new('java.lang.String')") }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { BoMAnnoExprMatcher("app.substring(0)") }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { BoMAnnoExprMatcher("app ==") }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { BoMAnnoExprMatcher("x".repeat(BoMAnnoExprEngine.MAX_EXPRESSION_LENGTH + 1)) }
            .isInstanceOf(BoMValidationException::class.java)
    }

    private fun candidate(vararg annotations: Pair<String, String>): BoMEntityMatchCandidate =
        BoMEntityDomainCandidate(
            BoMEntity(
                id = UUID.randomUUID(),
                type = "Thing",
                schemaVersion = "1",
                annotations = annotations.toMap().toMutableMap(),
            ),
        )
}
