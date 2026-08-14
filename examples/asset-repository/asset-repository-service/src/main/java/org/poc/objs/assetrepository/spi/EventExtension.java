package org.poc.objs.assetrepository.spi;

import java.util.List;
import java.util.UUID;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.core.domain.BoMEdge;
import org.poc.objs.core.domain.BoMEntity;

/**
 * Post-persist event analysis hook (G-P5). Default implementations emit nothing.
 */
@FunctionalInterface
public interface EventExtension {

    List<DomainEvent> onChanges(CollectionChangeSet changes);

    record DomainEvent(String type, String message) {
    }

    record CollectionChangeSet(
            CollectionEntity collection,
            List<ObjectChange> objects,
            List<BoMEdge> edges
    ) {
    }

    record ObjectChange(UUID id, String type, Op op, BoMEntity entity) {
        public enum Op {
            CREATE,
            UPDATE,
            DELETE
        }
    }
}
