package org.poc.objs.api.match

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.validation.ValidationException
import java.util.UUID

/**
 * C-13 DSL: `all` / `graph-expr` / `obj-expr` / chained array.
 * See [shouldRejectRetiredKeysWithMigrateMessage] for the retired `anno` / `anno-expr` / `ids` /
 * `subgraph` / `subg-expr` keys (G-G17).
 */
class MatcherDslTest {
    private val dsl = MatcherDsl.create()

    @Test
    fun shouldDecodeAllMatcher() {
        val fromJson = dsl.decode("""{"all":true}""", MatcherFormat.JSON)
        val fromYaml = dsl.decode("all: true", MatcherFormat.YAML)
        assertThat(fromJson).isSameAs(AllGraphsMatcher)
        assertThat(fromYaml).isSameAs(AllGraphsMatcher)
        assertThat(dsl.encode(AllGraphsMatcher, MatcherFormat.JSON)).contains("\"all\"").contains("true")
    }

    @Test
    fun shouldRejectAllWhenNotBooleanTrue() {
        assertThatThrownBy { dsl.decode("""{"all":false}""", MatcherFormat.JSON) }
            .isInstanceOf(ValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"all":"yes"}""", MatcherFormat.JSON) }
            .isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun shouldDecodeGraphExprFromJsonAndYaml() {
        val json = """{"graph-expr":"a.env == 'prod'"}"""
        val yaml = """
            graph-expr: "a.env == 'prod'"
        """.trimIndent()

        val fromJson = dsl.decode(json, MatcherFormat.JSON)
        val fromYaml = dsl.decode(yaml, MatcherFormat.YAML)

        assertThat(fromJson).isInstanceOf(GraphExprMatcher::class.java)
        assertThat(fromYaml).isInstanceOf(GraphExprMatcher::class.java)
        assertThat((fromJson as GraphExprMatcher).matchesHeader(UUID.randomUUID(), mapOf("env" to "prod"))).isTrue()
        assertThat((fromYaml as GraphExprMatcher).matchesHeader(UUID.randomUUID(), mapOf("env" to "test"))).isFalse()
    }

    @Test
    fun shouldDecodeObjExpr() {
        val obj = dsl.decode("""{"obj-expr":"type == 'Product' && a.env == 'prod'"}""", MatcherFormat.JSON)
        assertThat(obj).isInstanceOf(ObjExprMatcher::class.java)
        assertThat((obj as ObjExprMatcher).expression).contains("Product")
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
            MatcherFormat.JSON,
        )

        assertThat(matcher).isInstanceOf(ChainedMatcher::class.java)
        val chained = matcher as ChainedMatcher
        assertThat(chained.matchers).hasSize(2)
        assertThat(chained.matchers[0]).isInstanceOf(GraphExprMatcher::class.java)
        assertThat(chained.matchers[1]).isInstanceOf(ObjExprMatcher::class.java)

        // GraphExprMatcher.matches() is always true (header-only); obj-expr does the filtering.
        assertThat(matcher.matches(candidate("app" to "payments"))).isTrue()
        assertThat(matcher.matches(candidate("app" to "other"))).isFalse()
    }

    @Test
    fun shouldDecodeGraphsInFromJsonAndYaml() {
        val id1 = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val id2 = UUID.fromString("22222222-2222-4222-8222-222222222222")
        val fromJson = dsl.decode(
            """{"graphs-in":["$id1","$id2"]}""",
            MatcherFormat.JSON,
        )
        val fromYaml = dsl.decode(
            """
            graphs-in:
              - $id1
              - $id2
            """.trimIndent(),
            MatcherFormat.YAML,
        )
        assertThat(fromJson).isInstanceOf(GraphIdsMatcher::class.java)
        assertThat((fromJson as GraphIdsMatcher).graphIds).containsExactly(id1, id2)
        assertThat((fromYaml as GraphIdsMatcher).graphIds).containsExactly(id1, id2)
        val encoded = dsl.encode(fromJson, MatcherFormat.JSON)
        val roundTrip = dsl.decode(encoded, MatcherFormat.JSON) as GraphIdsMatcher
        assertThat(roundTrip.graphIds).containsExactly(id1, id2)
    }

    @Test
    fun shouldRejectGraphsInWhenNotArray() {
        assertThatThrownBy { dsl.decode("""{"graphs-in":"nope"}""", MatcherFormat.JSON) }
            .isInstanceOf(ValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"graphs-in":["not-a-uuid"]}""", MatcherFormat.JSON) }
            .isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun shouldRejectEmptyUnknownAndMultiKeyMatchers() {
        assertThatThrownBy { dsl.decode("[]", MatcherFormat.JSON) }
            .isInstanceOf(ValidationException::class.java)
        assertThatThrownBy { dsl.decode("{}", MatcherFormat.JSON) }
            .isInstanceOf(ValidationException::class.java)
        assertThatThrownBy {
            dsl.decode("""{"obj-expr":"type == 'A'","graph-expr":"id == '1'"}""", MatcherFormat.JSON)
        }.isInstanceOf(ValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"unknown":{}}""", MatcherFormat.JSON) }
            .isInstanceOf(ValidationException::class.java)
        assertThatThrownBy { dsl.decode("""{"obj-expr":""}""", MatcherFormat.JSON) }
            .isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun shouldRoundTripEncodeDecode() {
        val original = dsl.decode(
            """
            - graph-expr: "a.env == 'prod'"
            - obj-expr: "a.team == 'core'"
            """.trimIndent(),
            MatcherFormat.YAML,
        )
        val encoded = dsl.encode(original, MatcherFormat.JSON)
        val roundTrip = dsl.decode(encoded, MatcherFormat.JSON)
        assertThat(roundTrip).isInstanceOf(ChainedMatcher::class.java)
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
            val ex = catchThrowableOfType(ValidationException::class.java) {
                dsl.decode(body, MatcherFormat.JSON)
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
        val unsupported = object : Matcher {
            override fun matches(candidate: EntityMatchCandidate): Boolean = true
        }
        assertThatThrownBy { dsl.encode(unsupported) }
            .isInstanceOf(ValidationException::class.java)
    }

    private fun candidate(vararg annotations: Pair<String, String>): EntityMatchCandidate =
        EntityDomainCandidate(
            Entity(
                id = UUID.randomUUID(),
                type = "Thing",
                schemaVersion = "1",
                annotations = annotations.toMap().toMutableMap(),
            ),
        )
}
