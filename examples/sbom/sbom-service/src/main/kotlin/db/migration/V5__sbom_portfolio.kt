package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

/**
 * Portfolio taxonomy: portfolio → subject-area tree → application membership (G-P10).
 * Membership pins applications only (no version). App at most once per portfolio.
 */
@Suppress("unused", "ClassName")
class V5__sbom_portfolio : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        val postgres = connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        val ts = if (postgres) "TIMESTAMPTZ" else "TIMESTAMP WITH TIME ZONE"

        exec(
            connection,
            """
            CREATE TABLE sbom_portfolio (
                id UUID NOT NULL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description VARCHAR(2048),
                created_at $ts NOT NULL,
                updated_at $ts NOT NULL,
                CONSTRAINT uq_sbom_portfolio_name UNIQUE (name)
            )
            """.trimIndent(),
        )

        exec(
            connection,
            """
            CREATE TABLE sbom_portfolio_node (
                id UUID NOT NULL PRIMARY KEY,
                portfolio_id UUID NOT NULL,
                parent_id UUID,
                name VARCHAR(255) NOT NULL,
                sort_order INT NOT NULL DEFAULT 0,
                CONSTRAINT fk_sbom_portfolio_node_portfolio
                    FOREIGN KEY (portfolio_id) REFERENCES sbom_portfolio (id) ON DELETE CASCADE,
                CONSTRAINT fk_sbom_portfolio_node_parent
                    FOREIGN KEY (parent_id) REFERENCES sbom_portfolio_node (id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        exec(
            connection,
            "CREATE INDEX idx_sbom_portfolio_node_portfolio ON sbom_portfolio_node (portfolio_id, parent_id)",
        )

        exec(
            connection,
            """
            CREATE TABLE sbom_portfolio_membership (
                id UUID NOT NULL PRIMARY KEY,
                portfolio_id UUID NOT NULL,
                node_id UUID,
                application_id UUID NOT NULL,
                CONSTRAINT uq_sbom_portfolio_membership_app UNIQUE (portfolio_id, application_id),
                CONSTRAINT fk_sbom_portfolio_membership_portfolio
                    FOREIGN KEY (portfolio_id) REFERENCES sbom_portfolio (id) ON DELETE CASCADE,
                CONSTRAINT fk_sbom_portfolio_membership_node
                    FOREIGN KEY (node_id) REFERENCES sbom_portfolio_node (id) ON DELETE CASCADE,
                CONSTRAINT fk_sbom_portfolio_membership_app
                    FOREIGN KEY (application_id) REFERENCES sbom_application (id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        exec(
            connection,
            "CREATE INDEX idx_sbom_portfolio_membership_node ON sbom_portfolio_membership (node_id)",
        )
    }

    private fun exec(connection: Connection, sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }
}
