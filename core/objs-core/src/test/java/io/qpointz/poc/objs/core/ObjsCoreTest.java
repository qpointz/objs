package io.qpointz.poc.objs.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjsCoreTest {

    @Test
    void shouldExposeModuleName() {
        assertThat(ObjsCore.MODULE).isEqualTo("objs-core");
    }
}
