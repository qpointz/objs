package org.poc.objs.codegen.generated;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.poc.objs.api.domain.Edge;
import org.poc.objs.api.domain.Graph;
import org.poc.objs.api.domain.GraphMutation;
import org.poc.objs.api.domain.MutationMode;
import org.poc.objs.api.typed.PayloadMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Smoke-test that jsonschema2pojo output from the objs full-catalog export carries
 * payload fields and outbound relation props.
 */
class GeneratedCatalogModelTest {

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

    @Test
    void shouldBuildAndNavigateTypedProductGraph() {
        PayloadMapper mapper = new PayloadMapper(JsonMapper.builder().build());
        GraphMutationBuilder mutations = new GraphMutationBuilder(mapper);
        ProductNode product = mutations.addProduct(new Product().withName("app"));
        ComponentNode component = mutations.addComponent(new Component().withName("library"));

        mutations.containsComponent(product, component);
        GraphMutation mutation = mutations.build();
        Graph graph = new Graph(
                new ArrayList<>(mutation.getEntities().getSet()),
                new ArrayList<>(mutation.getEdges().getSet()));

        GeneratedReadView view = GeneratedReadView.from(graph, mapper);
        assertThat(view.products()).hasSize(1);
        assertThat(view.products().get(0).getContainsComponents()).hasSize(1);
        assertThat(view.products().get(0).getContainsComponentEdges()).hasSize(1);
        Edge contains = mutation.getEdges().getSet().get(0);
        assertThat(contains.getSource()).isEqualTo(product.id());
        assertThat(contains.getTarget()).isEqualTo(component.id());
        assertThat(contains.getRole()).isEqualTo("CONTAINS");
        assertThat(contains.getProperties()).isNull();

        GraphMutation replacement = new GraphMutationBuilder(mapper, MutationMode.REPLACE).build();
        assertThat(replacement.getMode()).isEqualTo(MutationMode.REPLACE);
    }
}
