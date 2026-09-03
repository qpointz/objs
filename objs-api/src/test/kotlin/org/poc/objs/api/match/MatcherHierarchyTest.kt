package org.poc.objs.api.match

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.poc.objs.api.domain.Entity
import java.util.UUID

class MatcherHierarchyTest {

    @Test
    fun shouldExposeSourceCapablePushdownForPostgres() {
        val matcher = ObjExprMatcher("a.app == 'payments' && a.appVersion == '1.0.0'")
        assertThat(matcher).isInstanceOf(SourceCapableMatcher::class.java)
        assertThat(matcher.localEvalOnly).isFalse()
        val backend = object : EntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): CandidateSource = CandidateSource { emptyList() }
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): CandidateSource? =
                null
            override fun objExprPushdownSource(plan: ObjExprPushdown): CandidateSource? {
                assertThat(plan.annotationEquals)
                    .containsEntry("app", "payments")
                    .containsEntry("appVersion", "1.0.0")
                return CandidateSource { emptyList() }
            }
        }
        assertThat(matcher.toCandidateSource(backend)).isNotNull()
        val noPushdown = object : EntityCandidateBackend {
            override val isPostgres: Boolean = false
            override fun allEntitiesSource(): CandidateSource = CandidateSource { emptyList() }
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): CandidateSource? =
                null
        }
        assertThat(matcher.toCandidateSource(noPushdown)).isNull()
    }

    @Test
    fun shouldMatchDomainCandidatesConsistently() {
        val matcher = ObjExprMatcher("a.env == 'test'")
        val entity = Entity(
            id = UUID.randomUUID(),
            type = "Person",
            schemaVersion = "1",
            annotations = mutableMapOf("env" to "test", "team" to "core"),
        )
        val candidate = EntityDomainCandidate(entity)
        assertThat(matcher.matches(candidate)).isTrue()
        assertThat(matcher.matches(EntityDomainCandidate(entity))).isTrue()
    }

    @Test
    fun shouldTreatGraphExprAsHeaderOnlyMatcher() {
        val matcher = GraphExprMatcher("a.decisionId == 'D-1'")
        assertThat(matcher).isNotInstanceOf(SourceCapableMatcher::class.java)
        val candidate = EntityDomainCandidate(
            Entity(type = "Person", schemaVersion = "1", annotations = mutableMapOf()),
        )
        assertThat(matcher.matches(candidate)).isTrue()
        assertThat(matcher.matchesHeader(UUID.randomUUID(), mapOf("decisionId" to "D-1"))).isTrue()
        assertThat(matcher.matchesHeader(UUID.randomUUID(), mapOf("decisionId" to "other"))).isFalse()
    }

    @Test
    fun shouldTreatAllGraphsMatcherAsHeaderOnlyMatcher() {
        assertThat(AllGraphsMatcher).isNotInstanceOf(SourceCapableMatcher::class.java)
        val candidate = EntityDomainCandidate(
            Entity(type = "Person", schemaVersion = "1", annotations = mutableMapOf()),
        )
        assertThat(AllGraphsMatcher.matches(candidate)).isTrue()
    }

    @Test
    fun shouldKeepInducedEdgeDefault() {
        val matcher = ObjExprMatcher("a.env == 'test'")
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()
        val edge = EdgeDomainCandidate(
            org.poc.objs.api.domain.Edge(source = a, target = b, role = "knows"),
        )
        assertThat(matcher.matchesEdge(edge, setOf(a, b))).isTrue()
        assertThat(matcher.matchesEdge(edge, setOf(a, c))).isFalse()
    }
}
