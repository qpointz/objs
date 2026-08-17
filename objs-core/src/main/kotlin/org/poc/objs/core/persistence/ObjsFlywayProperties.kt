package org.poc.objs.core.persistence

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "objs.flyway")
data class ObjsFlywayProperties(
    var enabled: Boolean = true,
    var table: String = "flyway_schema_history_objs",
    /**
     * Flyway locations. `{vendor}` is replaced with the Spring Boot [org.springframework.boot.jdbc.DatabaseDriver]
     * id (`postgresql`, `h2`, …).
     */
    var locations: String = "classpath:org/poc/objs/core/db/migration/{vendor}",
)
