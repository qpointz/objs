package org.poc.objs.codegen.draft07.generated;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Smoke-test jsonschema2pojo output from objs {@code format=json-schema-codegen&dialect=draft-07}
 * (definitions keyword + allOf singular refs).
 */
class GeneratedDraft07CatalogModelTest {

    @Test
    void shouldExposeDatabaseContainsDatasetRelation() {
        Dataset dataset = new Dataset().withName("orders").withDatasetType("table");
        Database database =
                new Database()
                        .withName("payments-db")
                        .withEngine("postgres")
                        .withContainsDataset(List.of(dataset));

        assertThat(database.getContainsDataset()).containsExactly(dataset);
        assertThat(database.getName()).isEqualTo("payments-db");
        assertThat(dataset.getName()).isEqualTo("orders");
    }

    @Test
    void shouldExposeProductPayloadAndOutboundRelations() {
        Product product = new Product().withName("app").withVersion("1.0.0");
        Api api = new Api().withName("orders-api").withProtocol("HTTPS");
        product.setCallsAPI(List.of(api));

        assertThat(product.getName()).isEqualTo("app");
        assertThat(product.getVersion()).isEqualTo("1.0.0");
        assertThat(product.getCallsAPI()).containsExactly(api);
    }

    @Test
    void shouldGenerateTypesWithSpacedSchemaTitlesAsPascalCase() {
        ContainerImage image = new ContainerImage().withName("payments").withTag("1.2.3");
        assertThat(image.getName()).isEqualTo("payments");
        assertThat(image.getTag()).isEqualTo("1.2.3");
    }

    @Test
    void shouldGenerateEdgePropertySchema() {
        CanonicalEdge edge = new CanonicalEdge().withSource("seed").withConfidence(0.9);
        assertThat(edge.getSource()).isEqualTo("seed");
        assertThat(edge.getConfidence()).isEqualTo(0.9);
    }
}
