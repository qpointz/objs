package org.poc.objs.sbom.assessment

import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.GraphFragmentPolicy
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Assessment needs [GraphFragmentPolicy] at construction time.
 * `:objs-policy-service` also exposes one via autoconfig; keep a local fallback so
 * inventory starts even when that sidecar bean is not yet (or not) on the context.
 */
@Configuration
class SbomAssessmentConfiguration {
    @Bean
    @ConditionalOnMissingBean(GraphFragmentPolicy::class)
    fun graphFragmentPolicy(): GraphFragmentPolicy = DefaultGraphFragmentPolicy
}
