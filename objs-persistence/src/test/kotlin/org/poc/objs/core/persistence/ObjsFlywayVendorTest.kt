package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ObjsFlywayVendorTest {

    @Test
    fun shouldResolvePostgresql_whenJdbcUrl() {
        assertThat(ObjsFlywayVendor.idFromJdbcUrl("jdbc:postgresql://localhost:5432/objs"))
            .isEqualTo("postgresql")
    }

    @Test
    fun shouldResolveH2_whenJdbcUrlEvenWithPostgresMode() {
        assertThat(ObjsFlywayVendor.idFromJdbcUrl("jdbc:h2:mem:objs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"))
            .isEqualTo("h2")
    }

    @Test
    fun shouldFailFast_whenUnknownDriver() {
        assertThatThrownBy { ObjsFlywayVendor.idFromJdbcUrl("jdbc:unknown:foo") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unsupported JDBC URL")
    }

    @Test
    fun shouldSubstituteVendorPlaceholder() {
        assertThat(
            ObjsFlywayVendor.resolveLocations(
                "jdbc:h2:mem:x",
                "classpath:org/poc/objs/core/db/migration/{vendor}",
            ),
        ).isEqualTo("classpath:org/poc/objs/core/db/migration/h2")
    }
}
