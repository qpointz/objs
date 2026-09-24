package org.poc.objs.policy.service.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ObjsPolicyOpenApiConfigurationTest {

    @Test
    fun shouldExposeSeparatePolicyGroup() {
        val policy = ObjsPolicyOpenApiConfiguration().objsPolicyApi()
        assertThat(policy.group).isEqualTo("policy")
        assertThat(policy.pathsToMatch.toList()).contains(
            "/api/v1/objs/policy",
            "/api/v1/objs/policy/**",
        )
    }
}
