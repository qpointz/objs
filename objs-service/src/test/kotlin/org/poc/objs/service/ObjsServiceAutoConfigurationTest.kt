package org.poc.objs.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ObjsServiceAutoConfigurationTest {
    @Test
    fun shouldLoadAutoConfigurationClass() {
        assertThat(ObjsServiceAutoConfiguration::class.java).isNotNull()
    }
}
