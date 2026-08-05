package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

/**
 * Promote graph JSON columns to JSONB and add a GIN index for annotation containment.
 * No-op on non-PostgreSQL databases (e.g. H2 unit tests).
 */
@Suppress("unused", "ClassName")
class V4__bom_graph_jsonb_gin : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val product = context.connection.metaData.databaseProductName
        if (!product.equals("PostgreSQL", ignoreCase = true)) {
            return
        }
        context.connection.createStatement().use { statement ->
            statement.execute(
                """
                ALTER TABLE bom_graph_entity
                    ALTER COLUMN payload TYPE jsonb USING payload::jsonb,
                    ALTER COLUMN annotations TYPE jsonb USING annotations::jsonb
                """.trimIndent(),
            )
            statement.execute(
                """
                ALTER TABLE bom_graph_edge
                    ALTER COLUMN properties TYPE jsonb USING properties::jsonb
                """.trimIndent(),
            )
            statement.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_bom_graph_entity_annotations_gin
                    ON bom_graph_entity USING GIN (annotations jsonb_path_ops)
                """.trimIndent(),
            )
        }
    }
}
