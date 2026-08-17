package db.migration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.poc.objs.sbom.domain.SemVerVersionComparer
import java.sql.Connection
import java.util.UUID

/**
 * Multi-BOM rows, tags arrays, multi-draft lineage, version_serial, fingerprint name/category.
 * Combined SBOM is ephemeral — drop version.graph_id after moving it onto the first BOM.
 */
@Suppress("unused", "ClassName")
class V8__sbom_multi_bom_versions : BaseJavaMigration() {
    private val mapper = ObjectMapper()
    private val comparer = SemVerVersionComparer()

    override fun migrate(context: Context) {
        val connection = context.connection
        val postgres = connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        val tagsType = if (postgres) "TEXT[]" else "VARCHAR ARRAY"
        val tagsDefault = if (postgres) "DEFAULT '{}'" else "DEFAULT CAST(ARRAY[] AS VARCHAR ARRAY)"
        val emptyTags = if (postgres) "'{}'" else "CAST(ARRAY[] AS VARCHAR ARRAY)"
        val jsonType = if (postgres) "jsonb" else "json"

        exec(connection, "ALTER TABLE sbom_application ADD COLUMN tags $tagsType NOT NULL $tagsDefault")
        exec(connection, "ALTER TABLE sbom_application_version ADD COLUMN tags $tagsType NOT NULL $tagsDefault")
        exec(connection, "ALTER TABLE sbom_application_version ADD COLUMN based_on_version_id UUID")
        exec(connection, "ALTER TABLE sbom_application_version ADD COLUMN based_on_fingerprint_id UUID")
        exec(connection, "ALTER TABLE sbom_application_version ADD COLUMN version_serial NUMERIC(40, 16) NOT NULL DEFAULT -1")

        exec(
            connection,
            """
            CREATE TABLE sbom_application_sbom (
                id UUID NOT NULL PRIMARY KEY,
                version_id UUID NOT NULL,
                name VARCHAR(255) NOT NULL,
                description VARCHAR(2048),
                tags $tagsType NOT NULL $tagsDefault,
                graph_id UUID NOT NULL,
                sort_order INT NOT NULL DEFAULT 0,
                CONSTRAINT uq_sbom_application_sbom_graph UNIQUE (graph_id),
                CONSTRAINT uq_sbom_application_sbom_name UNIQUE (version_id, name),
                CONSTRAINT fk_sbom_application_sbom_version
                    FOREIGN KEY (version_id) REFERENCES sbom_application_version (id) ON DELETE CASCADE,
                CONSTRAINT fk_sbom_application_sbom_graph
                    FOREIGN KEY (graph_id) REFERENCES bom_graph (id)
            )
            """.trimIndent(),
        )
        exec(connection, "CREATE INDEX idx_sbom_application_sbom_version ON sbom_application_sbom (version_id, sort_order)")

        connection.prepareStatement(
            "SELECT id, application_id, graph_id, version, status FROM sbom_application_version",
        ).use { select ->
            select.executeQuery().use { rs ->
                while (rs.next()) {
                    val versionId = rs.getObject("id")
                    val applicationId = rs.getObject("application_id")
                    val graphId = rs.getObject("graph_id")
                    val versionStr = rs.getString("version")
                    val status = rs.getString("status")
                    val bomId = UUID.randomUUID()
                    connection.prepareStatement(
                        """
                        INSERT INTO sbom_application_sbom (id, version_id, name, description, tags, graph_id, sort_order)
                        VALUES (?, ?, 'BOM', NULL, $emptyTags, ?, 0)
                        """.trimIndent(),
                    ).use { insert ->
                        insert.setObject(1, bomId)
                        insert.setObject(2, versionId)
                        insert.setObject(3, graphId)
                        insert.executeUpdate()
                    }
                    reannotateBomGraph(connection, jsonType, graphId, bomId, versionId, applicationId, status, versionStr)
                    val serial = comparer.toSerial(versionStr ?: "")
                    connection.prepareStatement(
                        "UPDATE sbom_application_version SET version_serial = ? WHERE id = ?",
                    ).use { upd ->
                        upd.setBigDecimal(1, serial)
                        upd.setObject(2, versionId)
                        upd.executeUpdate()
                    }
                }
            }
        }

        connection.prepareStatement(
            "SELECT id, application_id FROM sbom_application_version WHERE version IS NULL",
        ).use { select ->
            select.executeQuery().use { rs ->
                val taken = mutableMapOf<UUID, MutableSet<String>>()
                connection.prepareStatement(
                    "SELECT application_id, version FROM sbom_application_version WHERE version IS NOT NULL",
                ).use { existing ->
                    existing.executeQuery().use { er ->
                        while (er.next()) {
                            val appId = er.getObject("application_id") as UUID
                            taken.getOrPut(appId) { mutableSetOf() }.add(er.getString("version"))
                        }
                    }
                }
                while (rs.next()) {
                    val id = rs.getObject("id")
                    val appId = rs.getObject("application_id") as UUID
                    val used = taken.getOrPut(appId) { mutableSetOf() }
                    val ident = unusedDraftVersion(used)
                    used += ident
                    val serial = comparer.toSerial(ident)
                    connection.prepareStatement(
                        "UPDATE sbom_application_version SET version = ?, version_serial = ? WHERE id = ?",
                    ).use { upd ->
                        upd.setString(1, ident)
                        upd.setBigDecimal(2, serial)
                        upd.setObject(3, id)
                        upd.executeUpdate()
                    }
                }
            }
        }

        exec(connection, "ALTER TABLE sbom_application_version ALTER COLUMN version SET NOT NULL")

        exec(
            connection,
            """
            ALTER TABLE sbom_application_version
                ADD CONSTRAINT fk_sbom_application_version_based_on_version
                    FOREIGN KEY (based_on_version_id) REFERENCES sbom_application_version (id) ON DELETE CASCADE
            """.trimIndent(),
        )
        exec(
            connection,
            """
            ALTER TABLE sbom_application_version
                ADD CONSTRAINT fk_sbom_application_version_based_on_fingerprint
                    FOREIGN KEY (based_on_fingerprint_id) REFERENCES sbom_application_fingerprint (id) ON DELETE CASCADE
            """.trimIndent(),
        )
        exec(
            connection,
            """
            CREATE UNIQUE INDEX uq_sbom_application_version_ident
                ON sbom_application_version (application_id, version)
            """.trimIndent(),
        )
        exec(
            connection,
            "CREATE INDEX idx_sbom_application_version_serial ON sbom_application_version (application_id, status, version_serial DESC)",
        )

        exec(connection, "ALTER TABLE sbom_application_fingerprint ADD COLUMN name VARCHAR(255)")
        exec(connection, "ALTER TABLE sbom_application_fingerprint ADD COLUMN category VARCHAR(32)")
        exec(
            connection,
            """
            UPDATE sbom_application_fingerprint
            SET name = CASE
                    WHEN note IS NOT NULL AND TRIM(note) <> '' THEN LEFT(TRIM(note), 255)
                    ELSE 'Fingerprint'
                END,
                category = 'unknown'
            """.trimIndent(),
        )
        exec(connection, "ALTER TABLE sbom_application_fingerprint ALTER COLUMN name SET NOT NULL")
        exec(connection, "ALTER TABLE sbom_application_fingerprint ALTER COLUMN category SET NOT NULL")
        if (postgres) {
            exec(
                connection,
                """
                ALTER TABLE sbom_application_fingerprint
                    ADD CONSTRAINT ck_sbom_application_fingerprint_category
                        CHECK (category IN ('approval', 'history', 'unknown'))
                """.trimIndent(),
            )
        }
        // H2 MODE=PostgreSQL stores CHECK (… IN (…)) as an empty expression and then
        // rejects every insert. Domain code still restricts category to the same set.
        exec(connection, "ALTER TABLE sbom_application_fingerprint DROP COLUMN note")

        dropVersionGraphId(connection, postgres)
    }

    private fun dropVersionGraphId(connection: Connection, postgres: Boolean) {
        if (postgres) {
            exec(connection, "ALTER TABLE sbom_application_version DROP COLUMN graph_id CASCADE")
            return
        }
        exec(connection, "ALTER TABLE sbom_application_version DROP CONSTRAINT uq_sbom_application_version_graph")
        exec(connection, "ALTER TABLE sbom_application_version DROP CONSTRAINT fk_sbom_application_version_graph")
        exec(connection, "ALTER TABLE sbom_application_version DROP COLUMN graph_id")
    }

    private fun reannotateBomGraph(
        connection: Connection,
        jsonType: String,
        graphId: Any,
        bomId: UUID,
        versionId: Any,
        applicationId: Any,
        status: String?,
        versionStr: String?,
    ) {
        val json =
            connection.prepareStatement("SELECT CAST(annotations AS VARCHAR) FROM bom_graph WHERE id = ?").use { select ->
                select.setObject(1, graphId)
                select.executeQuery().use { rs ->
                    if (!rs.next()) return
                    rs.getString(1)
                }
            }
        val raw =
            if (json.isNullOrBlank()) {
                mutableMapOf<String, Any?>()
            } else {
                mapper.readValue(json, object : TypeReference<MutableMap<String, Any?>>() {})
            }
        val annotations = linkedMapOf<String, String>()
        for ((key, value) in raw) {
            if (value != null) annotations[key] = value.toString()
        }
        annotations["kind"] = "application-bom"
        annotations["bomId"] = bomId.toString()
        annotations["versionId"] = versionId.toString()
        annotations["applicationId"] = applicationId.toString()
        if (!status.isNullOrBlank()) annotations["status"] = status
        if (!versionStr.isNullOrBlank()) annotations["version"] = versionStr
        connection.prepareStatement("UPDATE bom_graph SET annotations = CAST(? AS $jsonType) WHERE id = ?").use { upd ->
            upd.setString(1, mapper.writeValueAsString(annotations))
            upd.setObject(2, graphId)
            upd.executeUpdate()
        }
    }

    private fun unusedDraftVersion(used: Set<String>): String {
        if ("0.1.0" !in used) return "0.1.0"
        var i = 1
        while (true) {
            val candidate = "0.0.0-draft.$i"
            if (candidate !in used) return candidate
            i++
        }
    }

    private fun exec(connection: Connection, sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }
}
