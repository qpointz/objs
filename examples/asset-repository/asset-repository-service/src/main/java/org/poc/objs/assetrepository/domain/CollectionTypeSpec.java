package org.poc.objs.assetrepository.domain;

/**
 * Input for accepting an object type on a collection, with optional type-level metadata.
 */
public record CollectionTypeSpec(String objectType, String metadata) {

    public CollectionTypeSpec {
        if (objectType == null || objectType.isBlank()) {
            throw new IllegalArgumentException("objectType is required");
        }
        objectType = objectType.trim();
    }

    public static CollectionTypeSpec of(String objectType) {
        return new CollectionTypeSpec(objectType, null);
    }
}
