package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EntityColumnProjectionTest {

    private val all = CandidateSource { emptyList() }
    private val sql = CandidateSource { emptyList() }

    private val postgres = object : EntityCandidateBackend {
        override val isPostgres: Boolean = true
        override fun allEntitiesSource(): CandidateSource = all
        override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): CandidateSource? =
            null
        override fun objExprPushdownSource(plan: ObjExprPushdown): CandidateSource? = sql
    }

    @Test
    fun shouldOmitJsonColumnsForPureSqlSource() {
        val matcher = ObjExprMatcher("a.env == 'prod'")
        val plan = EntitySelectionPlan.resolve(listOf(matcher), postgres)
        assertThat(plan.localEval).isFalse()
        assertThat(plan.filters).isEmpty()
        val projection = EntityColumnProjection.forPlan(plan)
        assertThat(projection.includePayload).isFalse()
        assertThat(projection.includeAnnotations).isFalse()
    }

    @Test
    fun shouldIncludeAnnotationsAndPayloadForLocalEvalObjExprFilters() {
        // obj-expr may read both a.* and p.* namespaces, so local eval always needs both columns.
        val matcher = ObjExprMatcher("a.team != null")
        val plan = EntitySelectionPlan.resolve(listOf(matcher), postgres)
        assertThat(plan.localEval).isTrue()
        val projection = EntityColumnProjection.forPlan(plan)
        assertThat(projection.includePayload).isTrue()
        assertThat(projection.includeAnnotations).isTrue()
    }
}
