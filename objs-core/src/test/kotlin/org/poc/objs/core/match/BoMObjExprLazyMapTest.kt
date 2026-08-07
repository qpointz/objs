package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.persistence.LazyJsonMap
import java.util.UUID

class BoMObjExprLazyMapTest {
    @Test
    fun shouldEvaluateDotAccessOnLazyJsonMap() {
        val matcher = BoMObjExprMatcher("type == 'Policy' && a.app == 'app-00001'")
        val candidate = object : BoMEntityMatchCandidate {
            override val id: UUID = UUID.randomUUID()
            override val type: String = "Policy"
            override val schemaVersion: String = "1.0.0"
            override val annotations = LazyJsonMap.annotations("""{"app":"app-00001"}""")
            override val payload = LazyJsonMap.payload("""{}""")
            override fun toDomain() = error("n/a")
        }
        assertThat(matcher.matches(candidate)).isTrue()
    }

    @Test
    fun shouldEvaluateBracketAccessOnLazyJsonMap() {
        val matcher = BoMObjExprMatcher("a['app'] == 'app-00001' && p['name'] == 'n'")
        val candidate = object : BoMEntityMatchCandidate {
            override val id: UUID = UUID.randomUUID()
            override val type: String = "Policy"
            override val schemaVersion: String = "1.0.0"
            override val annotations = LazyJsonMap.annotations("""{"app":"app-00001"}""")
            override val payload = LazyJsonMap.payload("""{"name":"n"}""")
            override fun toDomain() = error("n/a")
        }
        assertThat(matcher.matches(candidate)).isTrue()
    }

    @Test
    fun shouldEvaluateDotAccessOnHashMap() {
        val matcher = BoMObjExprMatcher("type == 'Policy' && a.app == 'app-00001'")
        val candidate = BoMEntityDomainCandidate(
            BoMEntity(
                id = UUID.randomUUID(),
                type = "Policy",
                schemaVersion = "1.0.0",
                payload = mutableMapOf(),
                annotations = mutableMapOf("app" to "app-00001"),
            ),
        )
        assertThat(matcher.matches(candidate)).isTrue()
    }
}
