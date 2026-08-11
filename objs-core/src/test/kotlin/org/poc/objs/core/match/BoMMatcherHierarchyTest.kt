package org.poc.objs.core.match

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.poc.objs.core.domain.BoMEntity
import java.util.UUID

class BoMMatcherHierarchyTest {

    @Test
    fun shouldExposeSourceCapablePushdownForPostgres() {
        val matcher = BoMObjExprMatcher("a.app == 'payments' && a.appVersion == '1.0.0'")
        assertThat(matcher).isInstanceOf(BoMSourceCapableMatcher::class.java)
        assertThat(matcher.localEvalOnly).isFalse()
        val backend = object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): BoMCandidateSource = BoMCandidateSource { emptyList() }
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                null
            override fun objExprPushdownSource(plan: BoMObjExprPushdown): BoMCandidateSource? {
                assertThat(plan.annotationEquals)
                    .containsEntry("app", "payments")
                    .containsEntry("appVersion", "1.0.0")
                return BoMCandidateSource { emptyList() }
            }
        }
        assertThat(matcher.toCandidateSource(backend)).isNotNull()
        val noPushdown = object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = false
            override fun allEntitiesSource(): BoMCandidateSource = BoMCandidateSource { emptyList() }
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                null
        }
        assertThat(matcher.toCandidateSource(noPushdown)).isNull()
    }

    @Test
    fun shouldMatchDomainCandidatesConsistently() {
        val matcher = BoMObjExprMatcher("a.env == 'test'")
        val entity = BoMEntity(
            id = UUID.randomUUID(),
            type = "Person",
            schemaVersion = "1",
            annotations = mutableMapOf("env" to "test", "team" to "core"),
        )
        val candidate = BoMEntityDomainCandidate(entity)
        assertThat(matcher.matches(candidate)).isTrue()
        assertThat(matcher.matches(BoMEntityDomainCandidate(entity))).isTrue()
    }

    @Test
    fun shouldTreatGraphExprAsHeaderOnlyMatcher() {
        val matcher = BoMGraphExprMatcher("a.decisionId == 'D-1'")
        assertThat(matcher).isNotInstanceOf(BoMSourceCapableMatcher::class.java)
        val candidate = BoMEntityDomainCandidate(
            BoMEntity(type = "Person", schemaVersion = "1", annotations = mutableMapOf()),
        )
        assertThat(matcher.matches(candidate)).isTrue()
        assertThat(matcher.matchesHeader(UUID.randomUUID(), mapOf("decisionId" to "D-1"))).isTrue()
        assertThat(matcher.matchesHeader(UUID.randomUUID(), mapOf("decisionId" to "other"))).isFalse()
    }

    @Test
    fun shouldTreatAllGraphsMatcherAsHeaderOnlyMatcher() {
        assertThat(BoMAllGraphsMatcher).isNotInstanceOf(BoMSourceCapableMatcher::class.java)
        val candidate = BoMEntityDomainCandidate(
            BoMEntity(type = "Person", schemaVersion = "1", annotations = mutableMapOf()),
        )
        assertThat(BoMAllGraphsMatcher.matches(candidate)).isTrue()
    }

    @Test
    fun shouldKeepInducedEdgeDefault() {
        val matcher = BoMObjExprMatcher("a.env == 'test'")
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
