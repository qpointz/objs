package io.qpointz.poc.objs.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjsServiceAutoConfigurationTest {

    @Test
    void shouldInstantiateAutoConfiguration() {
        assertThat(new ObjsServiceAutoConfiguration()).isNotNull();
    }
}
