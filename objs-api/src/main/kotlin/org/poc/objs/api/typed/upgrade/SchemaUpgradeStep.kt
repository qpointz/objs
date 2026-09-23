package org.poc.objs.api.typed.upgrade

import org.poc.objs.api.typed.PayloadMapper

/**
 * Uniform runtime hop for schema payload upgrades (L2). Chain engine only calls [apply].
 */
interface SchemaUpgradeStep {
    fun type(): String

    fun fromVersion(): String

    fun toVersion(): String

    fun apply(payload: Map<String, Any?>, mapper: PayloadMapper): Map<String, Any?>
}
