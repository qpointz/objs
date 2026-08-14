package org.poc.objs.assetrepository.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.web.dto.ApiDtos;
import org.poc.objs.core.domain.BoMSchema;
import org.poc.objs.core.domain.BoMSchemaCatalog;
import org.poc.objs.core.domain.BoMSchemaUsage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchemaQueryService {

    private final BoMSchemaCatalog schemas;
    private final CollectionService collections;

    public SchemaQueryService(BoMSchemaCatalog schemas, CollectionService collections) {
        this.schemas = schemas;
        this.collections = collections;
    }

    public List<BoMSchema> list(String typeFilter) {
        if (typeFilter == null || typeFilter.isBlank()) {
            return schemas.all().stream()
                    .sorted(Comparator.comparing(BoMSchema::getType).thenComparing(BoMSchema::getVersion))
                    .toList();
        }
        return listByType(typeFilter.trim());
    }

    public List<BoMSchema> listByType(String type) {
        List<BoMSchema> rows = schemas.listByType(type);
        if (rows.isEmpty()) {
            throw new NoSuchElementException("No schemas for type=" + type);
        }
        return rows.stream().sorted(Comparator.comparing(BoMSchema::getVersion)).toList();
    }

    public BoMSchema get(String type, String version) {
        BoMSchema schema = schemas.get(type, version);
        if (schema == null) {
            throw new NoSuchElementException("Schema not found: " + type + "@" + version);
        }
        return schema;
    }

    /** Latest (lexicographic last version) schema per accepted type on the collection. */
    public List<BoMSchema> forCollection(UUID collectionId) {
        CollectionEntity collection = collections.require(collectionId);
        List<BoMSchema> out = new ArrayList<>();
        for (String type : collection.acceptedTypes()) {
            List<BoMSchema> versions = schemas.listByType(type);
            if (versions.isEmpty()) {
                continue;
            }
            out.add(versions.stream()
                    .max(Comparator.comparing(BoMSchema::getVersion))
                    .orElseThrow());
        }
        return out;
    }

    /**
     * Latest ENTITY schema per type, with collections that accept that type.
     */
    @Transactional(readOnly = true)
    public List<ApiDtos.SchemaCatalogEntryDto> catalog() {
        Map<String, List<BoMSchema>> byType = new LinkedHashMap<>();
        schemas.all().stream()
                .filter(s -> s.getUsage() == BoMSchemaUsage.ENTITY)
                .sorted(Comparator.comparing(BoMSchema::getType).thenComparing(BoMSchema::getVersion))
                .forEach(s -> byType.computeIfAbsent(s.getType(), k -> new ArrayList<>()).add(s));

        Map<String, List<ApiDtos.CollectionRefDto>> usedIn = new LinkedHashMap<>();
        for (CollectionEntity collection : collections.list(null, null, null)) {
            ApiDtos.CollectionRefDto ref = new ApiDtos.CollectionRefDto(collection.getId(), collection.getName());
            for (String type : collection.acceptedTypes()) {
                usedIn.computeIfAbsent(type, k -> new ArrayList<>()).add(ref);
            }
        }

        List<ApiDtos.SchemaCatalogEntryDto> out = new ArrayList<>();
        for (Map.Entry<String, List<BoMSchema>> e : byType.entrySet()) {
            List<BoMSchema> versions = e.getValue();
            BoMSchema latest = versions.get(versions.size() - 1);
            var node = latest.getContentSchema();
            out.add(new ApiDtos.SchemaCatalogEntryDto(
                    latest.getType(),
                    latest.getVersion(),
                    versions.stream().map(BoMSchema::getVersion).toList(),
                    node.getTitle(),
                    node.getDescription(),
                    latest.getUsage().name(),
                    usedIn.getOrDefault(e.getKey(), List.of())));
        }
        return out;
    }
}
