package org.poc.objs.core.persistence

import com.github.benmanes.caffeine.cache.Ticker
import org.flywaydb.core.Flyway
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import org.poc.objs.api.domain.CatalogSupport
import org.poc.objs.api.versioning.ExplicitOnlyVersioningStrategy
import org.poc.objs.core.persistence.tx.EntityManagerUnitOfWork
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.core.seed.AllowedEdgeRuleSeedHandler
import org.poc.objs.core.seed.GraphSeedHandler
import org.poc.objs.core.seed.ObjectSchemaSeedHandler
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.core.seed.SeedLedger
import org.poc.objs.core.validation.Validator
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.io.PrintWriter
import java.util.UUID
import java.util.logging.Logger
import javax.sql.DataSource
import jakarta.persistence.EntityManagerFactory

/**
 * Spring-free persistence harness (G-A4): Flyway + Hibernate EMF + EM [UnitOfWork] + stores.
 */
class ObjsPersistenceTestSupport private constructor(
    val dataSource: DataSource,
    val emf: EntityManagerFactory,
    val flyway: Flyway,
    catalogTicker: Ticker,
    catalogProperties: ObjsCatalogProperties,
) : AutoCloseable {

    val uow: UnitOfWork = EntityManagerUnitOfWork(emf)

    val entityDao = EntityDao(uow)
    val edgeDao = EdgeDao(uow)
    val graphDao = GraphDao(uow)
    val membershipDao = GraphMembershipDao(uow)
    val entityVersionDao = EntityVersionDao(uow)
    val graphVersionDao = GraphVersionDao(uow)
    val edgeVersionDao = EdgeVersionDao(uow)
    val graphVersionMemberDao = GraphVersionMemberDao(uow)
    val graphVersionEdgeDao = GraphVersionEdgeDao(uow)
    val schemaCatalogDao = SchemaCatalogDao(uow)
    val allowedEdgeRuleDao = AllowedEdgeRuleDao(uow)
    val seedLedgerDao = SeedLedgerDao(uow)

    val schemaCatalog = JpaSchemaCatalog(schemaCatalogDao, uow, catalogProperties, catalogTicker)
    val edgeCatalog = JpaAllowedEdgeCatalog(allowedEdgeRuleDao, uow, catalogProperties, catalogTicker)
    val catalogSupport = CatalogSupport(schemaCatalog, edgeCatalog)
    val validator = Validator(schemaCatalog, edgeCatalog)
    val poolReader = PoolEntityReader(uow)
    val deepVersions = DeepGraphVersionService(
        graphDao = graphDao,
        membershipDao = membershipDao,
        entityDao = entityDao,
        edgeDao = edgeDao,
        entityVersions = entityVersionDao,
        graphVersions = graphVersionDao,
        edgeVersions = edgeVersionDao,
        versionMembers = graphVersionMemberDao,
        versionEdges = graphVersionEdgeDao,
        uow = uow,
    )
    val namedGraphs = NamedGraphStore(
        graphDao = graphDao,
        membershipDao = membershipDao,
        entityDao = entityDao,
        edgeDao = edgeDao,
        validator = validator,
        deepVersions = deepVersions,
        versionMemberDao = graphVersionMemberDao,
        uow = uow,
    )
    val graphStore = GraphStore(
        entityDao = entityDao,
        edgeDao = edgeDao,
        validator = validator,
        namedGraphs = namedGraphs,
        poolReader = poolReader,
        schemas = schemaCatalog,
        catalog = catalogSupport,
        versioning = ExplicitOnlyVersioningStrategy(),
        uow = uow,
    ).also { namedGraphs.attachGraphStore(it) }

    val objectSchemaSeedHandler = ObjectSchemaSeedHandler(schemaCatalog)
    val allowedEdgeRuleSeedHandler = AllowedEdgeRuleSeedHandler(edgeCatalog)
    val graphSeedHandler = GraphSeedHandler(namedGraphs)
    val seedImporter = SeedImporter(
        listOf(objectSchemaSeedHandler, allowedEdgeRuleSeedHandler, graphSeedHandler),
        uow,
    )
    val seedLedger = SeedLedger(seedLedgerDao, uow)

    init {
        schemaCatalog.hydrate()
        edgeCatalog.hydrate()
    }

    fun queryLong(sql: String): Long =
        dataSource.connection.use { c ->
            c.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    check(rs.next()) { "Expected a row for: $sql" }
                    rs.getLong(1)
                }
            }
        }

    fun queryStrings(sql: String): List<String> =
        dataSource.connection.use { c ->
            c.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    buildList {
                        while (rs.next()) add(rs.getString(1))
                    }
                }
            }
        }

    override fun close() {
        emf.close()
    }

    companion object {
        fun h2(
            catalogTicker: Ticker = Ticker.systemTicker(),
            catalogProperties: ObjsCatalogProperties = ObjsCatalogProperties(),
        ): ObjsPersistenceTestSupport {
            val url = "jdbc:h2:mem:objs-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
            return open(
                jdbcUrl = url,
                username = "sa",
                password = "",
                driver = "org.h2.Driver",
                catalogTicker = catalogTicker,
                catalogProperties = catalogProperties,
            )
        }

        fun postgres(
            jdbcUrl: String,
            username: String,
            password: String,
            catalogTicker: Ticker = Ticker.systemTicker(),
            catalogProperties: ObjsCatalogProperties = ObjsCatalogProperties(),
        ): ObjsPersistenceTestSupport {
            val schema = "t" + UUID.randomUUID().toString().replace("-", "")
            DriverManager.getConnection(jdbcUrl, username, password).use { c ->
                c.createStatement().use { it.execute("CREATE SCHEMA $schema") }
            }
            val joiner = if (jdbcUrl.contains('?')) "&" else "?"
            return open(
                jdbcUrl = "$jdbcUrl${joiner}currentSchema=$schema",
                username = username,
                password = password,
                driver = "org.postgresql.Driver",
                catalogTicker = catalogTicker,
                catalogProperties = catalogProperties,
            )
        }

        fun open(
            jdbcUrl: String,
            username: String,
            password: String,
            driver: String,
            catalogTicker: Ticker = Ticker.systemTicker(),
            catalogProperties: ObjsCatalogProperties = ObjsCatalogProperties(),
        ): ObjsPersistenceTestSupport {
            Class.forName(driver)
            val dataSource = DriverManagerDataSource(jdbcUrl, username, password)
            val flywayProps = ObjsFlywayProperties()
            val location = ObjsFlywayVendor.resolveLocations(jdbcUrl, flywayProps.locations)
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(flywayProps.table)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
            flyway.migrate()

            val cfg = Configuration()
            cfg.setProperty(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl)
            cfg.setProperty(AvailableSettings.JAKARTA_JDBC_USER, username)
            cfg.setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, password)
            cfg.setProperty(AvailableSettings.JAKARTA_JDBC_DRIVER, driver)
            cfg.setProperty(AvailableSettings.HBM2DDL_AUTO, "validate")
            cfg.setProperty(AvailableSettings.SHOW_SQL, "false")
            MANAGED_TYPES.forEach { cfg.addAnnotatedClass(it) }
            val emf = cfg.buildSessionFactory()
            return ObjsPersistenceTestSupport(
                dataSource = dataSource,
                emf = emf,
                flyway = flyway,
                catalogTicker = catalogTicker,
                catalogProperties = catalogProperties,
            )
        }

        private val MANAGED_TYPES = listOf(
            EntityRecord::class.java,
            EdgeRecord::class.java,
            GraphRecord::class.java,
            GraphMembershipRecord::class.java,
            SchemaCatalogRecord::class.java,
            AllowedEdgeRuleRecord::class.java,
            SeedLedgerRecord::class.java,
            EntityVersionRecord::class.java,
            GraphVersionRecord::class.java,
            EdgeVersionRecord::class.java,
            GraphVersionMemberRecord::class.java,
            GraphVersionEdgeRecord::class.java,
        )
    }
}

private class DriverManagerDataSource(
    private val url: String,
    private val user: String,
    private val password: String,
) : DataSource {
    override fun getConnection(): Connection = DriverManager.getConnection(url, user, password)
    override fun getConnection(username: String?, password: String?): Connection =
        DriverManager.getConnection(url, username, password)
    override fun getLogWriter(): PrintWriter? = null
    override fun setLogWriter(out: PrintWriter?) {}
    override fun setLoginTimeout(seconds: Int) {}
    override fun getLoginTimeout(): Int = 0
    override fun getParentLogger(): Logger = Logger.getLogger("global")
    override fun isWrapperFor(iface: Class<*>?): Boolean = false
    override fun <T : Any> unwrap(iface: Class<T>?): T = throw SQLException("not a wrapper")
}
