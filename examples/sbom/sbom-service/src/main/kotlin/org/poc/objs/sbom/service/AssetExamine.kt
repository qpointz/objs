package org.poc.objs.sbom.service

import org.poc.objs.api.domain.Entity
import org.poc.objs.api.typed.PayloadMapper
import org.poc.objs.api.typed.TypedEntityBindingRegistry
import org.poc.objs.api.typed.upgrade.HydrationPolicy
import org.poc.objs.api.typed.upgrade.PayloadUpgrade
import org.poc.objs.api.typed.upgrade.SchemaUpgradeRegistry
import org.poc.objs.api.typed.upgrade.SchemaUpgradeTargetRegistry
import org.poc.objs.sbom.domain.AssetView
import org.poc.objs.sbom.domain.PayloadRepresentation

/**
 * Evidence (as-saved) vs examine (latest) projection for BOM asset views.
 */
object AssetExamine {
    fun project(
        entity: Entity,
        representation: PayloadRepresentation,
        mapper: PayloadMapper,
        upgrades: SchemaUpgradeRegistry,
        targets: SchemaUpgradeTargetRegistry,
    ): AssetView {
        val evidence = AssetViews.asset(entity)
        if (representation == PayloadRepresentation.SAVED) {
            return evidence
        }
        val outcome = PayloadUpgrade.hydrate(
            entity = entity,
            bindings = TypedEntityBindingRegistry { _, _ -> null },
            mapper = mapper,
            policy = HydrationPolicy.UPGRADE_TO_LATEST,
            upgrades = upgrades,
            targets = targets,
        )
        val latestMap = outcome.hydrated?.let { mapper.toMap(it) }
        val latestVersion = outcome.diagnostics?.effectiveSchemaVersion
        return when (representation) {
            PayloadRepresentation.LATEST -> {
                if (latestMap == null || latestVersion == null) {
                    evidence
                } else {
                    evidence.copy(
                        schemaVersion = latestVersion,
                        payload = latestMap,
                        label = AssetViews.label(latestMap, entity.type),
                        latestSchemaVersion = null,
                        latestPayload = null,
                    )
                }
            }
            PayloadRepresentation.BOTH -> evidence.copy(
                latestSchemaVersion = latestVersion,
                latestPayload = latestMap,
            )
            PayloadRepresentation.SAVED -> evidence
        }
    }
}
