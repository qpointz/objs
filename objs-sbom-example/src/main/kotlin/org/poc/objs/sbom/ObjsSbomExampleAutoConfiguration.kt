package org.poc.objs.sbom

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.sbom.registry.SbomRegistry
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@AutoConfiguration
@ComponentScan(basePackageClasses = [ObjsSbomExample::class])
class ObjsSbomExampleAutoConfiguration {
    @Bean
    fun sbomRegistrySeed(
        schemas: BoMSchemaCatalog,
        edges: BoMAllowedEdgeCatalog,
    ): ApplicationRunner = ApplicationRunner {
        SbomRegistry.pack().registerInto(schemas, edges)
    }
}
