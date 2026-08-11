package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoMEntitySelectionPlanTest {

    private val allEntities = BoMCandidateSource { emptyList() }
    private val sqlSource = BoMCandidateSource { emptyList() }

    private fun postgresBackend(pushdown: BoMCandidateSource? = sqlSource) =
        object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): BoMCandidateSource = allEntities
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                null
            override fun objExprPushdownSource(plan: BoMObjExprPushdown): BoMCandidateSource? = pushdown
        }

    @Test
    fun shouldUseSqlSourceWhenObjExprIsLowerable() {
        val matcher = BoMObjExprMatcher("a.app == 'payments' && a.env == 'prod'")
        assertThat(matcher.localEvalOnly).isFalse()
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isFalse()
        assertThat(plan.source).isSameAs(sqlSource)
        assertThat(plan.filters).isEmpty()
    }

    @Test
    fun shouldSwitchToLocalEvalWhenObjExprCannotConvertToSql() {
        val matcher = BoMObjExprMatcher("a.team != null || a.app == 'x'")
        assertThat(matcher.localEvalOnly).isTrue()
        assertThat(matcher.pushdown).isNull()
        assertThat(matcher.toCandidateSource(postgresBackend())).isNull()

        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isTrue()
        assertThat(plan.source).isSameAs(allEntities)
        assertThat(plan.filters).containsExactly(matcher)
    }

    @Test
    fun shouldSwitchToLocalEvalWhenBackendRejectsPushdown() {
        val matcher = BoMObjExprMatcher("a.app == 'payments'")
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
    fun shouldAttachEdgeStrategyWhenPushdownSourceProvidesOne() {
        val edgeStrategy = BoMEdgeCandidateStrategy { _, _ -> emptyList() }
        val withEdges = object : BoMCandidateSourceWithEdges {
            override fun collect(checkBudget: () -> Unit) = emptyList<BoMEntityMatchCandidate>()
            override val edgeStrategy: BoMEdgeCandidateStrategy = edgeStrategy
        }
        val backend = object : BoMEntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): BoMCandidateSource = allEntities
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
                null
            override fun objExprPushdownSource(plan: BoMObjExprPushdown): BoMCandidateSource? = withEdges
        }
        val plan = BoMEntitySelectionPlan.resolve(
            listOf(BoMObjExprMatcher("a.app == 'x'")),
            backend,
        )
        assertThat(plan.localEval).isFalse()
        assertThat(plan.edgeStrategy).isSameAs(edgeStrategy)
    }

    @Test
    fun shouldOmitEdgeStrategyForLocalEvalAllEntities() {
        val matcher = BoMObjExprMatcher("a.team != null")
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isTrue()
        assertThat(plan.edgeStrategy).isNull()
    }

    @Test
    fun shouldUseSqlSourceForMultiFieldEquality() {
        val matcher = BoMObjExprMatcher(
            "type == 'Product' && a.app == 'payments' && p.sku == 'x'",
        )
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isFalse()
        assertThat(plan.source).isSameAs(sqlSource)
    }
}
