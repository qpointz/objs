package org.poc.objs.assetrepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.domain.CollectionTypeSpec;
import org.poc.objs.assetrepository.domain.ObjectWriteMode;
import org.poc.objs.assetrepository.service.CollectionService;
import org.poc.objs.assetrepository.service.ObjectWriteService;
import org.poc.objs.assetrepository.web.dto.ApiDtos;
import org.poc.objs.core.domain.SchemaCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ar_wi003;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "objs.seeds.enabled=true",
        "objs.seeds.resources[0]=classpath:seeds/asset-repository-ontology.yaml",
        "objs.seeds.on-failure=FAIL_FAST"
})
class CollectionServiceTest {

    @Autowired
    CollectionService collections;

    @Autowired
    ObjectWriteService objects;

    @Autowired
    SchemaCatalog schemas;

    @Test
    void shouldCreateCollection_withTypeRowsAndAcceptedGate() {
        CollectionEntity created = collections.create(
                "model-catalog",
                "Models and datasets",
                "platform-data",
                "data@example.com",
                "support@example.com",
                "best effort",
                ObjectWriteMode.UUID_OR_IDENTIFIER,
                List.of(
                        new CollectionTypeSpec("LlmModel", "{\"source\":\"cmdb\"}"),
                        CollectionTypeSpec.of("Dataset")));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getGraphId()).isNotNull();
        assertThat(created.acceptedTypes()).containsExactly("LlmModel", "Dataset");
        assertThat(created.getTypes()).hasSize(2);
        assertThat(created.getTypes().get(0).getMetadata()).isEqualTo("{\"source\":\"cmdb\"}");
        assertThat(schemas.get("LlmModel", "1.0.0")).isNotNull();
        assertThat(schemas.get("Dataset", "1.0.0")).isNotNull();

        collections.assertAcceptedType(created, "LlmModel");
        assertThatThrownBy(() -> collections.assertAcceptedType(created, "AiAgent"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUpdateCollection_reusingExistingAcceptedTypes() {
        CollectionEntity created = collections.create(
                "tools-catalog",
                null,
                "platform",
                null,
                null,
                null,
                ObjectWriteMode.UUID_OR_IDENTIFIER,
                List.of(CollectionTypeSpec.of("Tool"), CollectionTypeSpec.of("Skill")));

        CollectionEntity updated = collections.updateMetadata(
                created.getId(),
                "tools-catalog",
                "updated",
                "platform",
                null,
                null,
                null,
                null,
                List.of(CollectionTypeSpec.of("Tool"), CollectionTypeSpec.of("Prompt")));

        assertThat(updated.getDescription()).isEqualTo("updated");
        assertThat(updated.acceptedTypes()).containsExactly("Tool", "Prompt");
        UUID toolId = created.getTypes().stream()
                .filter(t -> "Tool".equals(t.getObjectType()))
                .findFirst()
                .orElseThrow()
                .getId();
        assertThat(updated.getTypes().stream()
                .filter(t -> "Tool".equals(t.getObjectType()))
                .findFirst()
                .orElseThrow()
                .getId()).isEqualTo(toolId);
    }

    @Test
    void shouldListCollections_whenNameAndOwnerFiltersOmitted() {
        collections.create(
                "list-unfiltered",
                null,
                "ops",
                null,
                null,
                null,
                ObjectWriteMode.UUID_OR_IDENTIFIER,
                List.of(CollectionTypeSpec.of("Dataset")));

        List<CollectionEntity> rows = collections.list(null, null, null);
        assertThat(rows).extracting(CollectionEntity::getName).contains("list-unfiltered");
        assertThat(collections.list("list-un", "ops", null))
                .extracting(CollectionEntity::getName)
                .containsExactly("list-unfiltered");
    }

    @Test
    void shouldCopyCollection_sharingObjectIdsAndNewGraphId() {
        CollectionEntity source = collections.create(
                "copy-source",
                "src",
                "ops",
                null,
                null,
                null,
                ObjectWriteMode.UUID_OR_IDENTIFIER,
                List.of(CollectionTypeSpec.of("Dataset")));
        ApiDtos.ObjectDto written = objects.writeObject(
                source.getId(),
                new ApiDtos.WriteObjectRequest(
                        null,
                        "Dataset",
                        "1.0.0",
                        Map.of(
                                "datasetId", "DS-COPY-SHARED",
                                "name", "shared-dataset",
                                "purpose", "Training",
                                "classification", "Public")));

        CollectionEntity copy = collections.copy(source.getId(), null);

        assertThat(copy.getId()).isNotEqualTo(source.getId());
        assertThat(copy.getGraphId()).isNotEqualTo(source.getGraphId());
        assertThat(copy.getName()).isEqualTo("Copy of copy-source");
        assertThat(copy.getOwner()).isEqualTo("ops");
        assertThat(copy.acceptedTypes()).containsExactly("Dataset");
        List<ApiDtos.ObjectDto> copiedObjects = objects.listObjects(copy.getId());
        assertThat(copiedObjects).extracting(ApiDtos.ObjectDto::id).containsExactly(written.id());
        assertThat(objects.listObjects(source.getId())).extracting(ApiDtos.ObjectDto::id).containsExactly(written.id());
    }
}
