package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.match.MatchAllAnnotationMatcher
import java.util.UUID

class BoMRawEntityCandidateLazyTest {

    @Test
    fun shouldNotParsePayload_whenExcludedByAnnotationFilter() {
        val candidate = BoMRawEntityCandidate(
            id = UUID.randomUUID(),
            type = "Thing",
            schemaVersion = "1",
            payloadJson = """{"name":"heavy","blob":"${"x".repeat(1000)}"}""",
            annotationsJson = """{"env":"prod"}""",
        )
        val matcher = MatchAllAnnotationMatcher(mapOf("env" to "dev"))
        assertThat(matcher.matches(candidate)).isFalse()
        assertThat(candidate.payloadParseInvocations()).isEqualTo(0)
        assertThat(candidate.annotationsParseInvocations()).isEqualTo(1)
    }

    @Test
    fun shouldMatchAnnotationsWithoutMaterializingPayload() {
        val candidate = BoMRawEntityCandidate(
            id = UUID.randomUUID(),
            type = "Thing",
            schemaVersion = "1",
            payloadJson = """{"name":"Alice"}""",
            annotationsJson = """{"env":"prod","team":"core"}""",
        )
        val matcher = MatchAllAnnotationMatcher(mapOf("env" to "prod", "team" to "core"))
        assertThat(matcher.matches(candidate)).isTrue()
        assertThat(candidate.payloadParseInvocations()).isEqualTo(0)
    }
}
