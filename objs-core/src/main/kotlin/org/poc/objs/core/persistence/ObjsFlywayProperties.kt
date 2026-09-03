package org.poc.objs.core.persistence

data class ObjsFlywayProperties(
    var enabled: Boolean = true,
    var table: String = "flyway_schema_history_objs",
    /**
     * Flyway locations. `{vendor}` is replaced with the JDBC vendor id (`postgresql`, `h2`, …).
     */
    var locations: String = "classpath:org/poc/objs/core/db/migration/{vendor}",
)
