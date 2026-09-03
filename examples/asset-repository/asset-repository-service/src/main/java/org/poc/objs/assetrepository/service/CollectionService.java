package org.poc.objs.assetrepository.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.domain.CollectionRepository;
import org.poc.objs.assetrepository.domain.CollectionTypeEntity;
import org.poc.objs.assetrepository.domain.CollectionTypeSpec;
import org.poc.objs.assetrepository.domain.ObjectWriteMode;
import org.poc.objs.api.domain.GraphSpec;
import org.poc.objs.core.persistence.GraphStore;
import org.poc.objs.core.persistence.NamedGraphStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionService {

    public static final String ANNO_COLLECTION = "collection";
    public static final String ANNO_OWNER = "owner";
    public static final String ANNO_COLLECTION_ID = "collectionId";

    private final CollectionRepository collections;
    private final NamedGraphStore graphs;
    private final GraphStore graphStore;

    public CollectionService(CollectionRepository collections, NamedGraphStore graphs, GraphStore graphStore) {
        this.collections = collections;
        this.graphs = graphs;
        this.graphStore = graphStore;
    }

    @Transactional
    public CollectionEntity create(
            String name,
            String description,
            String owner,
            String ownerEmail,
            String supportEmail,
            String sla,
            ObjectWriteMode writeMode,
            List<CollectionTypeSpec> acceptedTypes
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner is required");
        }
        if (acceptedTypes == null || acceptedTypes.isEmpty()) {
            throw new IllegalArgumentException("acceptedTypes must not be empty");
        }

        CollectionEntity entity = new CollectionEntity();
        entity.setName(name.trim());
        entity.setDescription(description);
        entity.setOwner(owner.trim());
        entity.setOwnerEmail(ownerEmail);
        entity.setSupportEmail(supportEmail);
        entity.setSla(sla);
        entity.setObjectWriteMode(writeMode != null ? writeMode : ObjectWriteMode.UUID_OR_IDENTIFIER);
        entity.replaceTypes(toTypeEntities(acceptedTypes));

        UUID graphId = UUID.randomUUID();
        Map<String, String> annotations = new HashMap<>();
        annotations.put(ANNO_COLLECTION, entity.getName());
        annotations.put(ANNO_OWNER, entity.getOwner());
        graphs.create(new GraphSpec(graphId, annotations, java.util.Set.of(), java.util.Set.of()));

        entity.setGraphId(graphId);
        CollectionEntity saved = collections.save(entity);

        annotations.put(ANNO_COLLECTION_ID, saved.getId().toString());
        graphs.updateAnnotations(graphId, annotations);
        return saved;
    }

    /**
     * Live collection copy: same pool object ids, new graph id, cloned collection metadata.
     */
    @Transactional
    public CollectionEntity copy(UUID id, String newName) {
        CollectionEntity source = require(id);
        String name = (newName == null || newName.isBlank())
                ? "Copy of " + source.getName()
                : newName.trim();
        CollectionEntity entity = new CollectionEntity();
        entity.setName(name);
        entity.setDescription(source.getDescription());
        entity.setOwner(source.getOwner());
        entity.setOwnerEmail(source.getOwnerEmail());
        entity.setSupportEmail(source.getSupportEmail());
        entity.setSla(source.getSla());
        entity.setObjectWriteMode(source.getObjectWriteMode());
        List<CollectionTypeSpec> typeSpecs = new ArrayList<>();
        for (CollectionTypeEntity type : source.getTypes()) {
            typeSpecs.add(new CollectionTypeSpec(type.getObjectType(), type.getMetadata()));
        }
        entity.replaceTypes(toTypeEntities(typeSpecs));

        Map<String, String> annotations = new HashMap<>();
        annotations.put(ANNO_COLLECTION, entity.getName());
        annotations.put(ANNO_OWNER, entity.getOwner());
        var copied = graphs.copyGraph(source.getGraphId(), annotations);
        entity.setGraphId(copied.getId());
        CollectionEntity saved = collections.save(entity);

        annotations.put(ANNO_COLLECTION_ID, saved.getId().toString());
        graphs.updateAnnotations(copied.getId(), annotations);
        return saved;
    }

    @Transactional(readOnly = true)
    public CollectionEntity require(UUID id) {
        return collections.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Collection not found: " + id));
    }

    @Transactional(readOnly = true)
    public CollectionEntity requireByName(String name) {
        return collections.findByName(name)
                .orElseThrow(() -> new NoSuchElementException("Collection not found: " + name));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CollectionEntity> findByName(String name) {
        return collections.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<CollectionEntity> list(String nameContains, String owner, String acceptedType) {
        List<CollectionEntity> rows = collections.search(
                blankToNull(nameContains),
                blankToNull(owner));
        if (acceptedType == null || acceptedType.isBlank()) {
            return rows;
        }
        String needle = acceptedType.trim();
        return rows.stream().filter(c -> c.acceptedTypes().contains(needle)).toList();
    }

    @Transactional
    public CollectionEntity updateMetadata(
            UUID id,
            String name,
            String description,
            String owner,
            String ownerEmail,
            String supportEmail,
            String sla,
            ObjectWriteMode writeMode,
            List<CollectionTypeSpec> acceptedTypes
    ) {
        CollectionEntity entity = require(id);
        if (name != null && !name.isBlank()) {
            entity.setName(name.trim());
        }
        if (description != null) {
            entity.setDescription(description);
        }
        if (owner != null && !owner.isBlank()) {
            entity.setOwner(owner.trim());
        }
        if (ownerEmail != null) {
            entity.setOwnerEmail(ownerEmail);
        }
        if (supportEmail != null) {
            entity.setSupportEmail(supportEmail);
        }
        if (sla != null) {
            entity.setSla(sla);
        }
        if (writeMode != null) {
            entity.setObjectWriteMode(writeMode);
        }
        if (acceptedTypes != null && !acceptedTypes.isEmpty()) {
            entity.replaceTypes(toTypeEntities(acceptedTypes));
        }
        CollectionEntity saved = collections.save(entity);
        Map<String, String> annotations = new HashMap<>();
        annotations.put(ANNO_COLLECTION, saved.getName());
        annotations.put(ANNO_OWNER, saved.getOwner());
        annotations.put(ANNO_COLLECTION_ID, saved.getId().toString());
        graphs.updateAnnotations(saved.getGraphId(), annotations);
        return saved;
    }

    @Transactional(readOnly = true)
    public int objectCount(CollectionEntity entity) {
        return graphStore.countByType(entity.getGraphId()).values().stream()
                .mapToInt(Long::intValue)
                .sum();
    }

    public void assertAcceptedType(CollectionEntity collection, String type) {
        if (!collection.acceptedTypes().contains(type)) {
            throw new IllegalArgumentException(
                    "Type '" + type + "' is not accepted by collection " + collection.getId());
        }
    }

    private static List<CollectionTypeEntity> toTypeEntities(List<CollectionTypeSpec> specs) {
        List<CollectionTypeEntity> rows = new ArrayList<>();
        for (CollectionTypeSpec spec : specs) {
            rows.add(new CollectionTypeEntity(spec.objectType(), spec.metadata()));
        }
        return rows;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
