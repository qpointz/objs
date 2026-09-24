package org.poc.objs.service.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ObjsOpenApiConfigurationTest {

    @Test
    fun shouldIncludeEntitiesAndEdgesInGraphGroup() {
        val graph = ObjsOpenApiConfiguration().objsGraphApi()
        assertThat(graph.group).isEqualTo("graph")
        assertThat(graph.pathsToMatch.toList()).contains(
            "/api/v1/objs/graphs/**",
            "/api/v1/objs/entities/**",
            "/api/v1/objs/edges/**",
            "/api/v1/objs/graph/**",
            "/api/v1/objs/graph/algorithms/**",
            "/api/v1/objs/status",
        )
        assertThat(graph.packagesToScan.toList()).contains(
            "org.poc.objs.jgrapht.service.web",
            "org.poc.objs.gremlin.service.web",
        )
    }

    @Test
    fun shouldKeepRegistryGroupSeparate() {
        val registry = ObjsOpenApiConfiguration().objsRegistryApi()
        assertThat(registry.group).isEqualTo("registry")
        assertThat(registry.pathsToMatch.toList()).containsExactly("/api/v1/objs/registry/**")
    }
}
