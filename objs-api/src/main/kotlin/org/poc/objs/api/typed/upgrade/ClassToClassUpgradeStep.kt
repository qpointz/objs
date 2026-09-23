package org.poc.objs.api.typed.upgrade

import org.poc.objs.api.typed.PayloadMapper

/**
 * Typed From→To hop (renames, splits, semantic remaps).
 * Decode of [fromClass] is strict when the supplied [PayloadMapper] is strict.
 */
abstract class ClassToClassUpgradeStep<F : Any, T : Any> : SchemaUpgradeStep {
    abstract fun fromClass(): Class<F>

    abstract fun toClass(): Class<T>

    abstract fun upgrade(from: F): T

    final override fun apply(payload: Map<String, Any?>, mapper: PayloadMapper): Map<String, Any?> {
        val from = mapper.fromMap(payload, fromClass())
        return mapper.toMap(upgrade(from))
    }
}
