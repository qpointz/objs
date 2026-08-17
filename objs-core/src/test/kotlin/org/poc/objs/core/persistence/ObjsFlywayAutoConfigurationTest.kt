package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
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

        assertThat(objsFlyway.flyway.info().current()?.version?.toString()).isEqualTo("1")
        assertThat(objsFlyway.flyway.configuration.table).isEqualTo("flyway_schema_history_objs")
    }
}
