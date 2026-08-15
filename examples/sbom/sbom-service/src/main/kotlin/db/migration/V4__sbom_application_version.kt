package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

/**
 * Application versions: each row owns a distinct named graph (G-F5).
 * Latest version (R22): max(captured_at), tie-break id DESC.
 */
@Suppress("unused", "ClassName")
class V4__sbom_application_version : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        val postgres = connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        val ts = if (postgres) "TIMESTAMPTZ" else "TIMESTAMP WITH TIME ZONE"

        exec(
            connection,
            """
            CREATE TABLE sbom_application_version (
                id UUID NOT NULL PRIMARY KEY,
                application_id UUID NOT NULL,
                label VARCHAR(255),
                captured_at $ts NOT NULL,
                graph_id UUID NOT NULL,
                CONSTRAINT uq_sbom_application_version_graph UNIQUE (graph_id),
                CONSTRAINT fk_sbom_application_version_app
                    FOREIGN KEY (application_id) REFERENCES sbom_application (id) ON DELETE CASCADE,
                CONSTRAINT fk_sbom_application_version_graph
                    FOREIGN KEY (graph_id) REFERENCES bom_graph (id)
            )
            """.trimIndent(),
        )
        exec(
            connection,
            """
            CREATE INDEX idx_sbom_application_version_latest
                ON sbom_application_version (application_id, captured_at DESC, id DESC)
            """.trimIndent(),
        )
    }

    private fun exec(connection: Connection, sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }
}
