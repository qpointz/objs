package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoMEntityColumnProjectionTest {

    private val all = BoMCandidateSource { emptyList() }
    private val sql = BoMCandidateSource { emptyList() }

    private val postgres = object : BoMEntityCandidateBackend {
        override val isPostgres: Boolean = true
        override fun allEntitiesSource(): BoMCandidateSource = all
        override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? =
            if (disjuncts.isEmpty()) null else sql
    }

    @Test
    fun shouldOmitJsonColumnsForPureSqlSource() {
        val matcher = MatchAllAnnotationMatcher(mapOf("env" to "prod"))
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgres)
        assertThat(plan.localEval).isFalse()
        assertThat(plan.filters).isEmpty()
        val projection = BoMEntityColumnProjection.forPlan(plan)
        assertThat(projection.includePayload).isFalse()
        assertThat(projection.includeAnnotations).isFalse()
    }

    @Test
    fun shouldIncludeAnnotationsForLocalEvalFilters() {
        val matcher = BoMAnnoExprMatcher("team != null")
        val plan = BoMEntitySelectionPlan.resolve(listOf(matcher), postgres)
        assertThat(plan.localEval).isTrue()
        val projection = BoMEntityColumnProjection.forPlan(plan)
        assertThat(projection.includePayload).isFalse()
        assertThat(projection.includeAnnotations).isTrue()
    }
}
