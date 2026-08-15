package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import java.util.UUID

/**
 * Draft is a version status. Fingerprints are immutable graph snapshots of a version.
 */
@Suppress("unused", "ClassName")
class V7__sbom_application_version_lifecycle : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        val postgres = connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        val ts = if (postgres) "TIMESTAMPTZ" else "TIMESTAMP WITH TIME ZONE"

        exec(connection, "ALTER TABLE sbom_application_version ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'RELEASED'")
        exec(connection, "ALTER TABLE sbom_application_version ADD COLUMN version VARCHAR(255)")
        exec(connection, "ALTER TABLE sbom_application_version ADD COLUMN promoted_at $ts")
        exec(
            connection,
            """
            UPDATE sbom_application_version
            SET version = label, promoted_at = captured_at, status = 'RELEASED'
            """.trimIndent(),
        )

        connection.prepareStatement("SELECT application_id, graph_id FROM sbom_application_draft").use { select ->
            select.executeQuery().use { rs ->
                while (rs.next()) {
                    val id = UUID.randomUUID()
                    connection.prepareStatement(
                        """
                        INSERT INTO sbom_application_version
                            (id, application_id, label, captured_at, graph_id, status, version, promoted_at)
                        VALUES (?, ?, NULL, CURRENT_TIMESTAMP, ?, 'DRAFT', NULL, NULL)
                        """.trimIndent(),
                    ).use { insert ->
                        insert.setObject(1, id)
                        insert.setObject(2, rs.getObject("application_id"))
                        insert.setObject(3, rs.getObject("graph_id"))
                        insert.executeUpdate()
                    }
                }
            }
        }

        exec(connection, "DROP TABLE sbom_application_draft")

        exec(
            connection,
            """
            CREATE TABLE sbom_application_fingerprint (
                id UUID NOT NULL PRIMARY KEY,
                version_id UUID NOT NULL,
                graph_id UUID NOT NULL,
                created_at $ts NOT NULL,
                note VARCHAR(2048),
                content_sha256 VARCHAR(64) NOT NULL,
                CONSTRAINT uq_sbom_application_fingerprint_graph UNIQUE (graph_id),
                CONSTRAINT fk_sbom_application_fingerprint_version
                    FOREIGN KEY (version_id) REFERENCES sbom_application_version (id) ON DELETE CASCADE,
                CONSTRAINT fk_sbom_application_fingerprint_graph
                    FOREIGN KEY (graph_id) REFERENCES bom_graph (id)
            )
            """.trimIndent(),
        )
        exec(
            connection,
            "CREATE INDEX idx_sbom_application_fingerprint_version ON sbom_application_fingerprint (version_id, created_at DESC)",
        )
    }

    private fun exec(connection: Connection, sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }
}
