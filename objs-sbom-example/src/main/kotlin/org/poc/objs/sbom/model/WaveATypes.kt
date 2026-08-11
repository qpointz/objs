package org.poc.objs.sbom.model

import org.poc.objs.core.typed.EntityTypeMeta
import org.poc.objs.core.typed.TypedEntity
import java.util.UUID

data class ProductPayload(
    val name: String,
    val version: String,
    val supplier: String? = null,
    val lifecycle: String? = null,
    val homepage: String? = null,
    val description: String? = null
)

object ProductType {
    val meta = EntityTypeMeta(type = "Product", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: ProductPayload, id: UUID? = null) =
        TypedEntity(meta, ProductPayload::class.java, id, payload)
}

data class OrganizationPayload(
    val name: String,
    val domain: String? = null,
    val website: String? = null,
    val country: String? = null,
    val description: String? = null
)

object OrganizationType {
    val meta = EntityTypeMeta(type = "Organization", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: OrganizationPayload, id: UUID? = null) =
        TypedEntity(meta, OrganizationPayload::class.java, id, payload)
}

data class LicensePayload(
    val name: String,
    val spdxId: String,
    val url: String? = null,
    val description: String? = null
)

object LicenseType {
    val meta = EntityTypeMeta(type = "License", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: LicensePayload, id: UUID? = null) =
        TypedEntity(meta, LicensePayload::class.java, id, payload)
}

data class VulnerabilityPayload(
    val name: String,
    val cve: String,
    val severity: String,
    val cvss: Double? = null,
    val description: String? = null
)

object VulnerabilityType {
    val meta = EntityTypeMeta(type = "Vulnerability", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: VulnerabilityPayload, id: UUID? = null) =
        TypedEntity(meta, VulnerabilityPayload::class.java, id, payload)
}

data class BuildPayload(
    val name: String,
    val buildNumber: String,
    val status: String,
    val builder: String? = null,
    val description: String? = null
)

object BuildType {
    val meta = EntityTypeMeta(type = "Build", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: BuildPayload, id: UUID? = null) =
        TypedEntity(meta, BuildPayload::class.java, id, payload)
}
