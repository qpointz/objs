package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoMEntitySelectionPlanTest {

    private val allEntities = BoMCandidateSource { emptyList() }
    private val sqlSource = BoMCandidateSource { emptyList() }

    private fun postgresBackend(containment: BoMCandidateSource? = sqlSource) =
        object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): BoMCandidateSource = allEntities
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                if (disjuncts.isEmpty()) null else containment
        }

    @Test
    fun shouldUseSqlSourceWhenAnnoExprIsLowerable() {
        val matcher = BoMAnnoExprMatcher("app == 'payments' && env == 'prod'")
        assertThat(matcher.localEvalOnly).isFalse()
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isFalse()
        assertThat(plan.source).isSameAs(sqlSource)
        assertThat(plan.filters).isEmpty()
    }

    @Test
    fun shouldSwitchToLocalEvalWhenAnnoExprCannotConvertToSql() {
        val matcher = BoMAnnoExprMatcher("team != null || app == 'x'")
        assertThat(matcher.localEvalOnly).isTrue()
        assertThat(matcher.sqlContainmentDisjuncts).isNull()
        assertThat(matcher.toCandidateSource(postgresBackend())).isNull()

        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isTrue()
        assertThat(plan.source).isSameAs(allEntities)
        assertThat(plan.filters).containsExactly(matcher)
    }

    @Test
    fun shouldSwitchToLocalEvalWhenBackendRejectsContainment() {
        val matcher = BoMAnnoExprMatcher("app == 'payments'")
        assertThat(matcher.localEvalOnly).isFalse()
        val h2 = object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = false
            override fun allEntitiesSource(): BoMCandidateSource = allEntities
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                null
        }
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), h2)
        assertThat(plan.localEval).isTrue()
        assertThat(plan.source).isSameAs(allEntities)
        assertThat(plan.filters).containsExactly(matcher)
        assertThat(plan.edgeStrategy).isNull()
    }

    @Test
    fun shouldAttachEdgeStrategyWhenContainmentSourceProvidesOne() {
        val edgeStrategy = BoMEdgeCandidateStrategy { _, _ -> emptyList() }
        val withEdges = object : BoMCandidateSourceWithEdges {
            override fun collect(checkBudget: () -> Unit) = emptyList<BoMEntityMatchCandidate>()
            override val edgeStrategy: BoMEdgeCandidateStrategy = edgeStrategy
        }
        val backend = object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): BoMCandidateSource = allEntities
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                withEdges
        }
        val plan = BoMEntitySelectionPlan.resolve(
            listOf(MatchAllAnnotationMatcher(mapOf("app" to "x"))),
            backend,
        )
        assertThat(plan.localEval).isFalse()
        assertThat(plan.edgeStrategy).isSameAs(edgeStrategy)
    }

    @Test
    fun shouldOmitEdgeStrategyForLocalEvalAllEntities() {
        val matcher = BoMAnnoExprMatcher("team != null")
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isTrue()
        assertThat(plan.edgeStrategy).isNull()
    }

    @Test
    fun shouldUseSqlSourceForOrAnnoExpr() {
        val matcher = BoMAnnoExprMatcher(
            "(app == 'app-00021' || app == 'app-00022') && appVersion == '1.0.0'",
        )
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isFalse()
        assertThat(plan.source).isSameAs(sqlSource)
    }
}
