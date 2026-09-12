package org.poc.objs.assetrepository.seed;

import java.time.Instant;
import java.util.Map;
import org.poc.objs.assetrepository.domain.CollectionRepository;
import org.poc.objs.core.persistence.NamedGraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * C-36: seed a few backdated deep versions on the first demo collection graph.
 */
@Component
@Profile("demo")
@Order(2000)
public class AssetRepositoryBackdatedVersionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AssetRepositoryBackdatedVersionSeeder.class);

    private final CollectionRepository collections;
    private final NamedGraphStore namedGraphs;

    public AssetRepositoryBackdatedVersionSeeder(
            CollectionRepository collections,
            NamedGraphStore namedGraphs
    ) {
        this.collections = collections;
        this.namedGraphs = namedGraphs;
    }

    @Override
    public void run(ApplicationArguments args) {
        var first = collections.findAll().stream().findFirst();
        if (first.isEmpty()) {
            return;
        }
        var graphId = first.get().getGraphId();
        if (graphId == null) {
            return;
        }
        if (!namedGraphs.listGraphVersions(graphId).isEmpty()) {
            return;
        }
        namedGraphs.createDeepGraphVersion(
                graphId,
                Map.of("kind", "demo-backdate", "era", "2020"),
                Instant.parse("2020-03-01T12:00:00Z")
        );
        namedGraphs.createDeepGraphVersion(
                graphId,
                Map.of("kind", "demo-backdate", "era", "2021"),
                Instant.parse("2021-09-01T12:00:00Z")
        );
        namedGraphs.createDeepGraphVersion(
                graphId,
                Map.of("kind", "demo-backdate", "era", "2022"),
                Instant.parse("2022-12-01T12:00:00Z")
        );
        log.info("Seeded backdated graph versions on collection graph {}", graphId);
    }
}
