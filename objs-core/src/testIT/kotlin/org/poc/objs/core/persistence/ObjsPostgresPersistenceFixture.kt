package org.poc.objs.core.persistence

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/** Opens a unique PostgreSQL schema per test method (shared container). */
@Testcontainers
abstract class ObjsPostgresPersistenceFixture {
    companion object {
        @Container
        @JvmStatic
        val pg: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17-alpine")
    }

    protected lateinit var db: ObjsPersistenceTestSupport

    @BeforeEach
    fun openPersistence() {
        db = ObjsPersistenceTestSupport.postgres(pg.jdbcUrl, pg.username, pg.password)
    }

    @AfterEach
    fun closePersistence() {
        if (::db.isInitialized) db.close()
    }

    protected val store get() = db.graphStore
    protected val namedGraphs get() = db.namedGraphs
    protected val schemas get() = db.schemaCatalog
    protected val allowed get() = db.edgeCatalog
    protected val graphRepository get() = db.graphDao
    protected val schemaCatalog get() = db.schemaCatalog
    protected val edgeCatalog get() = db.edgeCatalog
    protected val schemaRepo get() = db.schemaCatalogDao
    protected val edgeRuleRepo get() = db.allowedEdgeRuleDao
    protected val uow get() = db.uow
}
