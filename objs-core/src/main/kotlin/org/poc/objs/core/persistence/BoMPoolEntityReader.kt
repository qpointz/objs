package org.poc.objs.core.persistence

import org.poc.objs.core.match.BoMCandidateSource
import org.poc.objs.core.match.BoMChainedMatcher
import org.poc.objs.core.match.BoMEntityCandidateBackend
import org.poc.objs.core.match.BoMEntityColumnProjection
import org.poc.objs.core.match.BoMEntityMatchCandidate
import org.poc.objs.core.match.BoMEntitySelectionPlan
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.match.BoMObjExprPushdown
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
 * Fetch-sized JDBC reader over the entity **pool** (`bom_entity`).
 *
 * Used by [BoMGraphStore.selectFromPool] for bare `obj-expr` (includes orphans). Equality/`&&`
 * pushdown uses column predicates (`type = ?`, …) and Postgres `annotations`/`payload` `@>`;
 * otherwise local JEXL over a scan. Does **not** load edges (edges are graph-local).
 */
@Component
class BoMPoolEntityReader(
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

    fun selectEntities(matcher: BoMMatcher): List<BoMEntityMatchCandidate> {
        val deadlineNanos = System.nanoTime() + SELECTION_BUDGET_NANOS
        val checkBudget = { checkBudget(deadlineNanos) }
        val stages = flattenStages(matcher)
        val plan = BoMEntitySelectionPlan.resolve(stages, backend)
        return selectEntities(plan, checkBudget)
    }

    private fun selectEntities(
        plan: BoMEntitySelectionPlan,
        checkBudget: () -> Unit,
    ): List<BoMEntityMatchCandidate> {
        // Pool query always returns full entities; keep JSON columns in the candidate SELECT.
        val projection = BoMEntityColumnProjection(includePayload = true, includeAnnotations = true)
        activeProjection = projection
        try {
            val collected = plan.source.collect(checkBudget)
            return if (plan.filters.isEmpty()) {
                collected
            } else {
                collected.filter { candidate ->
                    checkBudget()
                    plan.filters.all { stage -> stage.matches(candidate) }
                }
            }
        } finally {
            activeProjection = BoMEntityColumnProjection(includePayload = true, includeAnnotations = true)
        }
    }

    private fun scanEntities(
        connection: Connection,
        projection: BoMEntityColumnProjection,
        consumer: (BoMEntityMatchCandidate) -> Unit,
    ) {
        connection.prepareStatement(entitySelectSql(projection, postgresCast = isPostgres)).use { statement ->
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

    private fun selectEntitiesByIds(
        ids: List<UUID>,
        projection: BoMEntityColumnProjection,
    ): List<BoMEntityMatchCandidate> {
        val entities = mutableListOf<BoMEntityMatchCandidate>()
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            for (chunk in ids.distinct().chunked(IN_CHUNK_SIZE)) {
                val placeholders = chunk.joinToString(",") { "?" }
                connection.prepareStatement(
                    """
                    ${entitySelectSql(projection, postgresCast = isPostgres)}
                    WHERE id IN ($placeholders)
                    """.trimIndent(),
                ).use { statement ->
                    statement.fetchSize = FETCH_SIZE
                    chunk.forEachIndexed { index, id -> statement.setObject(index + 1, id) }
                    statement.executeQuery().use { rs ->
                        while (rs.next()) {
                            entities += readEntity(rs, projection)
                        }
                    }
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
        return entities
    }

    private fun selectObjExprPushdown(
        plan: BoMObjExprPushdown,
        projection: BoMEntityColumnProjection,
    ): List<BoMEntityMatchCandidate> {
        val where = mutableListOf<String>()
        val params = mutableListOf<Any?>()
        plan.typeEquals?.let {
            where += "type = ?"
            params += it
        }
        plan.idEquals?.let {
            where += "id = ?"
            params += it
        }
        plan.schemaVersionEquals?.let {
            where += "schema_version = ?"
            params += it
        }
        if (plan.annotationEquals.isNotEmpty()) {
            where += "annotations @> CAST(? AS jsonb)"
            params += PayloadMapper.mapper.writeValueAsString(plan.annotationEquals)
        }
        if (plan.payloadEquals.isNotEmpty()) {
            where += "payload @> CAST(? AS jsonb)"
            params += PayloadMapper.mapper.writeValueAsString(plan.payloadEquals)
        }
        require(where.isNotEmpty()) { "obj-expr pushdown WHERE must not be empty" }
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            val entities = mutableListOf<BoMEntityMatchCandidate>()
            connection.prepareStatement(
                """
                ${entitySelectSql(projection, postgresCast = isPostgres)}
                WHERE ${where.joinToString(" AND ")}
                """.trimIndent(),
            ).use { statement ->
                statement.fetchSize = FETCH_SIZE
                params.forEachIndexed { index, value ->
                    when (value) {
                        is UUID -> statement.setObject(index + 1, value)
                        else -> statement.setString(index + 1, value as String)
                    }
                }
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
            FROM bom_entity
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

    private fun flattenStages(matcher: BoMMatcher): List<BoMMatcher> =
        when (matcher) {
            is BoMChainedMatcher -> matcher.matchers.flatMap(::flattenStages)
            else -> listOf(matcher)
        }

    private fun checkBudget(deadlineNanos: Long) {
        if (System.nanoTime() > deadlineNanos) {
            throw BoMValidationException(
                "pool-query",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_SELECTION_TIMEOUT",
                        message = "Pool entity selection exceeded the ${SELECTION_BUDGET_MINUTES}-minute budget",
                    ),
                ),
            )
        }
    }

    private inner class JdbcEntityCandidateBackend : BoMEntityCandidateBackend {
        override val isPostgres: Boolean
            get() = this@BoMPoolEntityReader.isPostgres

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
            // Pool reader only uses obj-expr pushdown; annotation-only OR sources are unused here.
            return null
        }

        override fun entityIdsSource(ids: List<UUID>): BoMCandidateSource {
            if (ids.isEmpty()) {
                return BoMCandidateSource { emptyList() }
            }
            return BoMCandidateSource { checkBudget ->
                checkBudget()
                selectEntitiesByIds(ids, activeProjection)
            }
        }

        override fun objExprPushdownSource(plan: BoMObjExprPushdown): BoMCandidateSource? {
            val needsJsonb = plan.annotationEquals.isNotEmpty() || plan.payloadEquals.isNotEmpty()
            if (needsJsonb && !isPostgres) {
                return null
            }
            return BoMCandidateSource { checkBudget ->
                checkBudget()
                selectObjExprPushdown(plan, activeProjection)
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
}
