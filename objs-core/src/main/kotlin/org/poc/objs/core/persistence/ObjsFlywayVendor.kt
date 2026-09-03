package org.poc.objs.core.persistence

/**
 * Maps a JDBC URL to Flyway `{vendor}` id (`postgresql`, `h2`, …).
 */
object ObjsFlywayVendor {
    fun idFromJdbcUrl(jdbcUrl: String): String {
        val normalized = jdbcUrl.lowercase()
        return when {
            normalized.startsWith("jdbc:postgresql:") -> "postgresql"
            normalized.startsWith("jdbc:h2:") -> "h2"
            else -> throw IllegalArgumentException(
                "Unsupported JDBC URL for objs Flyway: $jdbcUrl (need postgresql or h2)",
            )
        }
    }

    fun resolveLocations(jdbcUrl: String, pattern: String): String =
        pattern.replace("{vendor}", idFromJdbcUrl(jdbcUrl))
}
