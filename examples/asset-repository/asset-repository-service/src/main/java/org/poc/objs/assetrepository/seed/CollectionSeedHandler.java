package org.poc.objs.assetrepository.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.domain.CollectionTypeSpec;
import org.poc.objs.assetrepository.domain.ObjectWriteMode;
import org.poc.objs.assetrepository.service.CollectionService;
import org.poc.objs.api.seed.ParsedSeedDocument;
import org.poc.objs.api.seed.SeedDocumentHandler;
import org.poc.objs.api.seed.SeedDocumentParseException;
import org.poc.objs.api.seed.SeedDocumentResult;
import org.poc.objs.api.seed.SeedRawDocument;
import org.springframework.stereotype.Component;

@Component
public class CollectionSeedHandler implements SeedDocumentHandler {

    public static final String KIND = "Collection";

    private final CollectionService collections;

    public CollectionSeedHandler(CollectionService collections) {
        this.collections = collections;
    }

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public int getApplyOrder() {
        return 20;
    }

    @Override
    public ParsedSeedDocument parse(SeedRawDocument document) {
        Map<String, Object> raw = document.getRaw();
        String name = text(raw, "name", document.getIndex());
        String owner = text(raw, "owner", document.getIndex());
        List<CollectionTypeSpec> types = parseTypes(raw.get("types"), document.getIndex());
        if (types.isEmpty()) {
            throw new SeedDocumentParseException(document.getIndex(), "Collection requires types");
        }
        ObjectWriteMode mode = ObjectWriteMode.UUID_OR_IDENTIFIER;
        if (raw.get("objectWriteMode") != null) {
            mode = ObjectWriteMode.valueOf(raw.get("objectWriteMode").toString().trim());
        }
        CollectionSeed payload = new CollectionSeed(
                name,
                optionalText(raw.get("description")),
                owner,
                optionalText(raw.get("ownerEmail")),
                optionalText(raw.get("supportEmail")),
                optionalText(raw.get("sla")),
                mode,
                types);
        return new ParsedSeedDocument(document, name, payload);
    }

    @Override
    public SeedDocumentResult apply(ParsedSeedDocument parsed) {
        CollectionSeed seed = (CollectionSeed) parsed.getPayload();
        CollectionEntity existing = collections.findByName(seed.name()).orElse(null);
        if (existing == null) {
            collections.create(
                    seed.name(),
                    seed.description(),
                    seed.owner(),
                    seed.ownerEmail(),
                    seed.supportEmail(),
                    seed.sla(),
                    seed.writeMode(),
                    seed.types());
        } else {
            collections.updateMetadata(
                    existing.getId(),
                    seed.name(),
                    seed.description(),
                    seed.owner(),
                    seed.ownerEmail(),
                    seed.supportEmail(),
                    seed.sla(),
                    seed.writeMode(),
                    seed.types());
        }
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

    @SuppressWarnings("unchecked")
    private static List<CollectionTypeSpec> parseTypes(Object raw, int index) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new SeedDocumentParseException(index, "types must be a non-empty list");
        }
        List<CollectionTypeSpec> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String s) {
                out.add(CollectionTypeSpec.of(s));
            } else if (item instanceof Map<?, ?> map) {
                Object type = map.get("objectType");
                if (type == null) {
                    throw new SeedDocumentParseException(index, "types[].objectType is required");
                }
                Object metadata = map.get("metadata");
                out.add(new CollectionTypeSpec(type.toString(), metadata == null ? null : metadata.toString()));
            } else {
                throw new SeedDocumentParseException(index, "types[] must be a string or object");
            }
        }
        return out;
    }

    private static String text(Map<String, Object> raw, String key, int index) {
        Object value = raw.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new SeedDocumentParseException(index, key + " must be a non-blank string");
        }
        return value.toString().trim();
    }

    private static String optionalText(Object value) {
        return value == null ? null : value.toString();
    }

    public record CollectionSeed(
            String name,
            String description,
            String owner,
            String ownerEmail,
            String supportEmail,
            String sla,
            ObjectWriteMode writeMode,
            List<CollectionTypeSpec> types
    ) {
    }
}
