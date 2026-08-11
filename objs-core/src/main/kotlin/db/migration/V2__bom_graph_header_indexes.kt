package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

/**
 * Graph-header annotation GIN (parity with [V1__bom_schema] entity GIN) plus composite
 * edge indexes for graph-scoped adjacency lookups.
 */
@Suppress("unused", "ClassName")
class V2__bom_graph_header_indexes : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        val postgres = connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)

        if (postgres) {
            exec(
                connection,
                """
                CREATE INDEX idx_bom_graph_annotations_gin
                    ON bom_graph USING GIN (annotations jsonb_path_ops)
                """.trimIndent(),
            )
        }

        // Graph-scoped endpoint lookups (complement single-column source/target indexes).
        exec(
            connection,
            "CREATE INDEX idx_bom_graph_edge_graph_source ON bom_graph_edge (graph_id, source_id)",
        )
        exec(
            connection,
            "CREATE INDEX idx_bom_graph_edge_graph_target ON bom_graph_edge (graph_id, target_id)",
        )
    }

    private fun exec(connection: Connection, sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }
}
