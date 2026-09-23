package org.poc.objs.api.typed.upgrade

import org.poc.objs.api.domain.Entity
import org.poc.objs.api.typed.PayloadMapper
import org.poc.objs.api.typed.TypedEntityBindingRegistry

/**
 * Orchestrates exact bind → explicit chain → additive fallback for one entity.
 */
object PayloadUpgrade {
    data class Outcome(
        val hydrated: Any?,
        val diagnostics: UpgradeDiagnostics?,
    )

    @JvmStatic
    fun hydrate(
        entity: Entity,
        bindings: TypedEntityBindingRegistry,
        mapper: PayloadMapper,
        policy: HydrationPolicy,
        upgrades: SchemaUpgradeRegistry?,
        targets: SchemaUpgradeTargetRegistry?,
    ): Outcome {
        bindings.find(entity.type, entity.schemaVersion)?.let { binding ->
            return Outcome(
                hydrated = binding.hydrate(entity, mapper),
                diagnostics = UpgradeDiagnostics.exact(entity.type, entity.schemaVersion),
            )
        }

        if (policy != HydrationPolicy.UPGRADE_TO_LATEST || upgrades == null || targets == null) {
            return Outcome(null, null)
        }

        val target = targets.latest(entity.type)
            ?: return Outcome(
                null,
                UpgradeDiagnostics(
                    storedType = entity.type,
                    storedSchemaVersion = entity.schemaVersion,
                    effectiveSchemaVersion = null,
                    failure = "no latest upgrade target for type",
                ),
            )

        if (entity.schemaVersion == target.schemaVersion) {
            return Outcome(
                null,
                UpgradeDiagnostics(
                    storedType = entity.type,
                    storedSchemaVersion = entity.schemaVersion,
                    effectiveSchemaVersion = null,
                    failure = "no exact binding for latest pin",
                ),
            )
        }

        val chain = try {
            upgrades.findChain(entity.type, entity.schemaVersion, target.schemaVersion)
        } catch (ex: AmbiguousUpgradePathException) {
            return Outcome(
                null,
                UpgradeDiagnostics(
                    storedType = entity.type,
                    storedSchemaVersion = entity.schemaVersion,
                    effectiveSchemaVersion = null,
                    failure = ex.message,
                ),
            )
        }

        if (chain.isNotEmpty()) {
            return try {
                var map: Map<String, Any?> = entity.payload.toMap()
                for (step in chain) {
                    map = step.apply(map, mapper)
                }
                val hydrated = mapper.fromMap(map, target.payloadClass)
                Outcome(
                    hydrated = hydrated,
                    diagnostics = UpgradeDiagnostics(
                        storedType = entity.type,
                        storedSchemaVersion = entity.schemaVersion,
                        effectiveSchemaVersion = target.schemaVersion,
                        stepsApplied = chain.map { "${it.fromVersion()}->${it.toVersion()}" },
                    ),
                )
            } catch (ex: Exception) {
                Outcome(
                    null,
                    UpgradeDiagnostics(
                        storedType = entity.type,
                        storedSchemaVersion = entity.schemaVersion,
                        effectiveSchemaVersion = null,
                        stepsApplied = chain.map { "${it.fromVersion()}->${it.toVersion()}" },
                        failure = ex.message ?: ex.javaClass.simpleName,
                    ),
                )
            }
        }

        return try {
            val hydrated = mapper.fromMap(entity.payload, target.payloadClass)
            Outcome(
                hydrated = hydrated,
                diagnostics = UpgradeDiagnostics(
                    storedType = entity.type,
                    storedSchemaVersion = entity.schemaVersion,
                    effectiveSchemaVersion = target.schemaVersion,
                    additiveFallback = true,
                ),
            )
        } catch (ex: Exception) {
            Outcome(
                null,
                UpgradeDiagnostics(
                    storedType = entity.type,
                    storedSchemaVersion = entity.schemaVersion,
                    effectiveSchemaVersion = null,
                    additiveFallback = true,
                    failure = ex.message ?: ex.javaClass.simpleName,
                ),
            )
        }
    }
}
