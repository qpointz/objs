package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.validation.BoMValidationException
import java.util.UUID

/**
 * C-13 DSL: `all` / `graph-expr` / `obj-expr` / chained array.
 * See [shouldRejectRetiredKeysWithMigrateMessage] for the retired `anno` / `anno-expr` / `ids` /
 * `subgraph` / `subg-expr` keys (G-G17).
 */
class BoMMatcherDslTest {
    private val dsl = BoMMatcherDsl.create()

    @Test
    fun shouldDecodeAllMatcher() {
        val fromJson = dsl.decode("""{"all":true}""", BoMMatcherFormat.JSON)
        val fromYaml = dsl.decode("all: true", BoMMatcherFormat.YAML)
        assertThat(fromJson).isSameAs(BoMAllGraphsMatcher)
        assertThat(fromYaml).isSameAs(BoMAllGraphsMatcher)
        assertThat(dsl.encode(BoMAllGraphsMatcher, BoMMatcherFormat.JSON)).contains("\"all\"").contains("true")
    }

    @Test
    fun shouldRejectAllWhenNotBooleanTrue() {
        assertThatThrownBy { dsl.decode("""{"all":false}""", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"all":"yes"}""", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
    }

    @Test
    fun shouldDecodeGraphExprFromJsonAndYaml() {
        val json = """{"graph-expr":"a.env == 'prod'"}"""
        val yaml = """
            graph-expr: "a.env == 'prod'"
        """.trimIndent()

        val fromJson = dsl.decode(json, BoMMatcherFormat.JSON)
        val fromYaml = dsl.decode(yaml, BoMMatcherFormat.YAML)

        assertThat(fromJson).isInstanceOf(BoMGraphExprMatcher::class.java)
        assertThat(fromYaml).isInstanceOf(BoMGraphExprMatcher::class.java)
        assertThat((fromJson as BoMGraphExprMatcher).matchesHeader(UUID.randomUUID(), mapOf("env" to "prod"))).isTrue()
        assertThat((fromYaml as BoMGraphExprMatcher).matchesHeader(UUID.randomUUID(), mapOf("env" to "test"))).isFalse()
    }

    @Test
    fun shouldDecodeObjExpr() {
        val obj = dsl.decode("""{"obj-expr":"type == 'Product' && a.env == 'prod'"}""", BoMMatcherFormat.JSON)
        assertThat(obj).isInstanceOf(BoMObjExprMatcher::class.java)
        assertThat((obj as BoMObjExprMatcher).expression).contains("Product")
    }

    @Test
    fun shouldDecodeChainedGraphExprThenObjExprInOrder() {
        val matcher = dsl.decode(
            """
            [
              {"graph-expr":"a.env == 'prod'"},
              {"obj-expr":"a.app == 'payments'"}
            ]
            """.trimIndent(),
            BoMMatcherFormat.JSON,
        )

        assertThat(matcher).isInstanceOf(BoMChainedMatcher::class.java)
        val chained = matcher as BoMChainedMatcher
        assertThat(chained.matchers).hasSize(2)
        assertThat(chained.matchers[0]).isInstanceOf(BoMGraphExprMatcher::class.java)
        assertThat(chained.matchers[1]).isInstanceOf(BoMObjExprMatcher::class.java)

        // BoMGraphExprMatcher.matches() is always true (header-only); obj-expr does the filtering.
        assertThat(matcher.matches(candidate("app" to "payments"))).isTrue()
        assertThat(matcher.matches(candidate("app" to "other"))).isFalse()
    }

    @Test
    fun shouldRejectEmptyUnknownAndMultiKeyMatchers() {
        assertThatThrownBy { dsl.decode("[]", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { dsl.decode("{}", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy {
            dsl.decode("""{"obj-expr":"type == 'A'","graph-expr":"id == '1'"}""", BoMMatcherFormat.JSON)
        }.isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"unknown":{}}""", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"obj-expr":""}""", BoMMatcherFormat.JSON) }
            .isInstanceOf(BoMValidationException::class.java)
    }

    @Test
    fun shouldRoundTripEncodeDecode() {
        val original = dsl.decode(
            """
            - graph-expr: "a.env == 'prod'"
            - obj-expr: "a.team == 'core'"
            """.trimIndent(),
            BoMMatcherFormat.YAML,
        )
        val encoded = dsl.encode(original, BoMMatcherFormat.JSON)
        val roundTrip = dsl.decode(encoded, BoMMatcherFormat.JSON)
        assertThat(roundTrip).isInstanceOf(BoMChainedMatcher::class.java)
        assertThat(roundTrip.matches(candidate("team" to "core"))).isTrue()
        assertThat(roundTrip.matches(candidate("team" to "other"))).isFalse()
    }

    @Test
    fun shouldRejectRetiredKeysWithMigrateMessage() {
        val cases = listOf(
            """{"anno":{"env":"prod"}}""",
            """{"anno-expr":"env == 'prod'"}""",
            """{"ids":["11111111-1111-4111-8111-111111111111"]}""",
            """{"subgraph":{"id":"11111111-1111-4111-8111-111111111111"}}""",
            """{"subg-expr":"id == '1'"}""",
        )
        cases.forEach { body ->
            val ex = catchThrowableOfType(BoMValidationException::class.java) {
                dsl.decode(body, BoMMatcherFormat.JSON)
            }
            assertThat(ex).isNotNull()
            assertThat(ex.result.issues).anySatisfy { issue ->
                assertThat(issue.code).isEqualTo("MATCHER_DSL_RETIRED_KEY")
                assertThat(issue.message).containsIgnoringCase("retired")
            }
        }
    }

    @Test
    fun shouldFailToEncodeUnsupportedMatcherTypes() {
        val unsupported = object : BoMMatcher {
            override fun matches(candidate: BoMEntityMatchCandidate): Boolean = true
        }
        assertThatThrownBy { dsl.encode(unsupported) }
            .isInstanceOf(BoMValidationException::class.java)
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
