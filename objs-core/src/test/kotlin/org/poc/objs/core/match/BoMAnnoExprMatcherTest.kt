package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.validation.BoMValidationException
import java.util.UUID

class BoMAnnoExprMatcherTest {

    private fun backend(
        isPostgres: Boolean = true,
        onAny: (List<Map<String, String>>) -> BoMCandidateSource? = { disjuncts ->
            if (!isPostgres || disjuncts.isEmpty()) null else BoMCandidateSource { emptyList() }
        },
    ): BoMEntityCandidateBackend =
        object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = isPostgres
            override fun allEntitiesSource(): BoMCandidateSource = BoMCandidateSource { emptyList() }
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                onAny(disjuncts)
        }

    @Test
    fun shouldEvaluateDirectAnnotationVariables() {
        val matcher = BoMAnnoExprMatcher("version == '1.0.0' && app == 'aapp-lala'")
        assertThat(matcher).isInstanceOf(BoMMatcher::class.java)
        assertThat(matcher).isInstanceOf(BoMSourceCapableMatcher::class.java)
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
    fun shouldExposeContainmentSourceForLowerableExprOnPostgres() {
        val matcher = BoMAnnoExprMatcher("version == '1.0.0' && app == 'aapp-lala'")
        assertThat(matcher.localEvalOnly).isFalse()
        assertThat(matcher.sqlContainmentFilter)
            .containsEntry("version", "1.0.0")
            .containsEntry("app", "aapp-lala")
        var captured: List<Map<String, String>>? = null
        val postgres = backend { disjuncts ->
            captured = disjuncts
            BoMCandidateSource { emptyList() }
        }
        assertThat(matcher.toCandidateSource(postgres)).isNotNull()
        assertThat(captured).containsExactly(
            mapOf("version" to "1.0.0", "app" to "aapp-lala"),
        )

        assertThat(matcher.toCandidateSource(backend(isPostgres = false) { null })).isNull()
    }

    @Test
    fun shouldExposeOrOfContainmentForDisjunctiveExpr() {
        val matcher = BoMAnnoExprMatcher(
            "(app == 'app-00021' || app == 'app-00022') && appVersion == '1.0.0'",
        )
        assertThat(matcher.localEvalOnly).isFalse()
        assertThat(matcher.sqlContainmentDisjuncts).containsExactlyInAnyOrder(
            mapOf("app" to "app-00021", "appVersion" to "1.0.0"),
            mapOf("app" to "app-00022", "appVersion" to "1.0.0"),
        )
        var captured: List<Map<String, String>>? = null
        assertThat(matcher.toCandidateSource(backend { captured = it; BoMCandidateSource { emptyList() } }))
            .isNotNull()
        assertThat(captured).containsExactlyInAnyOrder(
            mapOf("app" to "app-00021", "appVersion" to "1.0.0"),
            mapOf("app" to "app-00022", "appVersion" to "1.0.0"),
        )
    }

    @Test
    fun shouldReturnNullSourceForNonLowerableExpr() {
        val matcher = BoMAnnoExprMatcher("team != null")
        assertThat(matcher.localEvalOnly).isTrue()
        assertThat(matcher.sqlContainmentDisjuncts).isNull()
        assertThat(matcher.toCandidateSource(backend())).isNull()
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
