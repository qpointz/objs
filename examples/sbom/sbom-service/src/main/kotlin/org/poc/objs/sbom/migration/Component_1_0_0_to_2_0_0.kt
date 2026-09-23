package org.poc.objs.sbom.migration

import org.poc.objs.api.typed.upgrade.ClassToClassUpgradeStep
import org.poc.objs.sbom.model.COMPONENT_SCHEMA_V2
import org.poc.objs.sbom.model.ComponentPayload
import org.poc.objs.sbom.model.ComponentPayloadV2
import org.poc.objs.sbom.model.SCHEMA_VERSION

/**
 * Hand L2 step: Component@1.0.0 → @2.0.0 (name→displayName, default tier).
 * Lives outside generated sources.
 */
class Component_1_0_0_to_2_0_0 : ClassToClassUpgradeStep<ComponentPayload, ComponentPayloadV2>() {
    override fun type(): String = "Component"

    override fun fromVersion(): String = SCHEMA_VERSION

    override fun toVersion(): String = COMPONENT_SCHEMA_V2

    override fun fromClass(): Class<ComponentPayload> = ComponentPayload::class.java

    override fun toClass(): Class<ComponentPayloadV2> = ComponentPayloadV2::class.java

    override fun upgrade(from: ComponentPayload): ComponentPayloadV2 =
        ComponentPayloadV2(
            displayName = from.name,
            version = from.version,
            ecosystem = from.ecosystem,
            kind = from.kind,
            coordinates = from.coordinates,
            description = from.description,
            tier = "STANDARD",
        )
}
