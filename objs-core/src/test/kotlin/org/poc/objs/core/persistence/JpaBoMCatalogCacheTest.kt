package org.poc.objs.core.persistence

import com.github.benmanes.caffeine.cache.Ticker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDsl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(JpaBoMCatalogCacheTest.CatalogTickerConfig::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:objs-catalog-ttl;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=false",
        "objs.seeds.enabled=false",
        "objs.catalogs.cache-ttl=5s",
    ],
)
class JpaBoMCatalogCacheTest {

    @SpringBootConfiguration
    class TestApp

    @TestConfiguration
    class CatalogTickerConfig {
        @Bean
        fun catalogFakeTicker(): FakeNanosTicker = FakeNanosTicker()

        @Bean
        @Primary
        fun bomSchemaCatalog(
            repo: BoMSchemaCatalogRepository,
            catalogProperties: ObjsCatalogProperties,
            catalogFakeTicker: FakeNanosTicker,
        ): BoMSchemaCatalog = JpaBoMSchemaCatalog(repo, catalogProperties, catalogFakeTicker)

        @Bean
        @Primary
        fun bomAllowedEdgeCatalog(
            repo: BoMAllowedEdgeRuleRepository,
            catalogProperties: ObjsCatalogProperties,
            catalogFakeTicker: FakeNanosTicker,
        ): JpaBoMAllowedEdgeCatalog = JpaBoMAllowedEdgeCatalog(repo, catalogProperties, catalogFakeTicker)
    }

    class FakeNanosTicker : Ticker {
        private val nanos = AtomicLong(0)

        override fun read(): Long = nanos.get()

        fun advance(duration: Duration) {
            nanos.addAndGet(duration.toNanos())
        }
    }

    @Autowired
    lateinit var schemas: JpaBoMSchemaCatalog

    @Autowired
    lateinit var schemaRepo: BoMSchemaCatalogRepository

    @Autowired
    lateinit var catalogFakeTicker: FakeNanosTicker

    @BeforeEach
    fun clear() {
        schemas.clear()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun shouldRehydrateAfterTtl_whenStoreTruncatedOutOfBand() {
        schemas.register(personSchema())
        assertThat(schemas.get("Person", "1")).isNotNull

        schemaRepo.deleteAll()
        assertThat(schemaRepo.count()).isZero()
        assertThat(schemas.get("Person", "1")).isNotNull

        catalogFakeTicker.advance(Duration.ofSeconds(6))
        assertThat(schemas.get("Person", "1")).isNull()
        assertThat(schemas.all()).isEmpty()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun shouldReflectStoreAfterExplicitRefresh() {
        schemas.register(personSchema())
        schemaRepo.deleteAll()
        assertThat(schemas.get("Person", "1")).isNotNull

        schemas.refreshFromStore()
        assertThat(schemas.get("Person", "1")).isNull()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun shouldWriteThroughWithoutWaitingForTtl() {
        schemas.register(personSchema())
        assertThat(schemas.get("Person", "1")).isNotNull
        assertThat(schemaRepo.findById(BoMSchemaCatalogId("Person", "1"))).isPresent
    }

    private fun personSchema() = BoMSchema(
        type = "Person",
        version = "1",
        contentSchema = BoMSchemaDsl.obj(
            title = "Person",
            description = "Person payload",
            fields = listOf(
                BoMSchemaDsl.field(
                    name = "name",
                    required = true,
                    schema = BoMSchemaDsl.string("Name", "Person name"),
                ),
            ),
        ),
    )
}
