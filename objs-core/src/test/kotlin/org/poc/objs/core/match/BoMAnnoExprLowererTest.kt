package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoMAnnoExprLowererTest {

    @Test
    fun shouldLowerEqualityConjunction() {
        val compiled = BoMAnnoExprEngine.compile("version == '1.0.0' && app == 'aapp-lala'")
        assertThat(BoMAnnoExprLowerer.toMatchExpression(compiled)).isEqualTo(
            BoMMatchExpression.And(
                listOf(
                    BoMMatchExpression.AnnotationEquals("version", "1.0.0"),
                    BoMMatchExpression.AnnotationEquals("app", "aapp-lala"),
                ),
            ),
        )
        assertThat(BoMAnnoExprLowerer.toContainmentDisjuncts(compiled)).containsExactly(
            mapOf("version" to "1.0.0", "app" to "aapp-lala"),
        )
    }

    @Test
    fun shouldLowerReversedOperandOrderAndParentheses() {
        val compiled = BoMAnnoExprEngine.compile("('x' == app) && (env == 'prod')")
        assertThat(BoMAnnoExprLowerer.toMatchExpression(compiled)).isEqualTo(
            BoMMatchExpression.And(
                listOf(
                    BoMMatchExpression.AnnotationEquals("app", "x"),
                    BoMMatchExpression.AnnotationEquals("env", "prod"),
                ),
            ),
        )
    }

    @Test
    fun shouldLowerOrAndDistributeAndOverOr() {
        val compiled = BoMAnnoExprEngine.compile(
            "(app == 'app-00021' || app == 'app-00022') && appVersion == '1.0.0'",
        )
        assertThat(BoMAnnoExprLowerer.toContainmentDisjuncts(compiled)).containsExactlyInAnyOrder(
            mapOf("app" to "app-00021", "appVersion" to "1.0.0"),
            mapOf("app" to "app-00022", "appVersion" to "1.0.0"),
        )
    }

    @Test
    fun shouldNotLowerUnsupportedShapes() {
        assertThat(BoMAnnoExprLowerer.toMatchExpression(BoMAnnoExprEngine.compile("team != null"))).isNull()
        assertThat(BoMAnnoExprLowerer.toMatchExpression(BoMAnnoExprEngine.compile("app == 1"))).isNull()
        assertThat(BoMAnnoExprLowerer.toContainmentDisjuncts(BoMAnnoExprEngine.compile("a == '1' || b == '2'")))
            .containsExactlyInAnyOrder(
                mapOf("a" to "1"),
                mapOf("b" to "2"),
            )
    }
}
