package org.poc.objs.core.seed

import org.springframework.boot.context.properties.ConfigurationProperties

enum class SeedFailureMode {
    FAIL_FAST,
    CONTINUE,
}

data class SeedResourceProperties(
    /** Spring resource location, e.g. `classpath:seeds/ontology.yaml` or `file:/path/to.yaml`. */
    var location: String = "",
    /** Optional stable ledger key. When blank, a sanitized location is used. */
    var name: String? = null,
)

@ConfigurationProperties(prefix = "objs.seeds")
data class BoMSeedProperties(
    var enabled: Boolean = true,
    var onFailure: SeedFailureMode = SeedFailureMode.FAIL_FAST,
    var resources: MutableList<SeedResourceProperties> = mutableListOf(),
)
