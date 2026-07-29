package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.typed.PayloadMapper

class LazyJsonMapTest {

    @Test
    fun shouldParseOnlyOnFirstAccess() {
        val map = LazyJsonMap.annotations("""{"app":"payments","appVersion":"1.0.0"}""")
        assertThat(map.wasParsed).isFalse()
        assertThat(map["app"]).isEqualTo("payments")
        assertThat(map.wasParsed).isTrue()
        assertThat(map.parseInvocations).isEqualTo(1)
        assertThat(map["appVersion"]).isEqualTo("1.0.0")
        assertThat(map.parseInvocations).isEqualTo(1)
    }

    @Test
    fun shouldAllowMutationAfterParse() {
        val map = LazyJsonMap.payload("""{"name":"Alice"}""")
        map["name"] = "Bob"
        map["age"] = 30
        assertThat(map).containsEntry("name", "Bob")
        assertThat(map).containsEntry("age", 30)
    }

    @Test
    fun shouldSerializeLazyMapsLikeOrdinaryMaps() {
        val map = LazyJsonMap.annotations("""{"env":"test"}""")
        val json = PayloadMapper.mapper.writeValueAsString(map)
        assertThat(json).isEqualTo("""{"env":"test"}""")
    }

    @Test
    fun shouldTreatNullPropertiesAsAbsent() {
        assertThat(LazyJsonMap.properties(null)).isNull()
    }
}
