package org.poc.objs.core.persistence

import com.github.benmanes.caffeine.cache.Ticker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Schema
import org.poc.objs.api.domain.SchemaDsl
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

class JpaCatalogCacheTest {

    class FakeNanosTicker : Ticker {
        private val nanos = AtomicLong(0)
        override fun read(): Long = nanos.get()
        fun advance(duration: Duration) {
            nanos.addAndGet(duration.toNanos())
        }
    }

    private val catalogFakeTicker = FakeNanosTicker()
    private lateinit var db: ObjsPersistenceTestSupport
    private lateinit var schemas: JpaSchemaCatalog
    private lateinit var schemaRepo: SchemaCatalogDao

    @BeforeEach
    fun open() {
        db = ObjsPersistenceTestSupport.h2(
            catalogTicker = catalogFakeTicker,
            catalogProperties = ObjsCatalogProperties(cacheTtl = Duration.ofSeconds(5)),
        )
        schemas = db.schemaCatalog
        schemaRepo = db.schemaCatalogDao
        schemas.clear()
    }

    @AfterEach
    fun close() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun shouldRehydrateAfterTtl_whenStoreTruncatedOutOfBand() {
        schemas.register(personSchema())
        assertThat(schemas.get("Person", "1")).isNotNull

        db.uow.write { schemaRepo.deleteAll() }
        assertThat(db.uow.read { schemaRepo.count() }).isZero()
        assertThat(schemas.get("Person", "1")).isNotNull

        catalogFakeTicker.advance(Duration.ofSeconds(6))
        assertThat(schemas.get("Person", "1")).isNull()
        assertThat(schemas.all()).isEmpty()
    }

    @Test
    fun shouldReflectStoreAfterExplicitRefresh() {
        schemas.register(personSchema())
        db.uow.write { schemaRepo.deleteAll() }
        assertThat(schemas.get("Person", "1")).isNotNull

        schemas.refreshFromStore()
        assertThat(schemas.get("Person", "1")).isNull()
    }

    @Test
    fun shouldWriteThroughWithoutWaitingForTtl() {
        schemas.register(personSchema())
        assertThat(schemas.get("Person", "1")).isNotNull
        assertThat(db.uow.read { schemaRepo.findById(SchemaCatalogId("Person", "1")) }).isNotNull
    }

    private fun personSchema() = Schema(
        type = "Person",
        version = "1",
        contentSchema = SchemaDsl.obj(
            title = "Person",
            description = "Person payload",
            fields = listOf(
                SchemaDsl.field(
                    name = "name",
                    required = true,
                    schema = SchemaDsl.string("Name", "Person name"),
                ),
            ),
        ),
    )
}
