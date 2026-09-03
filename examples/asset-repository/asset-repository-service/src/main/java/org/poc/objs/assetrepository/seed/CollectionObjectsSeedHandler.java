package org.poc.objs.assetrepository.seed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.service.CollectionService;
import org.poc.objs.assetrepository.service.ObjectWriteService;
import org.poc.objs.assetrepository.web.dto.ApiDtos;
import org.poc.objs.api.seed.ParsedSeedDocument;
import org.poc.objs.api.seed.SeedDocumentHandler;
import org.poc.objs.api.seed.SeedDocumentParseException;
import org.poc.objs.api.seed.SeedDocumentResult;
import org.poc.objs.api.seed.SeedRawDocument;
import org.springframework.stereotype.Component;

@Component
public class CollectionObjectsSeedHandler implements SeedDocumentHandler {

    public static final String KIND = "CollectionObjects";
    private static final String VERSION = "1.0.0";

    private final CollectionService collections;
    private final ObjectWriteService objects;

    public CollectionObjectsSeedHandler(CollectionService collections, ObjectWriteService objects) {
        this.collections = collections;
        this.objects = objects;
    }

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public int getApplyOrder() {
        return 40;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ParsedSeedDocument parse(SeedRawDocument document) {
        Map<String, Object> raw = document.getRaw();
        Object collection = raw.get("collection");
        if (collection == null || collection.toString().isBlank()) {
            throw new SeedDocumentParseException(document.getIndex(), "collection must be a non-blank string");
        }
        Object objectsRaw = raw.get("objects");
        if (!(objectsRaw instanceof List<?> objectRows) || objectRows.isEmpty()) {
            throw new SeedDocumentParseException(document.getIndex(), "objects must be a non-empty list");
        }
        List<Map<String, Object>> objects = new ArrayList<>();
        for (int i = 0; i < objectRows.size(); i++) {
            Object row = objectRows.get(i);
            if (!(row instanceof Map<?, ?> map)) {
                throw new SeedDocumentParseException(document.getIndex(), "objects[" + i + "] must be an object");
            }
            objects.add((Map<String, Object>) map);
        }
        List<Map<String, Object>> relations = new ArrayList<>();
        Object relRaw = raw.get("relations");
        if (relRaw instanceof List<?> relRows) {
            for (int i = 0; i < relRows.size(); i++) {
                Object row = relRows.get(i);
                if (!(row instanceof Map<?, ?> map)) {
                    throw new SeedDocumentParseException(document.getIndex(), "relations[" + i + "] must be an object");
                }
                relations.add((Map<String, Object>) map);
            }
        }
        CollectionObjectsSeed payload = new CollectionObjectsSeed(
                collection.toString().trim(),
                objects,
                relations);
        return new ParsedSeedDocument(document, payload.collection(), payload);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SeedDocumentResult apply(ParsedSeedDocument parsed) {
        CollectionObjectsSeed seed = (CollectionObjectsSeed) parsed.getPayload();
        CollectionEntity collection = collections.requireByName(seed.collection());

        Map<String, String> keyToObjN = new LinkedHashMap<>();
        List<ApiDtos.WriteObjectRequest> writes = new ArrayList<>();
        int i = 0;
        for (Map<String, Object> row : seed.objects()) {
            Object keyRaw = row.get("key");
            String key = keyRaw != null ? keyRaw.toString() : ("obj-" + i);
            keyToObjN.put(key, "obj-" + i);
            Object type = row.get("type");
            if (type == null || type.toString().isBlank()) {
                throw new IllegalStateException("objects[" + i + "].type is required");
            }
            Object version = row.get("schemaVersion");
            String schemaVersion = version == null || version.toString().isBlank()
                    ? VERSION
                    : version.toString();
            Map<String, Object> payload = (Map<String, Object>) row.get("payload");
            writes.add(new ApiDtos.WriteObjectRequest(null, type.toString(), schemaVersion, payload));
            i++;
        }

        List<ApiDtos.RelationInput> rels = new ArrayList<>();
        for (Map<String, Object> rel : seed.relations()) {
            String source = remap(keyToObjN, rel.get("source"));
            String target = remap(keyToObjN, rel.get("target"));
            Object role = rel.get("role");
            if (role == null || role.toString().isBlank()) {
                throw new IllegalStateException("relation role is required");
            }
            rels.add(new ApiDtos.RelationInput(source, role.toString(), target));
        }

        objects.writeComposition(collection.getId(), new ApiDtos.CompositionRequest(writes, rels));
        return new SeedDocumentResult(
                parsed.getDocument().getIndex(),
                KIND,
                parsed.getDocument().getApiVersion(),
                parsed.getIdentity(),
                true,
                false,
                List.of(),
                List.of());
    }

    private static String remap(Map<String, String> keyToObjN, Object key) {
        if (key == null) {
            throw new IllegalStateException("relation source/target is required");
        }
        String mapped = keyToObjN.get(key.toString());
        if (mapped == null) {
            throw new IllegalStateException("Unknown relation key: " + key);
        }
        return mapped;
    }

    public record CollectionObjectsSeed(
            String collection,
            List<Map<String, Object>> objects,
            List<Map<String, Object>> relations
    ) {
    }
}
