package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class ObjsFlywayIntoExistingAppSchemaTest {

    @Test
    fun shouldBaselineAndApplyObjs_whenSchemaAlreadyHasAppTables() {
        val jdbcUrl = "jdbc:h2:mem:objs-flyway-nonempty;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        Class.forName("org.h2.Driver")
        DriverManager.getConnection(jdbcUrl, "sa", "").use { c ->
            c.createStatement().use { it.execute("CREATE TABLE app_existing (id INT PRIMARY KEY)") }
        }

        val location = ObjsFlywayVendor.resolveLocations(
            jdbcUrl,
            "classpath:org/poc/objs/core/db/migration/{vendor}",
        )
        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations(location)
            .table("flyway_schema_history_objs")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
        flyway.migrate()

        fun count(sql: String): Long =
            DriverManager.getConnection(jdbcUrl, "sa", "").use { c ->
                c.createStatement().use { st ->
                    st.executeQuery(sql).use { rs ->
                        check(rs.next())
                        rs.getLong(1)
                    }
                }
            }

        assertThat(count("SELECT COUNT(*) FROM app_existing")).isZero()
        assertThat(count("SELECT COUNT(*) FROM objs_entity")).isZero()
        assertThat(flyway.info().current()?.version?.toString()).isEqualTo("6")
    }
}
