package org.poc.objs.assetrepository.spi;

import org.poc.objs.assetrepository.domain.CollectionEntity;

/**
 * Pre-persist enrichment hook (G-P5). Default implementations are no-ops.
 */
@FunctionalInterface
public interface PreprocessingExtension {

    WriteBatch preprocess(WriteContext context, WriteBatch batch);

    record WriteContext(CollectionEntity collection) {
    }
}
