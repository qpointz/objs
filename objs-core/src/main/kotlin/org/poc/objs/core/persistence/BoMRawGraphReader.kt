package org.poc.objs.core.persistence

import org.poc.objs.core.match.BoMEdgeMatchCandidate
import org.poc.objs.core.match.BoMEntityMatchCandidate
import org.poc.objs.core.match.BoMMatchExpression
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.match.BoMPushableMatcher
import org.poc.objs.core.typed.PayloadMapper
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

/**
 * Fetch-sized JDBC reader that keeps JSON columns as raw strings until accessed.
 */
@Component
class BoMRawGraphReader(
    private val dataSource: DataSource,
) {
    private val postgres = lazy(LazyThreadSafetyMode.PUBLICATION) {
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    val isPostgres: Boolean get() = postgres.value

    fun select(matcher: BoMMatcher): Pair<List<BoMEntityMatchCandidate>, List<BoMEdgeMatchCandidate>> {
        val pushable = matcher as? BoMPushableMatcher
        if (pushable != null && isPostgres) {
            val compiled = compilePostgres(pushable.expression)
            if (compiled != null) {
                return selectPushdown(compiled)
            }
        }
        return selectScan(matcher)
    }

    private fun selectPushdown(compiled: CompiledPostgresPredicate): Pair<List<BoMEntityMatchCandidate>, List<BoMEdgeMatchCandidate>> {
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            val entities = mutableListOf<BoMEntityMatchCandidate>()
            connection.prepareStatement(
                """
                SELECT id, type, schema_version, payload::text, annotations::text
                FROM bom_graph_entity
                WHERE CAST(annotations AS jsonb) @> CAST(? AS jsonb)
                """.trimIndent(),
            ).use { statement ->
                statement.fetchSize = FETCH_SIZE
                statement.setString(1, compiled.filterJson)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        entities += readEntity(rs)
                    }
                }
            }

            val edges = mutableListOf<BoMEdgeMatchCandidate>()
            connection.prepareStatement(
                """
                WITH matched_entity AS MATERIALIZED (
                    SELECT id
                    FROM bom_graph_entity
                    WHERE CAST(annotations AS jsonb) @> CAST(? AS jsonb)
                )
                SELECT e.id, e.source_id, e.target_id, e.role, e.type, e.schema_version, e.properties::text
                FROM bom_graph_edge e
                JOIN matched_entity source ON source.id = e.source_id
                JOIN matched_entity target ON target.id = e.target_id
                """.trimIndent(),
            ).use { statement ->
                statement.fetchSize = FETCH_SIZE
                statement.setString(1, compiled.filterJson)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        edges += readEdge(rs)
                    }
                }
            }
            return entities to edges
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    private fun selectScan(matcher: BoMMatcher): Pair<List<BoMEntityMatchCandidate>, List<BoMEdgeMatchCandidate>> {
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            val selected = mutableListOf<BoMEntityMatchCandidate>()
            val selectedIds = linkedSetOf<UUID>()
            scanEntities(connection) { candidate ->
                if (matcher.matches(candidate)) {
                    selected += candidate
                    candidate.id?.let { selectedIds += it }
                }
            }

            val edges = mutableListOf<BoMEdgeMatchCandidate>()
            if (selectedIds.isNotEmpty()) {
                scanEdges(connection) { candidate ->
                    if (matcher.matchesEdge(candidate, selectedIds)) {
                        edges += candidate
                    }
                }
            }
            return selected to edges
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    private fun scanEntities(connection: Connection, consumer: (BoMEntityMatchCandidate) -> Unit) {
        connection.prepareStatement(
            """
            SELECT id, type, schema_version, payload, annotations
            FROM bom_graph_entity
            """.trimIndent(),
        ).use { statement ->
            configureFetch(statement)
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    consumer(readEntity(rs))
                }
            }
        }
    }

    private fun scanEdges(connection: Connection, consumer: (BoMEdgeMatchCandidate) -> Unit) {
        connection.prepareStatement(
            """
            SELECT id, source_id, target_id, role, type, schema_version, properties
            FROM bom_graph_edge
            """.trimIndent(),
        ).use { statement ->
            configureFetch(statement)
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    consumer(readEdge(rs))
                }
            }
        }
    }

    private fun configureFetch(statement: PreparedStatement) {
        statement.fetchSize = FETCH_SIZE
    }

    private fun readEntity(rs: ResultSet): BoMEntityMatchCandidate =
        RawEntityCandidate(
            id = rs.getObject("id", UUID::class.java),
            type = rs.getString("type"),
            schemaVersion = rs.getString("schema_version"),
            payloadJson = rs.getString("payload"),
            annotationsJson = rs.getString("annotations"),
        )

    private fun readEdge(rs: ResultSet): BoMEdgeMatchCandidate =
        RawEdgeCandidate(
            id = rs.getObject("id", UUID::class.java),
            source = rs.getObject("source_id", UUID::class.java),
            target = rs.getObject("target_id", UUID::class.java),
            role = rs.getString("role"),
            type = rs.getString("type"),
            schemaVersion = rs.getString("schema_version"),
            propertiesJson = rs.getString("properties"),
        )

    private fun compilePostgres(expression: BoMMatchExpression): CompiledPostgresPredicate? {
        val filter = linkedMapOf<String, String>()
        if (!collectAnnotationEquals(expression, filter)) {
            return null
        }
        val filterJson = PayloadMapper.mapper.writeValueAsString(filter)
        return CompiledPostgresPredicate(filterJson)
    }

    private fun collectAnnotationEquals(
        expression: BoMMatchExpression,
        out: MutableMap<String, String>,
    ): Boolean = when (expression) {
        is BoMMatchExpression.AnnotationEquals -> {
            out[expression.key] = expression.value
            true
        }
        is BoMMatchExpression.And -> expression.expressions.all { collectAnnotationEquals(it, out) }
    }

    private data class CompiledPostgresPredicate(val filterJson: String)

    private class RawEntityCandidate(
        override val id: UUID?,
        override val type: String,
        override val schemaVersion: String,
        payloadJson: String?,
        annotationsJson: String?,
    ) : BoMEntityMatchCandidate {
        override val annotations: MutableMap<String, String> = LazyJsonMap.annotations(annotationsJson)
        override val payload: MutableMap<String, Any?> = LazyJsonMap.payload(payloadJson)
    }

    private class RawEdgeCandidate(
        override val id: UUID?,
        override val source: UUID,
        override val target: UUID,
        override val role: String,
        override val type: String?,
        override val schemaVersion: String?,
        propertiesJson: String?,
    ) : BoMEdgeMatchCandidate {
        override val properties: MutableMap<String, Any?>? = LazyJsonMap.properties(propertiesJson)
    }

    companion object {
        const val FETCH_SIZE = 500
    }
}
