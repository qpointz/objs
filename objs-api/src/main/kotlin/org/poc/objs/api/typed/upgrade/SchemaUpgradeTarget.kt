package org.poc.objs.api.typed.upgrade

/**
 * Lane A latest pin + payload class for a catalog type (upgrade target).
 */
fun interface SchemaUpgradeTargetRegistry {
    fun latest(type: String): SchemaUpgradeTarget?
}

data class SchemaUpgradeTarget(
    val schemaVersion: String,
    val payloadClass: Class<*>,
)

data class UpgradeDiagnostics(
    val storedType: String,
    val storedSchemaVersion: String,
    val effectiveSchemaVersion: String?,
    val stepsApplied: List<String> = emptyList(),
    val additiveFallback: Boolean = false,
    val failure: String? = null,
) {
    companion object {
        fun exact(type: String, version: String) = UpgradeDiagnostics(
            storedType = type,
            storedSchemaVersion = version,
            effectiveSchemaVersion = version,
        )
    }
}
