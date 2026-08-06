package org.poc.objs.gremlin.service

import org.poc.objs.gremlin.core.BoMGremlinEngine
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

/**
 * Autoconfiguration for Gremlin traverse REST.
 */
@AutoConfiguration
@ComponentScan(basePackages = ["org.poc.objs.gremlin.service"])
class ObjsGremlinServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun bomGremlinEngine(): BoMGremlinEngine = BoMGremlinEngine()
}
