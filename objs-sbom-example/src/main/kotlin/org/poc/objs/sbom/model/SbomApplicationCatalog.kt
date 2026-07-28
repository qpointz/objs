package org.poc.objs.sbom.model

/**
 * Catalog of distinct BOM partitions discovered from entity annotations
 * (`app` / `appVersion`).
 */
data class SbomApplicationCatalog(
    val applications: List<SbomApplicationVersions>,
)

data class SbomApplicationVersions(
    val app: String,
    val versions: List<String>,
)
