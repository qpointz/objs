package org.poc.objs.assetrepository.web.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.poc.objs.api.domain.AllowedEdgeRule;
import org.poc.objs.assetrepository.domain.ObjectWriteMode;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record CollectionTypeDto(UUID id, String objectType, String metadata) {
    }

    public record CollectionDto(
            UUID id,
            String name,
            String description,
            String owner,
            String ownerEmail,
            String supportEmail,
            String sla,
            ObjectWriteMode objectWriteMode,
            UUID graphId,
            List<CollectionTypeDto> types
    ) {
    }

    public record CollectionStatisticsDto(
            UUID collectionId,
            int objectCount,
            java.time.Instant lastUpdated
    ) {
    }

    public record CollectionRefDto(UUID id, String name) {
    }

    public record TypeAllowedEdgesDto(
            List<AllowedEdgeRule> incoming,
            List<AllowedEdgeRule> outgoing
    ) {
    }

    public record SchemaCatalogEntryDto(
            String type,
            String latestVersion,
            List<String> versions,
            String title,
            String description,
            String usage,
            List<CollectionRefDto> usedIn
    ) {
    }

    public record CollectionTypeInput(String objectType, String metadata) {
    }

    public record CreateCollectionRequest(
            String name,
            String description,
            String owner,
            String ownerEmail,
            String supportEmail,
            String sla,
            ObjectWriteMode objectWriteMode,
            List<CollectionTypeInput> types
    ) {
    }

    public record CopyCollectionRequest(String name) {
    }

    public record PatchCollectionRequest(
            String name,
            String description,
            String owner,
            String ownerEmail,
            String supportEmail,
            String sla,
            ObjectWriteMode objectWriteMode,
            List<CollectionTypeInput> types
    ) {
    }

    public record ObjectDto(
            UUID id,
            String type,
            String schemaVersion,
            Map<String, Object> payload
    ) {
    }

    public record ObjectRelationDto(
            UUID edgeId,
            String role,
            String direction,
            ObjectDto related
    ) {
    }

    public record WriteObjectRequest(
            UUID id,
            String type,
            String schemaVersion,
            Map<String, Object> payload
    ) {
    }

    public record RelationInput(String sourceKey, String role, String targetKey) {
    }

    public record CompositionRequest(
            List<WriteObjectRequest> objects,
            List<RelationInput> relations
    ) {
    }

    public record SearchRequest(String matcherExpr, Map<String, String> filters) {
    }

    public record ErrorBody(String error, String detail) {
    }
}
