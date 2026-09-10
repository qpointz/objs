package org.poc.objs.policy.api

/**
 * Closed finding / reported severity set (C-34 / G-P51v).
 * No soft aliases (`MODERATE`, `INFORMATIONAL`, `WARN`).
 */
enum class FindingSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO,
    ;

    companion object {
        /** Parse canonical token; unknown / blank → null. Does not map legacy aliases (that is [parseLegacyOrNull]). */
        fun parseOrNull(raw: String?): FindingSeverity? {
            if (raw.isNullOrBlank()) return null
            return runCatching { valueOf(raw.trim().uppercase()) }.getOrNull()
        }

        /**
         * Dual-read for archived / filter tokens (G-P54v):
         * `ERROR`→HIGH, `WARNING`/`WARN`→MEDIUM, `OK`→null; else [parseOrNull].
         */
        fun parseLegacyOrNull(raw: String?): FindingSeverity? {
            if (raw.isNullOrBlank()) return null
            return when (raw.trim().uppercase()) {
                "ERROR" -> HIGH
                "WARNING", "WARN" -> MEDIUM
                "OK" -> null
                else -> parseOrNull(raw)
            }
        }
    }
}
