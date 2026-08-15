package org.poc.objs.sbom.persistence

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(basePackages = ["org.poc.objs.sbom.persistence"])
@EnableJpaRepositories(basePackages = ["org.poc.objs.sbom.persistence"])
class SbomPersistenceConfiguration
