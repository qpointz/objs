package org.poc.objs.assetrepository.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.domain.CollectionTypeEntity;
import org.poc.objs.assetrepository.domain.CollectionTypeSpec;
import org.poc.objs.assetrepository.service.CollectionService;
import org.poc.objs.assetrepository.service.ObjectWriteService;
import org.poc.objs.assetrepository.service.SchemaQueryService;
import org.poc.objs.assetrepository.web.dto.ApiDtos;
import org.poc.objs.core.domain.BoMSchema;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/asset-repository")
@Tag(name = "asset-repository", description = "Collections and objects (domain API)")
public class AssetRepositoryController {

    private final CollectionService collections;
    private final ObjectWriteService objects;
    private final SchemaQueryService schemaQuery;

    public AssetRepositoryController(
            CollectionService collections,
            ObjectWriteService objects,
            SchemaQueryService schemaQuery
    ) {
        this.collections = collections;
        this.objects = objects;
        this.schemaQuery = schemaQuery;
    }

    @GetMapping("/schema-catalog")
    @Operation(summary = "Latest object schema per type, with collections that use it")
    List<ApiDtos.SchemaCatalogEntryDto> schemaCatalog() {
        return schemaQuery.catalog();
    }

    @GetMapping("/schemas")
    @Operation(summary = "List object schemas", description = "Optional type filter.")
    List<BoMSchema> listSchemas(@RequestParam(required = false) String type) {
        return schemaQuery.list(type);
    }

    @GetMapping("/schemas/{type}")
    @Operation(summary = "List schema versions for a type")
    List<BoMSchema> listSchemasByType(@PathVariable("type") String type) {
        return schemaQuery.listByType(type);
    }

    @GetMapping("/schemas/{type}/{version}")
    @Operation(summary = "Get object schema by type and version")
    BoMSchema getSchema(@PathVariable("type") String type, @PathVariable("version") String version) {
        return schemaQuery.get(type, version);
    }

    @GetMapping("/schema-catalog/{type}/allowed-edges")
    @Operation(summary = "Allowed-edge rules for a type, including wildcards")
    ApiDtos.TypeAllowedEdgesDto allowedEdges(@PathVariable("type") String type) {
        return schemaQuery.allowedEdgesForType(type);
    }

    @GetMapping("/collections/{id}/schemas")
    @Operation(summary = "Schemas for a collection's accepted types")
    List<BoMSchema> collectionSchemas(@PathVariable("id") UUID id) {
        return schemaQuery.forCollection(id);
    }

    @GetMapping("/collections")
    @Operation(summary = "List collections", description = "Filter by name contains, owner, or accepted type.")
    List<ApiDtos.CollectionDto> listCollections(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String acceptedType
    ) {
        return collections.list(name, owner, acceptedType).stream().map(this::toDto).toList();
    }

    @PostMapping("/collections")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create collection")
    ApiDtos.CollectionDto createCollection(@RequestBody ApiDtos.CreateCollectionRequest request) {
        List<CollectionTypeSpec> types = request.types() == null
                ? List.of()
                : request.types().stream()
                        .map(t -> new CollectionTypeSpec(t.objectType(), t.metadata()))
                        .toList();
        return toDto(collections.create(
                request.name(),
                request.description(),
                request.owner(),
                request.ownerEmail(),
                request.supportEmail(),
                request.sla(),
                request.objectWriteMode(),
                types));
    }

    @GetMapping("/collections/{id}")
    @Operation(summary = "Get collection")
    ApiDtos.CollectionDto getCollection(@PathVariable("id") UUID id) {
        return toDto(collections.require(id));
    }

    @PostMapping("/collections/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Copy collection", description = "Live copy: shared object ids, new graph_id. Optional name, default Copy of {name}.")
    ApiDtos.CollectionDto copyCollection(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ApiDtos.CopyCollectionRequest request
    ) {
        String name = request == null ? null : request.name();
        return toDto(collections.copy(id, name));
    }

    @GetMapping("/collections/{id}/statistics")
    @Operation(
            summary = "Collection statistics",
            description = "Deferred stats for a collection. objectCount now; lastUpdated reserved for later.")
    ApiDtos.CollectionStatisticsDto collectionStatistics(@PathVariable("id") UUID id) {
        var entity = collections.require(id);
        return new ApiDtos.CollectionStatisticsDto(entity.getId(), collections.objectCount(entity), null);
    }

    @PatchMapping("/collections/{id}")
    @Operation(summary = "Update collection metadata")
    ApiDtos.CollectionDto patchCollection(
            @PathVariable("id") UUID id,
            @RequestBody ApiDtos.PatchCollectionRequest request
    ) {
        List<CollectionTypeSpec> types = request.types() == null
                ? null
                : request.types().stream()
                        .map(t -> new CollectionTypeSpec(t.objectType(), t.metadata()))
                        .toList();
        return toDto(collections.updateMetadata(
                id,
                request.name(),
                request.description(),
                request.owner(),
                request.ownerEmail(),
                request.supportEmail(),
                request.sla(),
                request.objectWriteMode(),
                types));
    }

    @GetMapping("/collections/{id}/objects")
    @Operation(summary = "List objects in a collection")
    List<ApiDtos.ObjectDto> listObjects(@PathVariable("id") UUID id) {
        return objects.listObjects(id);
    }

    @GetMapping("/collections/{id}/objects/{objectId}")
    @Operation(summary = "Get object")
    ApiDtos.ObjectDto getObject(@PathVariable("id") UUID id, @PathVariable("objectId") UUID objectId) {
        return objects.getObject(id, objectId);
    }

    @GetMapping("/collections/{id}/objects/{objectId}/relations")
    @Operation(summary = "List related objects", description = "In-collection edges incident to this object.")
    List<ApiDtos.ObjectRelationDto> listRelations(
            @PathVariable("id") UUID id,
            @PathVariable("objectId") UUID objectId
    ) {
        return objects.listRelations(id, objectId);
    }

    @PostMapping("/collections/{id}/objects")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create or update object", description = "Identity resolve per collection object_write_mode. When id matches an existing object, payload is a partial merge (omitted fields kept).")
    ApiDtos.ObjectDto writeObject(@PathVariable("id") UUID id, @RequestBody ApiDtos.WriteObjectRequest request) {
        return objects.writeObject(id, request);
    }

    @PostMapping("/collections/{id}/compositions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Write composition", description = "Objects plus in-collection relations (e.g. Database CONTAINS Dataset).")
    List<ApiDtos.ObjectDto> writeComposition(
            @PathVariable("id") UUID id,
            @RequestBody ApiDtos.CompositionRequest request
    ) {
        return objects.writeComposition(id, request);
    }

    @PostMapping("/collections/{id}/objects/search")
    @Operation(summary = "Search objects", description = "Collection-scoped matchers via filters or matcherExpr.")
    List<ApiDtos.ObjectDto> search(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ApiDtos.SearchRequest request
    ) {
        return objects.search(id, request);
    }

    @DeleteMapping("/collections/{id}/objects/{objectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete object")
    void deleteObject(@PathVariable("id") UUID id, @PathVariable("objectId") UUID objectId) {
        objects.deleteObject(id, objectId);
    }

    private ApiDtos.CollectionDto toDto(CollectionEntity entity) {
        List<ApiDtos.CollectionTypeDto> types = entity.getTypes().stream()
                .map(this::toTypeDto)
                .toList();
        return new ApiDtos.CollectionDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwner(),
                entity.getOwnerEmail(),
                entity.getSupportEmail(),
                entity.getSla(),
                entity.getObjectWriteMode(),
                entity.getGraphId(),
                types);
    }

    private ApiDtos.CollectionTypeDto toTypeDto(CollectionTypeEntity type) {
        return new ApiDtos.CollectionTypeDto(type.getId(), type.getObjectType(), type.getMetadata());
    }
}
