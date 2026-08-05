package org.poc.objs.core.match

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.poc.objs.core.domain.BoMEntity
import java.util.UUID

class BoMMatcherHierarchyTest {

    @Test
    fun shouldExposeSourceCapableMatchAllForPostgres() {
        val matcher = MatchAllAnnotationMatcher(mapOf("app" to "payments", "appVersion" to "1.0.0"))
        assertThat(matcher).isInstanceOf(BoMSourceCapableMatcher::class.java)
        assertThat(matcher.expression).isEqualTo(
            BoMMatchExpression.And(
                listOf(
                    BoMMatchExpression.AnnotationEquals("app", "payments"),
                    BoMMatchExpression.AnnotationEquals("appVersion", "1.0.0"),
                ),
            ),
        )
        val backend = object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): BoMCandidateSource = BoMCandidateSource { emptyList() }
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? {
                assertThat(disjuncts).hasSize(1)
                assertThat(disjuncts.single())
                    .containsEntry("app", "payments")
                    .containsEntry("appVersion", "1.0.0")
                return BoMCandidateSource { emptyList() }
            }
        }
        assertThat(matcher.toCandidateSource(backend)).isNotNull()
        val h2 = object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = false
            override fun allEntitiesSource(): BoMCandidateSource = BoMCandidateSource { emptyList() }
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                null
        }
        assertThat(matcher.toCandidateSource(h2)).isNull()
    }

    @Test
    fun shouldMatchCandidatesAndDomainEntitiesIdentically() {
        val matcher = MatchAllAnnotationMatcher(mapOf("env" to "test"))
        val entity = BoMEntity(
            id = UUID.randomUUID(),
            type = "Person",
            schemaVersion = "1",
            annotations = mutableMapOf("env" to "test", "team" to "core"),
        )
        val candidate = BoMEntityDomainCandidate(entity)
        assertThat(matcher.matches(entity)).isTrue()
        assertThat(matcher.matches(candidate)).isTrue()
        assertThat(matcher.expression.matches(candidate)).isTrue()
    }

    @Test
    fun shouldAdaptLegacyAnnotationMatchersAsFilterOnly() {
        val legacy = BoMAnnotationMatcher { it.annotations["env"] == "prod" }
        val adapted = legacy.asBoMMatcher()
        assertThat(adapted).isInstanceOf(BoMAnnotationMatcherAdapter::class.java)
        assertThat(adapted).isNotInstanceOf(BoMSourceCapableMatcher::class.java)

        val prod = BoMEntity(
            type = "Person",
            schemaVersion = "1",
            annotations = mutableMapOf("env" to "prod"),
        )
        val test = BoMEntity(
            type = "Person",
            schemaVersion = "1",
            annotations = mutableMapOf("env" to "test"),
        )
        assertThat(adapted.matches(BoMEntityDomainCandidate(prod))).isTrue()
        assertThat(adapted.matches(BoMEntityDomainCandidate(test))).isFalse()
    }

    @Test
    fun shouldKeepInducedEdgeDefault() {
        val matcher = MatchAllAnnotationMatcher(mapOf("env" to "test"))
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()
        val edge = BoMEdgeDomainCandidate(
            org.poc.objs.core.domain.BoMEdge(source = a, target = b, role = "knows"),
        )
        assertThat(matcher.matchesEdge(edge, setOf(a, b))).isTrue()
        assertThat(matcher.matchesEdge(edge, setOf(a, c))).isFalse()
    }
}
