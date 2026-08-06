package org.poc.objs.gremlin.core

/**
 * Per-request traversal options.
 *
 * @param timeoutSeconds evaluation timeout; null → [DEFAULT_TIMEOUT_SECONDS]
 * @param language script dialect; null → [LANGUAGE_GREMLIN_LANG]; unknown values rejected
 */
data class BoMGremlinTraversalOptions(
    val timeoutSeconds: Int? = null,
    val language: String? = null,
) {
    fun effectiveTimeoutSeconds(): Int {
        val value = timeoutSeconds ?: DEFAULT_TIMEOUT_SECONDS
        require(value > 0) { "timeoutSeconds must be > 0, got $value" }
        return value
    }

    fun effectiveLanguage(): String {
        val value = language ?: LANGUAGE_GREMLIN_LANG
        require(value == LANGUAGE_GREMLIN_LANG) {
            "Unsupported language '$value'; supported=[$LANGUAGE_GREMLIN_LANG]"
        }
        return value
    }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS: Int = 60
        const val LANGUAGE_GREMLIN_LANG: String = "gremlin-lang"
    }
}
