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
import org.poc.objs.api.domain.CatalogSupport;
import org.poc.objs.api.domain.AllowedEdgeCatalog;
import org.poc.objs.api.domain.AllowedEdgeRule;
import org.poc.objs.api.domain.Schema;
import org.poc.objs.api.domain.SchemaCatalog;
import org.poc.objs.api.domain.SchemaUsage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchemaQueryService {

    private final SchemaCatalog schemas;
    private final AllowedEdgeCatalog edges;
    private final CatalogSupport catalog;
    private final CollectionService collections;

    public SchemaQueryService(
            SchemaCatalog schemas,
            AllowedEdgeCatalog edges,
            CatalogSupport catalog,
            CollectionService collections
    ) {
        this.schemas = schemas;
        this.edges = edges;
        this.catalog = catalog;
        this.collections = collections;
    }

    public List<Schema> list(String typeFilter) {
        if (typeFilter == null || typeFilter.isBlank()) {
            return schemas.all().stream()
                    .sorted(Comparator.comparing(Schema::getType).thenComparing(Schema::getVersion))
                    .toList();
        }
        return listByType(typeFilter.trim());
    }

    public List<Schema> listByType(String type) {
        List<Schema> rows = schemas.listByType(type);
        if (rows.isEmpty()) {
            throw new NoSuchElementException("No schemas for type=" + type);
        }
        return rows.stream().sorted(Comparator.comparing(Schema::getVersion)).toList();
    }

    public Schema get(String type, String version) {
        Schema schema = schemas.get(type, version);
        if (schema == null) {
            throw new NoSuchElementException("Schema not found: " + type + "@" + version);
        }
        return schema;
    }

    /** Latest (lexicographic last version) schema per accepted type on the collection. */
    public List<Schema> forCollection(UUID collectionId) {
        CollectionEntity collection = collections.require(collectionId);
        List<Schema> out = new ArrayList<>();
        for (String type : collection.acceptedTypes()) {
            List<Schema> versions = schemas.listByType(type);
            if (versions.isEmpty()) {
                continue;
            }
            Schema latest = catalog.latestSchema(type);
            if (latest != null) {
                out.add(latest);
            }
        }
        return out;
    }

    /**
     * Latest schema per type (entity and edge-property), with collections that accept that type.
     */
    @Transactional(readOnly = true)
    public List<ApiDtos.SchemaCatalogEntryDto> catalog() {
        Map<String, List<Schema>> byType = new LinkedHashMap<>();
        schemas.all().stream()
                .sorted(Comparator.comparing(Schema::getType).thenComparing(Schema::getVersion))
                .forEach(s -> byType.computeIfAbsent(s.getType(), k -> new ArrayList<>()).add(s));

        Map<String, List<ApiDtos.CollectionRefDto>> usedIn = new LinkedHashMap<>();
        for (CollectionEntity collection : collections.list(null, null, null)) {
            ApiDtos.CollectionRefDto ref = new ApiDtos.CollectionRefDto(collection.getId(), collection.getName());
            for (String type : collection.acceptedTypes()) {
                usedIn.computeIfAbsent(type, k -> new ArrayList<>()).add(ref);
            }
        }

        List<ApiDtos.SchemaCatalogEntryDto> out = new ArrayList<>();
        for (Map.Entry<String, List<Schema>> e : byType.entrySet()) {
            List<Schema> versions = e.getValue();
            Schema latest = catalog.latestSchema(e.getKey());
            if (latest == null) {
                latest = versions.get(versions.size() - 1);
            }
            var node = latest.getContentSchema();
            out.add(new ApiDtos.SchemaCatalogEntryDto(
                    latest.getType(),
                    latest.getVersion(),
                    versions.stream().map(Schema::getVersion).toList(),
                    node.getTitle(),
                    node.getDescription(),
                    latest.getUsage().name(),
                    usedIn.getOrDefault(e.getKey(), List.of())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ApiDtos.TypeAllowedEdgesDto allowedEdgesForType(String type) {
        var allowed = catalog.allowedEdgesForType(type);
        return new ApiDtos.TypeAllowedEdgesDto(allowed.getIncoming(), allowed.getOutgoing());
    }
}
