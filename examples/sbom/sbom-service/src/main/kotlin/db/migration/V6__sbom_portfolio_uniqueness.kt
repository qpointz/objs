package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

/**
 * Portfolio uniqueness metadata, category description, optional version pin, drop portfolio-wide unique app.
 */
@Suppress("unused", "ClassName")
class V6__sbom_portfolio_uniqueness : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        exec(connection, "ALTER TABLE sbom_portfolio ADD COLUMN uniqueness VARCHAR(32) NOT NULL DEFAULT 'UNIQUE_APP'")
        exec(connection, "ALTER TABLE sbom_portfolio ADD COLUMN origin VARCHAR(32) NOT NULL DEFAULT 'MANUAL'")
        exec(connection, "ALTER TABLE sbom_portfolio ADD COLUMN source VARCHAR(255)")
        exec(connection, "ALTER TABLE sbom_portfolio_node ADD COLUMN description VARCHAR(2048)")
        exec(connection, "ALTER TABLE sbom_portfolio_membership ADD COLUMN version_id UUID")
        exec(
            connection,
            """
            ALTER TABLE sbom_portfolio_membership
                ADD CONSTRAINT fk_sbom_portfolio_membership_version
                    FOREIGN KEY (version_id) REFERENCES sbom_application_version (id) ON DELETE SET NULL
            """.trimIndent(),
        )
        exec(connection, "ALTER TABLE sbom_portfolio_membership DROP CONSTRAINT uq_sbom_portfolio_membership_app")
    }

    private fun exec(connection: Connection, sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }
}
