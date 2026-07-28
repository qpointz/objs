package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.validation.BoMValidator
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
    fun bomSchemaCatalog(): BoMSchemaCatalog = BoMSchemaCatalog()

    @Bean
    fun bomAllowedEdgeCatalog(): BoMAllowedEdgeCatalog = BoMAllowedEdgeCatalog()

    @Bean
    fun bomValidator(
        schemas: BoMSchemaCatalog,
        allowedEdges: BoMAllowedEdgeCatalog,
    ): BoMValidator = BoMValidator(schemas, allowedEdges)
}
