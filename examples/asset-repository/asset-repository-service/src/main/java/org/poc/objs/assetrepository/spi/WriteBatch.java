package org.poc.objs.assetrepository.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.poc.objs.core.domain.BoMEdge;
import org.poc.objs.core.domain.BoMEntity;

/** Mutable pending write batch between identity resolution and persist. */
public final class WriteBatch {

    private final List<PendingObject> objects = new ArrayList<>();
    private final List<BoMEdge> edges = new ArrayList<>();
    private final List<UUID> deleteEntityIds = new ArrayList<>();
    private final List<UUID> deleteEdgeIds = new ArrayList<>();

    public List<PendingObject> getObjects() {
        return objects;
    }

    public List<BoMEdge> getEdges() {
        return edges;
    }

    public List<UUID> getDeleteEntityIds() {
        return deleteEntityIds;
    }

    public List<UUID> getDeleteEdgeIds() {
        return deleteEdgeIds;
    }

    public record PendingObject(BoMEntity entity, EventExtension.ObjectChange.Op op) {
    }
}
