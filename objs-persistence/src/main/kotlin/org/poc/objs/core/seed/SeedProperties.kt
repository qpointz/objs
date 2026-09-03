package org.poc.objs.core.seed

enum class SeedFailureMode {
    FAIL_FAST,
    CONTINUE,
}

data class SeedProperties(
    var enabled: Boolean = true,
    var onFailure: SeedFailureMode = SeedFailureMode.FAIL_FAST,
    /** Ordered resource locations (classpath, file, etc.). */
    var resources: MutableList<String> = mutableListOf(),
)
