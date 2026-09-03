package org.poc.objs.api.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EntitySelectionPlanTest {

    private val allEntities = CandidateSource { emptyList() }
    private val sqlSource = CandidateSource { emptyList() }

    private fun postgresBackend(pushdown: CandidateSource? = sqlSource) =
        object : EntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): CandidateSource = allEntities
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): CandidateSource? =
                null
            override fun objExprPushdownSource(plan: ObjExprPushdown): CandidateSource? = pushdown
        }

    @Test
    fun shouldUseSqlSourceWhenObjExprIsLowerable() {
        val matcher = ObjExprMatcher("a.app == 'payments' && a.env == 'prod'")
        assertThat(matcher.localEvalOnly).isFalse()
        val plan = EntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isFalse()
        assertThat(plan.source).isSameAs(sqlSource)
        assertThat(plan.filters).isEmpty()
    }

    @Test
    fun shouldSwitchToLocalEvalWhenObjExprCannotConvertToSql() {
        val matcher = ObjExprMatcher("a.team != null || a.app == 'x'")
        assertThat(matcher.localEvalOnly).isTrue()
        assertThat(matcher.pushdown).isNull()
        assertThat(matcher.toCandidateSource(postgresBackend())).isNull()

        val plan = EntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isTrue()
        assertThat(plan.source).isSameAs(allEntities)
        assertThat(plan.filters).containsExactly(matcher)
    }

    @Test
    fun shouldSwitchToLocalEvalWhenBackendRejectsPushdown() {
        val matcher = ObjExprMatcher("a.app == 'payments'")
        assertThat(matcher.localEvalOnly).isFalse()
        val h2 = object : EntityCandidateBackend {
            override val isPostgres: Boolean = false
            override fun allEntitiesSource(): CandidateSource = allEntities
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): CandidateSource? =
                null
        }
        val plan = EntitySelectionPlan.resolve(listOf(matcher), h2)
        assertThat(plan.localEval).isTrue()
        assertThat(plan.source).isSameAs(allEntities)
        assertThat(plan.filters).containsExactly(matcher)
        assertThat(plan.edgeStrategy).isNull()
    }

    @Test
    fun shouldAttachEdgeStrategyWhenPushdownSourceProvidesOne() {
        val edgeStrategy = EdgeCandidateStrategy { _, _ -> emptyList() }
        val withEdges = object : CandidateSourceWithEdges {
            override fun collect(checkBudget: () -> Unit) = emptyList<EntityMatchCandidate>()
            override val edgeStrategy: EdgeCandidateStrategy = edgeStrategy
        }
        val backend = object : EntityCandidateBackend {
            override val isPostgres: Boolean = true
            override fun allEntitiesSource(): CandidateSource = allEntities
            override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): CandidateSource? =
                null
            override fun objExprPushdownSource(plan: ObjExprPushdown): CandidateSource? = withEdges
        }
        val plan = EntitySelectionPlan.resolve(
            listOf(ObjExprMatcher("a.app == 'x'")),
            backend,
        )
        assertThat(plan.localEval).isFalse()
        assertThat(plan.edgeStrategy).isSameAs(edgeStrategy)
    }

    @Test
    fun shouldOmitEdgeStrategyForLocalEvalAllEntities() {
        val matcher = ObjExprMatcher("a.team != null")
        val plan = EntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isTrue()
        assertThat(plan.edgeStrategy).isNull()
    }

    @Test
    fun shouldUseSqlSourceForMultiFieldEquality() {
        val matcher = ObjExprMatcher(
            "type == 'Product' && a.app == 'payments' && p.sku == 'x'",
        )
        val plan = EntitySelectionPlan.resolve(listOf(matcher), postgresBackend())
        assertThat(plan.localEval).isFalse()
        assertThat(plan.source).isSameAs(sqlSource)
    }
}
