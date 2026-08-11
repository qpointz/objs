package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

/**
 * Canonical objs schema (C-13 final form).
 *
 * Creates the pool + graphs model in one step — no intermediate rename / backfill history.
 * PostgreSQL uses JSONB (+ GIN on entity annotations); H2 uses JSON.
 */
@Suppress("unused", "ClassName")
class V1__bom_schema : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        val postgres = connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        val json = if (postgres) "JSONB" else "JSON"

        // --- Entity pool -------------------------------------------------------
        exec(
            connection,
            """
            CREATE TABLE bom_entity (
                id UUID NOT NULL PRIMARY KEY,
                type VARCHAR(255) NOT NULL,
                schema_version VARCHAR(64) NOT NULL,
                payload $json NOT NULL,
                annotations $json NOT NULL
            )
            """.trimIndent(),
        )
        exec(
            connection,
            "CREATE INDEX idx_bom_entity_type_schema_version ON bom_entity (type, schema_version)",
        )
        if (postgres) {
            exec(
                connection,
                """
                CREATE INDEX idx_bom_entity_annotations_gin
                    ON bom_entity USING GIN (annotations jsonb_path_ops)
                """.trimIndent(),
            )
        }

        // --- Graph header ------------------------------------------------------
        exec(
            connection,
            """
            CREATE TABLE bom_graph (
                id UUID NOT NULL PRIMARY KEY,
                annotations $json NOT NULL
            )
            """.trimIndent(),
        )

        // --- Membership M2M (entity ∈ 0..n graphs) -----------------------------
        exec(
            connection,
            """
            CREATE TABLE bom_graph_entity (
                graph_id UUID NOT NULL,
                entity_id UUID NOT NULL,
                PRIMARY KEY (graph_id, entity_id),
                CONSTRAINT fk_bom_graph_entity_graph
                    FOREIGN KEY (graph_id) REFERENCES bom_graph (id) ON DELETE CASCADE,
                CONSTRAINT fk_bom_graph_entity_entity
                    FOREIGN KEY (entity_id) REFERENCES bom_entity (id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        exec(connection, "CREATE INDEX idx_bom_graph_entity_entity ON bom_graph_entity (entity_id)")

        // --- Graph-local edges -------------------------------------------------
        exec(
            connection,
            """
            CREATE TABLE bom_graph_edge (
                id UUID NOT NULL PRIMARY KEY,
                graph_id UUID NOT NULL,
                source_id UUID NOT NULL,
                target_id UUID NOT NULL,
                role VARCHAR(255) NOT NULL,
                type VARCHAR(255),
                schema_version VARCHAR(64),
                properties $json,
                CONSTRAINT fk_bom_graph_edge_graph
                    FOREIGN KEY (graph_id) REFERENCES bom_graph (id) ON DELETE CASCADE,
                CONSTRAINT fk_bom_graph_edge_source
                    FOREIGN KEY (source_id) REFERENCES bom_entity (id),
                CONSTRAINT fk_bom_graph_edge_target
                    FOREIGN KEY (target_id) REFERENCES bom_entity (id)
            )
            """.trimIndent(),
        )
        exec(connection, "CREATE INDEX idx_bom_graph_edge_graph ON bom_graph_edge (graph_id)")
        exec(connection, "CREATE INDEX idx_bom_graph_edge_source ON bom_graph_edge (source_id)")
        exec(connection, "CREATE INDEX idx_bom_graph_edge_target ON bom_graph_edge (target_id)")
        exec(connection, "CREATE INDEX idx_bom_graph_edge_role ON bom_graph_edge (role)")

        // --- Schema catalogs ---------------------------------------------------
        exec(
            connection,
            """
            CREATE TABLE bom_entity_schema (
                type VARCHAR(255) NOT NULL,
                version VARCHAR(64) NOT NULL,
                definition_doc $json NOT NULL,
                usages $json NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT pk_bom_entity_schema PRIMARY KEY (type, version)
            )
            """.trimIndent(),
        )
        exec(
            connection,
            """
            CREATE TABLE bom_edge_schema (
                source_type VARCHAR(255) NOT NULL,
                role VARCHAR(255) NOT NULL,
                target_type VARCHAR(255) NOT NULL,
                properties_policy VARCHAR(32) NOT NULL DEFAULT 'NONE',
                empty_properties_allowed BOOLEAN NOT NULL DEFAULT TRUE,
                properties_schema_type VARCHAR(255),
                properties_schema_version VARCHAR(64),
                cardinality VARCHAR(32) NOT NULL DEFAULT 'UNSPECIFIED',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT pk_bom_edge_schema PRIMARY KEY (source_type, role, target_type)
            )
            """.trimIndent(),
        )

        // --- Seed ledger -------------------------------------------------------
        exec(
            connection,
            """
            CREATE TABLE bom_seed_ledger (
                seed_key VARCHAR(512) NOT NULL,
                last_success_fingerprint VARCHAR(128),
                last_success_at TIMESTAMP,
                last_attempt_fingerprint VARCHAR(128),
                last_attempt_status VARCHAR(32) NOT NULL,
                last_attempt_at TIMESTAMP NOT NULL,
                last_error TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT pk_bom_seed_ledger PRIMARY KEY (seed_key)
            )
            """.trimIndent(),
        )
        exec(connection, "CREATE INDEX idx_bom_seed_ledger_status ON bom_seed_ledger (last_attempt_status)")
    }

    private fun exec(connection: Connection, sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }
}
