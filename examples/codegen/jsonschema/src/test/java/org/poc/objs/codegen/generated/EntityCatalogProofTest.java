package org.poc.objs.codegen.generated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.poc.objs.api.domain.Entity;
import org.poc.objs.api.domain.IdentityProjection;
import org.poc.objs.api.domain.SchemaDsl;
import org.poc.objs.api.domain.SchemaNode;
import org.poc.objs.api.typed.EntityTypeMeta;
import org.poc.objs.api.typed.PayloadMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves write-side EntityCatalog / EdgeCatalog and generic GraphMutationBuilder.add
 * without a per-type switch (G-W7).
 */
class EntityCatalogProofTest {

    private final PayloadMapper mapper = new PayloadMapper(JsonMapper.builder().build());

    @Test
    void shouldConvertProductViaCatalog_withoutPerTypeSwitch() {
        Object payload = new Product().withName("app").withVersion("1.0.0");

        assertThat(EntityCatalog.supports(payload.getClass())).isTrue();
        EntityTypeMeta meta = EntityCatalog.meta(payload);
        assertThat(meta.getType()).isEqualTo("Product");
        assertThat(meta.getSchemaVersion()).isEqualTo("1.0.0");

        UUID id = UUID.randomUUID();
        Entity entity = EntityCatalog.toEntity(id, payload, mapper);
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getType()).isEqualTo("Product");
        assertThat(entity.getPayload()).containsEntry("name", "app");

        GeneratedNode<?> node = EntityCatalog.toNode(id, payload);
        assertThat(node).isInstanceOf(ProductNode.class);
        assertThat(node.id()).isEqualTo(id);

        GeneratedNode<?> fromEntity = EntityCatalog.fromEntity(entity, mapper);
        assertThat(fromEntity.id()).isEqualTo(id);
        assertThat(fromEntity.getPayload()).isInstanceOf(Product.class);

        Map<String, Object> map = EntityCatalog.toMap(payload, mapper);
        Product roundTrip = EntityCatalog.fromMap(map, Product.class, mapper);
        assertThat(roundTrip.getName()).isEqualTo("app");
        assertThat(roundTrip.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldProjectIdentityFromCatalogMap_withoutPerTypeSwitch() {
        Object payload = new Product().withName("app").withVersion("1.0.0");
        Map<String, Object> map = EntityCatalog.toMap(payload, mapper);

        SchemaNode contentSchema = SchemaDsl.INSTANCE.obj(
                "Product",
                "Product payload",
                List.of(
                        SchemaDsl.INSTANCE.field(
                                "name",
                                SchemaDsl.INSTANCE.string("Name", "Name", null, null),
                                true,
                                true,
                                false,
                                null,
                                List.of(),
                                Map.of()),
                        SchemaDsl.INSTANCE.field(
                                "version",
                                SchemaDsl.INSTANCE.string("Version", "Version", null, null),
                                true,
                                true,
                                false,
                                null,
                                List.of(),
                                Map.of())));

        Map<String, Object> identity = IdentityProjection.INSTANCE.project(contentSchema, map);
        assertThat(identity).containsEntry("name", "app").containsEntry("version", "1.0.0");
    }

    @Test
    void shouldRegisterViaGenericAdd_matchingTypedAddProduct() {
        Product payload = new Product().withName("app").withVersion("1.0.0");
        UUID id = UUID.randomUUID();

        GraphMutationBuilder typed = new GraphMutationBuilder(mapper);
        typed.addProduct(id, payload);
        GraphMutationBuilder generic = new GraphMutationBuilder(mapper);
        GeneratedNode<?> node = generic.add(id, (Object) payload);

        assertThat(node).isInstanceOf(ProductNode.class);
        assertThat(generic.build().getEntities().getSet())
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(typed.build().getEntities().getSet());

        GraphMutationBuilder fromNode = new GraphMutationBuilder(mapper);
        fromNode.add(node);
        assertThat(fromNode.build().getEntities().getSet()).hasSize(1);
        assertThat(fromNode.build().getEntities().getSet().get(0).getId()).isEqualTo(id);
    }

    @Test
    void shouldFailClosedOnUnknownPayloadClass() {
        assertThatThrownBy(() -> EntityCatalog.meta(String.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity payload class");
        assertThatThrownBy(() -> new GraphMutationBuilder(mapper).add("not-a-payload"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldEmitEmptyEdgeCatalog_whenNoEdgePropertiesGenerated() {
        assertThat(EdgeCatalog.supports(Product.class)).isFalse();
        assertThatThrownBy(() -> EdgeCatalog.meta(Product.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown edge payload class");
    }
}
