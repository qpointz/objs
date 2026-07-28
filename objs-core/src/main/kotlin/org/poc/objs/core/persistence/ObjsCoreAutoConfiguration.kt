package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoAllowedEdgeCatalog
import org.poc.objs.core.domain.BoSchemaCatalog
import org.poc.objs.core.validation.BoValidator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Autoconfiguration for objs-core persistence and validation beans.
 * Catalogs are mutable singletons for the process (in-memory G-6/G-7).
 */
@AutoConfiguration
@ComponentScan(basePackages = ["org.poc.objs.core"])
@EntityScan(basePackages = ["org.poc.objs.core.persistence"])
@EnableJpaRepositories(basePackages = ["org.poc.objs.core.persistence"])
class ObjsCoreAutoConfiguration {

    @Bean
    fun boSchemaCatalog(): BoSchemaCatalog = BoSchemaCatalog()

    @Bean
    fun boAllowedEdgeCatalog(): BoAllowedEdgeCatalog = BoAllowedEdgeCatalog()

    @Bean
    fun boValidator(
        schemas: BoSchemaCatalog,
        allowedEdges: BoAllowedEdgeCatalog,
    ): BoValidator = BoValidator(schemas, allowedEdges)
}
