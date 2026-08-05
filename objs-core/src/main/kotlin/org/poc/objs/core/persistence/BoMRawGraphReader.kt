package org.poc.objs.core.persistence

import org.poc.objs.core.match.BoMCandidateSource
import org.poc.objs.core.match.BoMCandidateSourceWithEdges
import org.poc.objs.core.match.BoMChainedMatcher
import org.poc.objs.core.match.BoMEdgeCandidateStrategy
import org.poc.objs.core.match.BoMEdgeMatchCandidate
import org.poc.objs.core.match.BoMEntityCandidateBackend
import org.poc.objs.core.match.BoMEntityColumnProjection
import org.poc.objs.core.match.BoMEntityMatchCandidate
import org.poc.objs.core.match.BoMEntitySelectionPlan
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.typed.PayloadMapper
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/**
 * Fetch-sized JDBC reader that keeps JSON columns as raw strings until accessed.
 *
 * Selection uses [BoMEntitySelectionPlan]: SQL/backend candidate source when the first stage
 * supplies one; otherwise **local eval** (all-entities scan + [BoMMatcher.matches]).
 * Column projection omits unused JSON during matching; survivors hydrate deferred columns
 * before [BoMEntityMatchCandidate.toDomain].
 *
 * Edges: when the entity source implements [org.poc.objs.core.match.BoMCandidateSourceWithEdges],
 * use its [org.poc.objs.core.match.BoMEdgeCandidateStrategy] (join on the same predicate).
 * Otherwise fall back to id-bounded induced-edge `IN` queries.
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

    @Volatile
    private var activeProjection: BoMEntityColumnProjection =
        BoMEntityColumnProjection(includePayload = false, includeAnnotations = true)

    private val backend: BoMEntityCandidateBackend = JdbcEntityCandidateBackend()

    fun select(matcher: BoMMatcher): Pair<List<BoMEntityMatchCandidate>, List<BoMEdgeMatchCandidate>> {
        val deadlineNanos = System.nanoTime() + SELECTION_BUDGET_NANOS
        val checkBudget = { checkBudget(deadlineNanos) }
        val stages = flattenStages(matcher)
        val plan = BoMEntitySelectionPlan.resolve(stages, backend)
        val entities = selectEntities(plan, checkBudget)
        if (entities.isEmpty()) {
            return entities to emptyList()
        }
        checkBudget()
        val selectedIds = entities.mapNotNullTo(linkedSetOf()) { it.id }
        val edges = selectEdges(plan, matcher, selectedIds, checkBudget)
        return entities to edges
    }

    private fun selectEntities(
        plan: BoMEntitySelectionPlan,
        checkBudget: () -> Unit,
    ): List<BoMEntityMatchCandidate> {
        val projection = BoMEntityColumnProjection.forPlan(plan)
        activeProjection = projection
        try {
            val collected = plan.source.collect(checkBudget)
            val filtered = if (plan.filters.isEmpty()) {
                collected
            } else {
                collected.filter { candidate ->
                    checkBudget()
                    plan.filters.all { stage -> stage.matches(candidate) }
                }
            }
            hydrateDeferredColumns(filtered, checkBudget)
            return filtered
        } finally {
            activeProjection = BoMEntityColumnProjection(includePayload = false, includeAnnotations = true)
        }
    }

    private fun selectEdges(
        plan: BoMEntitySelectionPlan,
        matcher: BoMMatcher,
        selectedIds: Set<UUID>,
        checkBudget: () -> Unit,
    ): List<BoMEdgeMatchCandidate> {
        if (selectedIds.isEmpty()) {
            return emptyList()
        }
        val candidates = plan.edgeStrategy?.collect(selectedIds, checkBudget)
            ?: loadInducedEdgesByIds(selectedIds, checkBudget)
        return candidates.filter { matcher.matchesEdge(it, selectedIds) }
    }

    private fun hydrateDeferredColumns(
        entities: List<BoMEntityMatchCandidate>,
        checkBudget: () -> Unit,
    ) {
        val raw = entities.mapNotNull { it as? BoMRawEntityCandidate }
        val missing = raw.filter { it.needsHydration() }
        if (missing.isEmpty()) {
            return
        }
        val byId = missing.associateBy { it.id!! }
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            for (chunk in byId.keys.toList().chunked(IN_CHUNK_SIZE)) {
                checkBudget()
                connection.prepareStatement(
                    """
                    SELECT id, payload${if (isPostgres) "::text" else ""}, annotations${if (isPostgres) "::text" else ""}
                    FROM bom_graph_entity
                    WHERE id IN (${chunk.joinToString(",") { "?" }})
                    """.trimIndent(),
                ).use { statement ->
                    chunk.forEachIndexed { index, id -> statement.setObject(index + 1, id) }
                    statement.executeQuery().use { rs ->
                        while (rs.next()) {
                            val id = rs.getObject("id", UUID::class.java)
                            byId[id]?.applyHydration(
                                payloadJson = rs.getString("payload"),
                                annotationsJson = rs.getString("annotations"),
                            )
                        }
                    }
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    /** Fallback when the entity source has no edge strategy (AllEntities / local eval). */
    private fun loadInducedEdgesByIds(
        selectedIds: Set<UUID>,
        checkBudget: () -> Unit,
    ): List<BoMEdgeMatchCandidate> {
        val edges = mutableListOf<BoMEdgeMatchCandidate>()
        val idList = selectedIds.toList()
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            for (chunk in idList.chunked(IN_CHUNK_SIZE)) {
                checkBudget()
                scanInducedEdges(connection, chunk) { candidate ->
                    edges += candidate
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
        return edges
    }

    /**
     * Max induced edges for entities matching any containment disjunct (union).
     * Both endpoints must satisfy the entity predicate.
     */
    private fun loadSourceInducedEdges(filterJsons: List<String>): List<BoMEdgeMatchCandidate> {
        if (filterJsons.isEmpty()) {
            return emptyList()
        }
        val sPred = filterJsons.indices.joinToString(" OR ") { "s.annotations @> CAST(? AS jsonb)" }
        val tPred = filterJsons.indices.joinToString(" OR ") { "t.annotations @> CAST(? AS jsonb)" }
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            val edges = mutableListOf<BoMEdgeMatchCandidate>()
            connection.prepareStatement(
                """
                SELECT e.id, e.source_id, e.target_id, e.role, e.type, e.schema_version, e.properties::text
                FROM bom_graph_edge e
                INNER JOIN bom_graph_entity s ON s.id = e.source_id
                INNER JOIN bom_graph_entity t ON t.id = e.target_id
                WHERE ($sPred)
                  AND ($tPred)
                """.trimIndent(),
            ).use { statement ->
                statement.fetchSize = FETCH_SIZE
                var index = 1
                repeat(2) {
                    for (json in filterJsons) {
                        statement.setString(index++, json)
                    }
                }
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        edges += readEdge(rs)
                    }
                }
            }
            return edges
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    private fun scanInducedEdges(
        connection: Connection,
        ids: List<UUID>,
        consumer: (BoMEdgeMatchCandidate) -> Unit,
    ) {
        val placeholders = ids.joinToString(",") { "?" }
        connection.prepareStatement(
            """
            SELECT id, source_id, target_id, role, type, schema_version, properties
            FROM bom_graph_edge
            WHERE source_id IN ($placeholders)
              AND target_id IN ($placeholders)
            """.trimIndent(),
        ).use { statement ->
            configureFetch(statement)
            var index = 1
            for (id in ids) {
                statement.setObject(index++, id)
            }
            for (id in ids) {
                statement.setObject(index++, id)
            }
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    consumer(readEdge(rs))
                }
            }
        }
    }

    private fun scanEntities(
        connection: Connection,
        projection: BoMEntityColumnProjection,
        consumer: (BoMEntityMatchCandidate) -> Unit,
    ) {
        connection.prepareStatement(entitySelectSql(projection, postgresCast = false)).use { statement ->
            configureFetch(statement)
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    consumer(readEntity(rs, projection))
                }
            }
        }
    }

    private fun configureFetch(statement: PreparedStatement) {
        statement.fetchSize = FETCH_SIZE
    }

    private fun selectPushdownEntities(
        filterJsons: List<String>,
        projection: BoMEntityColumnProjection,
    ): List<BoMEntityMatchCandidate> {
        if (filterJsons.isEmpty()) {
            return emptyList()
        }
        val where = filterJsons.indices.joinToString(" OR ") { "annotations @> CAST(? AS jsonb)" }
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            val entities = mutableListOf<BoMEntityMatchCandidate>()
            connection.prepareStatement(
                """
                ${entitySelectSql(projection, postgresCast = true)}
                WHERE $where
                """.trimIndent(),
            ).use { statement ->
                statement.fetchSize = FETCH_SIZE
                filterJsons.forEachIndexed { index, json -> statement.setString(index + 1, json) }
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        entities += readEntity(rs, projection)
                    }
                }
            }
            return entities
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    private fun entitySelectSql(projection: BoMEntityColumnProjection, postgresCast: Boolean): String {
        // SELECT-list casts (::text) are for JDBC string reads only and do not affect the
        // GIN-backed WHERE annotations @> … predicate on pushdown queries.
        val cast = if (postgresCast) "::text" else ""
        val columns = buildList {
            add("id")
            add("type")
            add("schema_version")
            if (projection.includePayload) add("payload$cast")
            if (projection.includeAnnotations) add("annotations$cast")
        }
        return """
            SELECT ${columns.joinToString(", ")}
            FROM bom_graph_entity
        """.trimIndent()
    }

    private fun readEntity(rs: ResultSet, projection: BoMEntityColumnProjection): BoMEntityMatchCandidate =
        BoMRawEntityCandidate(
            id = rs.getObject("id", UUID::class.java),
            type = rs.getString("type"),
            schemaVersion = rs.getString("schema_version"),
            payloadJson = if (projection.includePayload) rs.getString("payload") else null,
            annotationsJson = if (projection.includeAnnotations) rs.getString("annotations") else null,
            payloadDeferred = !projection.includePayload,
            annotationsDeferred = !projection.includeAnnotations,
        )

    private fun readEdge(rs: ResultSet): BoMEdgeMatchCandidate =
        BoMRawEdgeCandidate(
            id = rs.getObject("id", UUID::class.java),
            source = rs.getObject("source_id", UUID::class.java),
            target = rs.getObject("target_id", UUID::class.java),
            role = rs.getString("role"),
            type = rs.getString("type"),
            schemaVersion = rs.getString("schema_version"),
            propertiesJson = rs.getString("properties"),
        )

    private fun flattenStages(matcher: BoMMatcher): List<BoMMatcher> =
        when (matcher) {
            is BoMChainedMatcher -> matcher.matchers.flatMap(::flattenStages)
            else -> listOf(matcher)
        }

    private fun checkBudget(deadlineNanos: Long) {
        if (System.nanoTime() > deadlineNanos) {
            throw BoMValidationException(
                "graph-query",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_SELECTION_TIMEOUT",
                        message = "Graph selection exceeded the ${SELECTION_BUDGET_MINUTES}-minute budget",
                    ),
                ),
            )
        }
    }

    private inner class JdbcEntityCandidateBackend : BoMEntityCandidateBackend {
        override val isPostgres: Boolean
            get() = this@BoMRawGraphReader.isPostgres

        override fun allEntitiesSource(): BoMCandidateSource =
            BoMCandidateSource { checkBudget ->
                val selected = mutableListOf<BoMEntityMatchCandidate>()
                val connection = DataSourceUtils.getConnection(dataSource)
                try {
                    scanEntities(connection, activeProjection) { candidate ->
                        checkBudget()
                        selected += candidate
                    }
                } finally {
                    DataSourceUtils.releaseConnection(connection, dataSource)
                }
                selected
            }

        override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource? {
            if (!isPostgres || disjuncts.isEmpty()) {
                return null
            }
            val filterJsons = disjuncts.map { PayloadMapper.mapper.writeValueAsString(it) }
            return AnnotationContainmentGraphSource(filterJsons)
        }
    }

    /**
     * Postgres entity source for annotation containment (OR of `@>` maps), with edge strategy
     * that loads the max induced edge set via joins on the same predicate (no UUID `IN` lists).
     */
    private inner class AnnotationContainmentGraphSource(
        private val filterJsons: List<String>,
    ) : BoMCandidateSourceWithEdges {
        override fun collect(checkBudget: () -> Unit): List<BoMEntityMatchCandidate> {
            checkBudget()
            return selectPushdownEntities(filterJsons, activeProjection)
        }

        override val edgeStrategy: BoMEdgeCandidateStrategy =
            BoMEdgeCandidateStrategy { selectedIds, checkBudget ->
                if (selectedIds.isEmpty()) {
                    return@BoMEdgeCandidateStrategy emptyList()
                }
                checkBudget()
                loadSourceInducedEdges(filterJsons).filter { edge ->
                    edge.source in selectedIds && edge.target in selectedIds
                }
            }
    }

    companion object {
        const val FETCH_SIZE = 500
        const val IN_CHUNK_SIZE = 500
        const val SELECTION_BUDGET_MINUTES = 3L
        val SELECTION_BUDGET_NANOS: Long = TimeUnit.MINUTES.toNanos(SELECTION_BUDGET_MINUTES)
    }
}

/**
 * Raw JDBC entity candidate. JSON columns may be deferred ([payloadDeferred] /
 * [annotationsDeferred]) until survivor hydration.
 */
class BoMRawEntityCandidate(
    override val id: UUID?,
    override val type: String,
    override val schemaVersion: String,
    payloadJson: String?,
    annotationsJson: String?,
    private val payloadDeferred: Boolean = false,
    private val annotationsDeferred: Boolean = false,
) : BoMEntityMatchCandidate {
    private var payloadJson: String? = payloadJson
    private var annotationsJson: String? = annotationsJson
    private var payloadMap: LazyJsonMap<Any?>? =
        if (payloadDeferred) null else LazyJsonMap.payload(payloadJson)
    private var annotationsMap: LazyJsonMap<String>? =
        if (annotationsDeferred) null else LazyJsonMap.annotations(annotationsJson)

    override val annotations: MutableMap<String, String>
        get() = annotationsMap ?: error("annotations column was deferred and not hydrated")

    override val payload: MutableMap<String, Any?>
        get() = payloadMap ?: error("payload column was deferred and not hydrated")

    override fun annotationsMatchAll(filter: Map<String, String>): Boolean {
        val map = annotationsMap
        return if (map != null) {
            map.stringEntriesContainAll(filter)
        } else {
            super.annotationsMatchAll(filter)
        }
    }

    fun needsHydration(): Boolean =
        id != null && ((payloadDeferred && payloadMap == null) || (annotationsDeferred && annotationsMap == null))

    fun applyHydration(payloadJson: String?, annotationsJson: String?) {
        if (payloadDeferred && payloadMap == null) {
            this.payloadJson = payloadJson
            payloadMap = LazyJsonMap.payload(payloadJson)
        }
        if (annotationsDeferred && annotationsMap == null) {
            this.annotationsJson = annotationsJson
            annotationsMap = LazyJsonMap.annotations(annotationsJson)
        }
    }

    fun payloadParseInvocations(): Int = payloadMap?.parseInvocations ?: 0

    fun annotationsParseInvocations(): Int = annotationsMap?.parseInvocations ?: 0
}

class BoMRawEdgeCandidate(
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
