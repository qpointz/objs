package org.poc.objs.assetrepository.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.domain.ObjectWriteMode;
import org.poc.objs.assetrepository.spi.EventExtension;
import org.poc.objs.assetrepository.spi.PreprocessingExtension;
import org.poc.objs.assetrepository.spi.WriteBatch;
import org.poc.objs.assetrepository.web.dto.ApiDtos;
import org.poc.objs.core.domain.BoMEdge;
import org.poc.objs.core.domain.BoMEntity;
import org.poc.objs.core.domain.BoMGraphContents;
import org.poc.objs.core.domain.BoMGraphDelete;
import org.poc.objs.core.domain.BoMGraphMutation;
import org.poc.objs.core.domain.BoMGraphUpsert;
import org.poc.objs.core.domain.BoMIdentityProjection;
import org.poc.objs.core.domain.BoMSchema;
import org.poc.objs.core.domain.BoMSchemaCatalog;
import org.poc.objs.core.match.BoMMatcher;
import org.poc.objs.core.match.BoMMatcherDsl;
import org.poc.objs.core.match.BoMMatcherFormat;
import org.poc.objs.core.persistence.BoMGraphStore;
import org.poc.objs.core.persistence.BoMNamedGraphStore;
import org.poc.objs.core.validation.BoMValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjectWriteService {

    private final CollectionService collections;
    private final BoMNamedGraphStore namedGraphs;
    private final BoMGraphStore graphStore;
    private final BoMSchemaCatalog schemas;
    private final List<PreprocessingExtension> preprocessors;
    private final List<EventExtension> eventExtensions;

    public ObjectWriteService(
            CollectionService collections,
            BoMNamedGraphStore namedGraphs,
            BoMGraphStore graphStore,
            BoMSchemaCatalog schemas,
            List<PreprocessingExtension> preprocessors,
            List<EventExtension> eventExtensions
    ) {
        this.collections = collections;
        this.namedGraphs = namedGraphs;
        this.graphStore = graphStore;
        this.schemas = schemas;
        this.preprocessors = preprocessors;
        this.eventExtensions = eventExtensions;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ObjectDto> listObjects(UUID collectionId) {
        CollectionEntity collection = collections.require(collectionId);
        return namedGraphs.get(collection.getGraphId()).getContents().getEntities().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiDtos.ObjectDto getObject(UUID collectionId, UUID objectId) {
        return findInCollection(collections.require(collectionId), objectId);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ObjectRelationDto> listRelations(UUID collectionId, UUID objectId) {
        CollectionEntity collection = collections.require(collectionId);
        findInCollection(collection, objectId);
        BoMGraphContents contents = namedGraphs.get(collection.getGraphId()).getContents();
        Map<UUID, BoMEntity> byId = new HashMap<>();
        for (BoMEntity entity : contents.getEntities()) {
            if (entity.getId() != null) {
                byId.put(entity.getId(), entity);
            }
        }
        List<ApiDtos.ObjectRelationDto> out = new ArrayList<>();
        for (BoMEdge edge : contents.getEdges()) {
            if (objectId.equals(edge.getSource())) {
                BoMEntity related = byId.get(edge.getTarget());
                if (related != null) {
                    out.add(new ApiDtos.ObjectRelationDto(edge.getId(), edge.getRole(), "OUTGOING", toDto(related)));
                }
            } else if (objectId.equals(edge.getTarget())) {
                BoMEntity related = byId.get(edge.getSource());
                if (related != null) {
                    out.add(new ApiDtos.ObjectRelationDto(edge.getId(), edge.getRole(), "INCOMING", toDto(related)));
                }
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ObjectDto> search(UUID collectionId, ApiDtos.SearchRequest request) {
        CollectionEntity collection = collections.require(collectionId);
        String expr = request != null && request.matcherExpr() != null && !request.matcherExpr().isBlank()
                ? request.matcherExpr().trim()
                : buildFilterExpr(request != null ? request.filters() : null);
        if (expr == null || expr.isBlank()) {
            return listObjects(collectionId);
        }
        BoMGraphContents contents =
                graphStore.selectInGraph(collection.getGraphId(), matcher(expr));
        return contents.getEntities().stream().map(this::toDto).toList();
    }

    @Transactional
    public ApiDtos.ObjectDto writeObject(UUID collectionId, ApiDtos.WriteObjectRequest request) {
        CollectionEntity collection = collections.require(collectionId);
        collections.assertAcceptedType(collection, request.type());

        WriteBatch batch = new WriteBatch();
        batch.getObjects().add(resolveOne(collection, request));
        batch = runPreprocess(collection, batch);
        persist(collection, batch);
        emitEvents(collection, batch);
        return findInCollection(collection, batch.getObjects().get(0).entity().getId());
    }

    @Transactional
    public List<ApiDtos.ObjectDto> writeComposition(UUID collectionId, ApiDtos.CompositionRequest request) {
        CollectionEntity collection = collections.require(collectionId);
        if (request.objects() == null || request.objects().isEmpty()) {
            throw new IllegalArgumentException("objects required");
        }

        WriteBatch batch = new WriteBatch();
        for (ApiDtos.WriteObjectRequest obj : request.objects()) {
            collections.assertAcceptedType(collection, obj.type());
            batch.getObjects().add(resolveOne(collection, obj));
        }
        batch = runPreprocess(collection, batch);

        Map<String, UUID> keyToId = new LinkedHashMap<>();
        for (int idx = 0; idx < batch.getObjects().size(); idx++) {
            BoMEntity entity = batch.getObjects().get(idx).entity();
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            keyToId.put("obj-" + idx, entity.getId());
            ApiDtos.WriteObjectRequest original = request.objects().get(idx);
            if (original.id() != null) {
                keyToId.put(original.id().toString(), entity.getId());
            }
        }

        if (request.relations() != null) {
            for (ApiDtos.RelationInput rel : request.relations()) {
                UUID source = resolveKey(keyToId, rel.sourceKey());
                UUID target = resolveKey(keyToId, rel.targetKey());
                batch.getEdges().add(new BoMEdge(null, null, source, target, rel.role(), null, null, null));
            }
        }

        persist(collection, batch);
        emitEvents(collection, batch);
        return batch.getObjects().stream().map(p -> toDto(p.entity())).toList();
    }

    @Transactional
    public void deleteObject(UUID collectionId, UUID objectId) {
        CollectionEntity collection = collections.require(collectionId);
        findInCollection(collection, objectId);

        WriteBatch batch = new WriteBatch();
        batch.getDeleteEntityIds().add(objectId);
        BoMEntity stub = newEntity(objectId, "Deleted", "0", new HashMap<>());
        batch.getObjects().add(new WriteBatch.PendingObject(stub, EventExtension.ObjectChange.Op.DELETE));
        batch = runPreprocess(collection, batch);
        persist(collection, batch);
        emitEvents(collection, batch);
    }

    private WriteBatch runPreprocess(CollectionEntity collection, WriteBatch batch) {
        WriteBatch current = batch;
        PreprocessingExtension.WriteContext ctx = new PreprocessingExtension.WriteContext(collection);
        for (PreprocessingExtension ext : preprocessors) {
            current = ext.preprocess(ctx, current);
        }
        return current;
    }

    private WriteBatch.PendingObject resolveOne(CollectionEntity collection, ApiDtos.WriteObjectRequest request) {
        if (request.type() == null || request.type().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        String version = request.schemaVersion() != null && !request.schemaVersion().isBlank()
                ? request.schemaVersion()
                : "1.0.0";
        Map<String, Object> payload =
                request.payload() != null ? new HashMap<>(request.payload()) : new HashMap<>();

        ObjectWriteMode mode = collection.getObjectWriteMode();
        UUID resolvedId = request.id();
        EventExtension.ObjectChange.Op op = EventExtension.ObjectChange.Op.CREATE;

        if (resolvedId != null) {
            try {
                ApiDtos.ObjectDto existing = findInCollection(collection, resolvedId);
                op = EventExtension.ObjectChange.Op.UPDATE;
                payload = mergePayload(existing.payload(), payload);
            } catch (java.util.NoSuchElementException missing) {
                // Client-supplied UUID for a new object (G-P3) — full payload required.
                op = EventExtension.ObjectChange.Op.CREATE;
            }
        } else if (mode != ObjectWriteMode.UUID) {
            UUID byIdentity = findByIdentity(collection, request.type(), version, payload);
            if (byIdentity != null) {
                resolvedId = byIdentity;
                op = EventExtension.ObjectChange.Op.UPDATE;
                payload = mergePayload(findInCollection(collection, resolvedId).payload(), payload);
            }
        } else {
            throw new IllegalArgumentException("object_write_mode UUID requires request.id");
        }

        return new WriteBatch.PendingObject(newEntity(resolvedId, request.type(), version, payload), op);
    }

    /** Top-level patch: omitted keys keep the stored value; provided keys overwrite. */
    private static Map<String, Object> mergePayload(Map<String, Object> existing, Map<String, Object> patch) {
        Map<String, Object> merged = existing != null ? new HashMap<>(existing) : new HashMap<>();
        if (patch != null) {
            merged.putAll(patch);
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private BoMEntity newEntity(UUID id, String type, String version, Map<String, Object> payload) {
        return new BoMEntity(
                id,
                type,
                version,
                (Map) new HashMap<>(payload),
                new HashMap<String, String>());
    }

    private UUID findByIdentity(
            CollectionEntity collection,
            String type,
            String version,
            Map<String, Object> payload
    ) {
        BoMSchema schema = schemas.get(type, version);
        if (schema == null) {
            throw new IllegalArgumentException("Unknown schema " + type + "@" + version);
        }
        Map<String, ?> wanted = BoMIdentityProjection.INSTANCE.project(schema.getContentSchema(), payload);
        if (wanted.isEmpty()) {
            return null;
        }
        String typeExpr = "type == '" + escape(type) + "'";
        BoMGraphContents contents = graphStore.selectInGraph(collection.getGraphId(), matcher(typeExpr));
        UUID match = null;
        for (BoMEntity entity : contents.getEntities()) {
            Map<String, ?> existing =
                    BoMIdentityProjection.INSTANCE.project(schema.getContentSchema(), entity.getPayload());
            if (existing.equals(wanted)) {
                if (match != null) {
                    throw new ConflictException("Multiple objects match identity fields for type " + type);
                }
                match = entity.getId();
            }
        }
        return match;
    }

    private void persist(CollectionEntity collection, WriteBatch batch) {
        BoMGraphUpsert upsert = new BoMGraphUpsert();
        for (WriteBatch.PendingObject pending : batch.getObjects()) {
            if (pending.op() != EventExtension.ObjectChange.Op.DELETE) {
                upsert.getEntities().add(pending.entity());
            }
        }
        upsert.getEdges().addAll(batch.getEdges());
        BoMGraphDelete delete = new BoMGraphDelete();
        delete.getEntities().addAll(batch.getDeleteEntityIds());
        delete.getEdges().addAll(batch.getDeleteEdgeIds());
        var result = namedGraphs.mutate(collection.getGraphId(), new BoMGraphMutation(upsert, delete));
        if (!result.isValid()) {
            throw new BoMValidationException("write", result);
        }
    }

    private void emitEvents(CollectionEntity collection, WriteBatch batch) {
        List<EventExtension.ObjectChange> changes = new ArrayList<>();
        for (WriteBatch.PendingObject pending : batch.getObjects()) {
            changes.add(new EventExtension.ObjectChange(
                    pending.entity().getId(),
                    pending.entity().getType(),
                    pending.op(),
                    pending.entity()));
        }
        EventExtension.CollectionChangeSet set =
                new EventExtension.CollectionChangeSet(collection, changes, List.copyOf(batch.getEdges()));
        for (EventExtension ext : eventExtensions) {
            ext.onChanges(set);
        }
    }

    private ApiDtos.ObjectDto findInCollection(CollectionEntity collection, UUID objectId) {
        return namedGraphs.get(collection.getGraphId()).getContents().getEntities().stream()
                .filter(e -> objectId.equals(e.getId()))
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new java.util.NoSuchElementException("Object not found: " + objectId));
    }

    private ApiDtos.ObjectDto toDto(BoMEntity entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (entity.getPayload() != null) {
            payload.putAll(entity.getPayload());
        }
        return new ApiDtos.ObjectDto(entity.getId(), entity.getType(), entity.getSchemaVersion(), payload);
    }

    private static BoMMatcher matcher(String objExpr) {
        String json = "{\"obj-expr\":\"" + objExpr.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        return new BoMMatcherDsl().decode(json, BoMMatcherFormat.JSON);
    }

    private static String buildFilterExpr(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        filters.forEach((k, v) -> {
            if (k == null || v == null) {
                return;
            }
            if (k.startsWith("p.") || k.startsWith("a.") || k.equals("type") || k.equals("id")) {
                parts.add(k + " == '" + escape(v) + "'");
            } else {
                parts.add("p." + k + " == '" + escape(v) + "'");
            }
        });
        return String.join(" && ", parts);
    }

    private static UUID resolveKey(Map<String, UUID> keyToId, String key) {
        UUID id = keyToId.get(key);
        if (id != null) {
            return id;
        }
        try {
            return UUID.fromString(key);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unknown relation key: " + key);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static final class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }
}
