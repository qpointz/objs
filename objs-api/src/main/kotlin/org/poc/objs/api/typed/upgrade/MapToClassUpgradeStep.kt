package org.poc.objs.api.typed.upgrade

import org.poc.objs.api.typed.PayloadMapper

/**
 * Map→typed To hop (additive defaults / light map surgery toward [toClass]).
 * Author controls Jackson strictness via [upgrade] implementation.
 */
abstract class MapToClassUpgradeStep<T : Any> : SchemaUpgradeStep {
    abstract fun toClass(): Class<T>

    abstract fun upgrade(from: Map<String, Any?>, mapper: PayloadMapper): T

    final override fun apply(payload: Map<String, Any?>, mapper: PayloadMapper): Map<String, Any?> =
        mapper.toMap(upgrade(payload, mapper))
}
