package org.poc.objs.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class ApiKotlinConsumerTest {
    @Test
    fun shouldCompileAgainstCallerSuppliedJacksonMapper() {
        val mapperType: Class<out ObjectMapper> = ObjectMapper::class.java

        assertThat(mapperType).isEqualTo(ObjectMapper::class.java)
    }
}
