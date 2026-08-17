package org.poc.objs.assetrepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Launchable asset-repository example: domain API + objs-service (workbench at {@code /workbench/})
 * + domain SPA at {@code /ar/}.
 *
 * <p>Core JPA packages come from {@code ObjsCoreAutoConfiguration}; domain entities from
 * {@link AssetRepositoryJpaConfig}.
 */
@SpringBootApplication(scanBasePackages = "org.poc.objs")
public class AssetRepositoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetRepositoryApplication.class, args);
    }
}
