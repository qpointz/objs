package org.poc.objs.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.persistence.ObjsFlyway
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

class ObjsFlywayAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration::class.java,
                HibernateJpaAutoConfiguration::class.java,
                TransactionAutoConfiguration::class.java,
                ObjsCoreAutoConfiguration::class.java,
            ),
        )
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:objs-flyway-boot;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=false",
            "objs.seeds.enabled=false",
        )

    @Test
    fun shouldApplyVendorSql_whenBootFlywayDisabled() {
        contextRunner.run { context ->
            val jdbc = JdbcTemplate(context.getBean(DataSource::class.java))
            val entityCount = jdbc.queryForObject("SELECT COUNT(*) FROM objs_entity", Int::class.java)
            assertThat(entityCount).isZero()

            val objsFlyway = context.getBean(ObjsFlyway::class.java)
            assertThat(objsFlyway.flyway.info().current()?.version?.toString()).isEqualTo("6")
            assertThat(objsFlyway.flyway.configuration.table).isEqualTo("flyway_schema_history_objs")
            val clocks = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE LOWER(table_name) = 'objs_entity'
                  AND LOWER(column_name) IN ('created_at', 'updated_at')
                """.trimIndent(),
                Int::class.java,
            )
            assertThat(clocks).isEqualTo(2)
        }
    }
}
