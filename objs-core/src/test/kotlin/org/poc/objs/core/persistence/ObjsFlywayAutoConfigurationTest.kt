package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-flyway;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=false",
    ],
)
class ObjsFlywayAutoConfigurationTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var objsFlyway: ObjsFlyway

    @Test
    fun shouldApplyVendorSql_whenBootFlywayDisabled() {
        val entityCount = jdbc.queryForObject("SELECT COUNT(*) FROM bom_entity", Int::class.java)
        assertThat(entityCount).isZero()

        assertThat(objsFlyway.flyway.info().current()?.version?.toString()).isEqualTo("5")
        assertThat(objsFlyway.flyway.configuration.table).isEqualTo("flyway_schema_history_objs")
        val clocks = jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE LOWER(table_name) = 'bom_entity'
              AND LOWER(column_name) IN ('created_at', 'updated_at')
            """.trimIndent(),
            Int::class.java,
        )
        assertThat(clocks).isEqualTo(2)
    }
}

class ObjsFlywayIntoExistingAppSchemaTest {

    @Test
    fun shouldBaselineAndApplyObjs_whenSchemaAlreadyHasAppTables() {
        val jdbcUrl = "jdbc:h2:mem:objs-flyway-nonempty;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        val ds = DataSourceBuilder.create()
            .url(jdbcUrl)
            .username("sa")
            .password("")
            .driverClassName("org.h2.Driver")
            .build()
        ds.connection.use { c ->
            c.createStatement().use { it.execute("CREATE TABLE app_existing (id INT PRIMARY KEY)") }
        }

        val location = ObjsFlywayVendor.resolveLocations(
            jdbcUrl,
            "classpath:org/poc/objs/core/db/migration/{vendor}",
        )
        val flyway = Flyway.configure()
            .dataSource(ds)
            .locations(location)
            .table("flyway_schema_history_objs")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
        flyway.migrate()

        val jdbc = JdbcTemplate(ds)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_existing", Int::class.java)).isZero()
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bom_entity", Int::class.java)).isZero()
        assertThat(flyway.info().current()?.version?.toString()).isEqualTo("5")
    }
}
