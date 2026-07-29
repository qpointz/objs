package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.validation.BoMValidationException
import java.util.UUID

class BoMMatcherDslTest {
    private val dsl = BoMMatcherDsl.create()

    @Test
    fun shouldDecodeAnnoObjectFromJsonAndYaml() {
        val json = """{"anno":{"app":"payments","env":"prod"}}"""
        val yaml = """
            anno:
              app: payments
              env: prod
        """.trimIndent()

        val fromJson = dsl.decode(json, BoMMatcherFormat.JSON)
        val fromYaml = dsl.decode(yaml, BoMMatcherFormat.YAML)

        assertThat(fromJson).isInstanceOf(MatchAllAnnotationMatcher::class.java)
        assertThat(fromYaml).isInstanceOf(MatchAllAnnotationMatcher::class.java)
        assertThat((fromJson as MatchAllAnnotationMatcher).filter)
            .isEqualTo(mapOf("app" to "payments", "env" to "prod"))
        assertThat((fromYaml as MatchAllAnnotationMatcher).filter)
            .isEqualTo(mapOf("app" to "payments", "env" to "prod"))

        val entity = candidate("app" to "payments", "env" to "prod", "team" to "core")
        assertThat(fromJson.matches(entity)).isTrue()
        assertThat(fromYaml.matches(entity)).isTrue()
    }

    @Test
    fun shouldDecodeChainedMatchersInOrder() {
        val matcher = dsl.decode(
            """
            [
              {"anno":{"env":"prod"}},
              {"anno-expr":"app == 'payments'"}
            ]
            """.trimIndent(),
            BoMMatcherFormat.JSON,
        )

        assertThat(matcher).isInstanceOf(BoMChainedMatcher::class.java)
        val chained = matcher as BoMChainedMatcher
        assertThat(chained.matchers).hasSize(2)
        assertThat(chained.matchers[0]).isInstanceOf(MatchAllAnnotationMatcher::class.java)
        assertThat(chained.matchers[1]).isInstanceOf(BoMAnnoExprMatcher::class.java)

        assertThat(matcher.matches(candidate("env" to "prod", "app" to "payments"))).isTrue()
        assertThat(matcher.matches(candidate("env" to "prod", "app" to "other"))).isFalse()
        assertThat(matcher.matches(candidate("env" to "test", "app" to "payments"))).isFalse()
    }

    @Test
    fun shouldRejectEmptyUnknownAndMultiKeyMatchers() {
        assertThatThrownBy { dsl.decode("[]", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { dsl.decode("{}", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"anno":{},"anno-expr":"x==1"}""", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"unknown":{}}""", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"anno":{}}""", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
    }

    @Test
    fun shouldRoundTripEncodeDecode() {
        val original = dsl.decode(
            """
            - anno:
                env: prod
            - anno-expr: "team != null"
            """.trimIndent(),
            BoMMatcherFormat.YAML,
        )
        val encoded = dsl.encode(original, BoMMatcherFormat.JSON)
        val roundTrip = dsl.decode(encoded, BoMMatcherFormat.JSON)
        assertThat(roundTrip).isInstanceOf(BoMChainedMatcher::class.java)
        assertThat(roundTrip.matches(candidate("env" to "prod", "team" to "core"))).isTrue()
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
