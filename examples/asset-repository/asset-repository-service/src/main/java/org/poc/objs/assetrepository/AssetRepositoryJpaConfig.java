package org.poc.objs.assetrepository;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "org.poc.objs.assetrepository.domain")
@EnableJpaRepositories(basePackages = "org.poc.objs.assetrepository.domain")
public class AssetRepositoryJpaConfig {
}
