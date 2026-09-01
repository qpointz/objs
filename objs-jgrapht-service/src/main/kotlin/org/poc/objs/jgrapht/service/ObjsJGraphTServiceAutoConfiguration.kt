package org.poc.objs.jgrapht.service

import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.GraphFragmentPolicy
import org.poc.objs.jgrapht.core.analysis.DirectedCycleRegionAnalyzer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

/** Autoconfiguration for graph algorithm REST endpoints. */
@AutoConfiguration
@ComponentScan(basePackages = ["org.poc.objs.jgrapht.service"])
class ObjsJGraphTServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun graphFragmentPolicy(): GraphFragmentPolicy = DefaultGraphFragmentPolicy

    @Bean
    @ConditionalOnMissingBean
    fun directedCycleRegionAnalyzer(): DirectedCycleRegionAnalyzer = DirectedCycleRegionAnalyzer()
}
