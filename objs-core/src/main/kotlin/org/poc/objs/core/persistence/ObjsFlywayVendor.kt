package org.poc.objs.core.persistence

import org.springframework.boot.jdbc.DatabaseDriver

/**
 * Maps a JDBC URL to Spring Boot Flyway `{vendor}` ([DatabaseDriver.id]).
 */
object ObjsFlywayVendor {
    fun idFromJdbcUrl(jdbcUrl: String): String {
        val driver = DatabaseDriver.fromJdbcUrl(jdbcUrl)
        require(driver != DatabaseDriver.UNKNOWN) {
            "Unsupported JDBC URL for objs Flyway: $jdbcUrl (need a Spring DatabaseDriver id such as postgresql or h2)"
        }
        return driver.id
    }

    fun resolveLocations(jdbcUrl: String, pattern: String): String =
        pattern.replace("{vendor}", idFromJdbcUrl(jdbcUrl))
}
