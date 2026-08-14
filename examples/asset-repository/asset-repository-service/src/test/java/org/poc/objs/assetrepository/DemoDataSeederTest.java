package org.poc.objs.assetrepository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.poc.objs.assetrepository.service.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ar_demo_data;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class DemoDataSeederTest {

    @Autowired
    CollectionService collections;

    @Test
    void shouldSeedDemoCollectionsFromYaml() {
        assertThat(collections.list(null, null, null))
                .extracting(c -> c.getName())
                .containsExactlyInAnyOrder(
                        "datasets",
                        "models",
                        "agents",
                        "composables",
                        "mcp-servers",
                        "customer-support");
    }
}
