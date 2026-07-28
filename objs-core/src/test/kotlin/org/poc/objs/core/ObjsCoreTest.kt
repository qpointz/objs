package org.poc.objs.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ObjsCoreTest {
    @Test
    fun shouldExposeModuleName() {
        assertThat(ObjsCore.MODULE).isEqualTo("objs-core")
    }
}
