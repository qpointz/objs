package org.poc.objs.core.persistence

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Opens a unique PostgreSQL schema per test method.
 *
 * When `OBJS_IT_JDBC_URL` is set (CI Postgres service), connects to that database.
 * Otherwise starts a shared Testcontainers `postgres:17-alpine` instance (local / Docker).
 */
abstract class ObjsPostgresPersistenceFixture {
    companion object {
        private val envJdbcUrl: String? = System.getenv("OBJS_IT_JDBC_URL")?.trim()?.takeIf { it.isNotEmpty() }

        private val ownedContainer: PostgreSQLContainer<*>? =
            if (envJdbcUrl != null) {
                null
            } else {
                PostgreSQLContainer("postgres:17-alpine").also { it.start() }
            }

        private val jdbcUrl: String =
            envJdbcUrl ?: ownedContainer!!.jdbcUrl

        private val username: String =
            if (envJdbcUrl != null) {
                System.getenv("OBJS_IT_JDBC_USER")?.trim()?.takeIf { it.isNotEmpty() } ?: "objs"
            } else {
                ownedContainer!!.username
            }

        private val password: String =
            if (envJdbcUrl != null) {
                System.getenv("OBJS_IT_JDBC_PASSWORD")?.trim().orEmpty()
            } else {
                ownedContainer!!.password
            }
    }

    protected lateinit var db: ObjsPersistenceTestSupport

    @BeforeEach
    fun openPersistence() {
        db = ObjsPersistenceTestSupport.postgres(jdbcUrl, username, password)
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
