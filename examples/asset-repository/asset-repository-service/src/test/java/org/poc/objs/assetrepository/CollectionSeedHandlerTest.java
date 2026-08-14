package org.poc.objs.assetrepository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.poc.objs.assetrepository.service.CollectionService;
import org.poc.objs.assetrepository.service.ObjectWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ar_collection_seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "objs.seeds.enabled=true",
        "objs.seeds.resources[0]=classpath:seeds/asset-repository-ontology.yaml",
        "objs.seeds.resources[1]=classpath:seeds/asset-repository-demo-data.yaml",
        "objs.seeds.on-failure=FAIL_FAST"
})
class CollectionSeedHandlerTest {

    @Autowired
    CollectionService collections;

    @Autowired
    ObjectWriteService objects;

    @Test
    void shouldSeedCollectionsAndObjectsFromYaml() {
        assertThat(collections.list(null, null, null))
                .extracting(c -> c.getName())
                .containsExactlyInAnyOrder(
                        "datasets",
                        "models",
                        "agents",
                        "composables",
                        "mcp-servers",
                        "customer-support");
        var datasets = collections.findByName("datasets").orElseThrow();
        assertThat(objects.listObjects(datasets.getId())).hasSize(50);
        assertThat(objects.listObjects(collections.findByName("models").orElseThrow().getId())).hasSize(20);
        assertThat(objects.listObjects(collections.findByName("agents").orElseThrow().getId())).hasSize(100);
        assertThat(objects.listObjects(collections.findByName("composables").orElseThrow().getId())).hasSize(200);
        assertThat(objects.listObjects(collections.findByName("mcp-servers").orElseThrow().getId())).hasSizeGreaterThanOrEqualTo(100);
        assertThat(objects.listObjects(collections.findByName("customer-support").orElseThrow().getId())).hasSizeGreaterThan(100);
    }
}
