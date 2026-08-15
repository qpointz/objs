package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

/**
 * SBOM product inventory: applications + one edit-draft row per app (G-A6 / G-P3).
 * Version and portfolio tables land in later WIs.
 */
@Suppress("unused", "ClassName")
class V3__sbom_application_inventory : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        val postgres = connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        val ts = if (postgres) "TIMESTAMPTZ" else "TIMESTAMP WITH TIME ZONE"

        exec(
            connection,
            """
            CREATE TABLE sbom_application (
                id UUID NOT NULL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description VARCHAR(2048),
                created_at $ts NOT NULL,
                updated_at $ts NOT NULL,
                CONSTRAINT uq_sbom_application_name UNIQUE (name)
            )
            """.trimIndent(),
        )
        exec(
            connection,
            "CREATE INDEX idx_sbom_application_name ON sbom_application (name)",
        )

        exec(
            connection,
            """
            CREATE TABLE sbom_application_draft (
                id UUID NOT NULL PRIMARY KEY,
                application_id UUID NOT NULL,
                graph_id UUID NOT NULL,
                CONSTRAINT uq_sbom_application_draft_app UNIQUE (application_id),
                CONSTRAINT uq_sbom_application_draft_graph UNIQUE (graph_id),
                CONSTRAINT fk_sbom_application_draft_app
                    FOREIGN KEY (application_id) REFERENCES sbom_application (id) ON DELETE CASCADE,
                CONSTRAINT fk_sbom_application_draft_graph
                    FOREIGN KEY (graph_id) REFERENCES bom_graph (id)
            )
            """.trimIndent(),
        )
    }

    private fun exec(connection: Connection, sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }
}
