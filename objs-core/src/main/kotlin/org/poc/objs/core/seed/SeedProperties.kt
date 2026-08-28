package org.poc.objs.core.seed

import org.springframework.boot.context.properties.ConfigurationProperties

enum class SeedFailureMode {
    FAIL_FAST,
    CONTINUE,
}

@ConfigurationProperties(prefix = "objs.seeds")
data class SeedProperties(
    var enabled: Boolean = true,
    var onFailure: SeedFailureMode = SeedFailureMode.FAIL_FAST,
    /** Ordered Spring resource locations (`classpath:` or `file:`). */
    var resources: MutableList<String> = mutableListOf(),
)
