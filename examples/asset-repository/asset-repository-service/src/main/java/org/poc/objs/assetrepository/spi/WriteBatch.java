package org.poc.objs.assetrepository.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.poc.objs.api.domain.Edge;
import org.poc.objs.api.domain.Entity;

/** Mutable pending write batch between identity resolution and persist. */
public final class WriteBatch {

    private final List<PendingObject> objects = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<UUID> deleteEntityIds = new ArrayList<>();
    private final List<UUID> deleteEdgeIds = new ArrayList<>();

    public List<PendingObject> getObjects() {
        return objects;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<UUID> getDeleteEntityIds() {
        return deleteEntityIds;
    }

    public List<UUID> getDeleteEdgeIds() {
        return deleteEdgeIds;
    }

    public record PendingObject(Entity entity, EventExtension.ObjectChange.Op op) {
    }
}
