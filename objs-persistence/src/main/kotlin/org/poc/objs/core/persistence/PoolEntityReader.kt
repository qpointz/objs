package org.poc.objs.core.persistence

import org.poc.objs.api.match.CandidateSource
import org.poc.objs.api.match.ChainedMatcher
import org.poc.objs.api.match.EntityCandidateBackend
import org.poc.objs.api.match.EntityColumnProjection
import org.poc.objs.api.match.EntityMatchCandidate
import org.poc.objs.api.match.EntitySelectionPlan
import org.poc.objs.api.match.Matcher
import org.poc.objs.api.match.ObjExprPushdown
import org.poc.objs.api.validation.ValidationException
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.api.validation.ValidationResult
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.core.typed.DefaultPayloadMapper
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Fetch-sized JDBC reader over the entity **pool** (`objs_entity`).
 *
 * Used by [GraphStore.selectFromPool] for bare `obj-expr` (includes orphans). Equality/`&&`
 * pushdown uses column predicates (`type = ?`, …) and Postgres `annotations`/`payload` `@>`;
 * otherwise local JEXL over a scan. Does **not** load edges (edges are graph-local).
 */
class PoolEntityReader(
    private val uow: UnitOfWork,
) {
    private var postgres: Boolean? = null

    private fun isPostgresBackend(): Boolean {
        if (postgres == null) {
            postgres = uow.connection().metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        }
        return postgres!!
    }

    val isPostgres: Boolean get() = isPostgresBackend()

    @Volatile
    private var activeProjection: EntityColumnProjection =
        EntityColumnProjection(includePayload = false, includeAnnotations = true)

    private val backend: EntityCandidateBackend = JdbcEntityCandidateBackend()

    fun selectEntities(matcher: Matcher): List<EntityMatchCandidate> {
        val deadlineNanos = System.nanoTime() + SELECTION_BUDGET_NANOS
        val checkBudget = { checkBudget(deadlineNanos) }
        val stages = flattenStages(matcher)
        val plan = EntitySelectionPlan.resolve(stages, backend)
        return selectEntities(plan, checkBudget)
    }

    private fun selectEntities(
        plan: EntitySelectionPlan,
        checkBudget: () -> Unit,
    ): List<EntityMatchCandidate> {
        // Pool query always returns full entities; keep JSON columns in the candidate SELECT.
        val projection = EntityColumnProjection(includePayload = true, includeAnnotations = true)
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
            activeProjection = EntityColumnProjection(includePayload = true, includeAnnotations = true)
        }
    }

    private fun scanEntities(
        connection: Connection,
        projection: EntityColumnProjection,
        consumer: (EntityMatchCandidate) -> Unit,
    ) {
        connection.prepareStatement(entitySelectSql(projection, postgresCast = isPostgresBackend())).use { statement ->
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
        projection: EntityColumnProjection,
    ): List<EntityMatchCandidate> {
        val entities = mutableListOf<EntityMatchCandidate>()
        val connection = uow.connection()
        for (chunk in ids.distinct().chunked(IN_CHUNK_SIZE)) {
            val placeholders = chunk.joinToString(",") { "?" }
            connection.prepareStatement(
                """
                ${entitySelectSql(projection, postgresCast = isPostgresBackend())}
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
        return entities
    }

    private fun selectObjExprPushdown(
        plan: ObjExprPushdown,
        projection: EntityColumnProjection,
    ): List<EntityMatchCandidate> {
        if (plan.isUnsatisfiable) {
            return emptyList()
        }
        val groupSql = ArrayList<String>()
        val params = ArrayList<Any?>()
        for (group in plan.dnf) {
            val where = ArrayList<String>()
            group.typeEquals?.let {
                where += "type = ?"
                params += it
            }
            for (v in group.typeNotEquals) {
                where += "type <> ?"
                params += v
            }
            group.idEquals?.let {
                where += "id = ?"
                params += it
            }
            for (v in group.idNotEquals) {
                where += "id <> ?"
                params += v
            }
            group.schemaVersionEquals?.let {
                where += "schema_version = ?"
                params += it
            }
            for (v in group.schemaVersionNotEquals) {
                where += "schema_version <> ?"
                params += v
            }
            if (group.annotationEquals.isNotEmpty()) {
                where += "annotations @> CAST(? AS jsonb)"
                params += DefaultPayloadMapper.mapper.writeValueAsString(group.annotationEquals)
            }
            for ((key, value) in group.annotationNotEquals) {
                where += "(annotations ->> ?) IS DISTINCT FROM ?"
                params += key
                params += value
            }
            if (group.payloadEquals.isNotEmpty()) {
                where += "payload @> CAST(? AS jsonb)"
                params += DefaultPayloadMapper.mapper.writeValueAsString(group.payloadEquals)
            }
            for ((key, value) in group.payloadNotEquals) {
                where += "${payloadTextExpr()} IS DISTINCT FROM ?"
                params += key
                params += value
            }
            for ((key, value) in group.payloadGt) {
                where += "${payloadTextExpr()} > ?"
                params += key
                params += value
            }
            for ((key, value) in group.payloadGe) {
                where += "${payloadTextExpr()} >= ?"
                params += key
                params += value
            }
            for ((key, value) in group.payloadLt) {
                where += "${payloadTextExpr()} < ?"
                params += key
                params += value
            }
            for ((key, value) in group.payloadLe) {
                where += "${payloadTextExpr()} <= ?"
                params += key
                params += value
            }
            for ((key, prefix) in group.payloadPrefix) {
                where += "${payloadTextExpr()} LIKE ?"
                params += key
                params += sqlLikePrefix(prefix)
            }
            require(where.isNotEmpty()) { "obj-expr AND-group WHERE must not be empty" }
            groupSql += "(${where.joinToString(" AND ")})"
        }
        require(groupSql.isNotEmpty()) { "obj-expr pushdown WHERE must not be empty" }
        val connection = uow.connection()
        val entities = mutableListOf<EntityMatchCandidate>()
        connection.prepareStatement(
            """
            ${entitySelectSql(projection, postgresCast = isPostgresBackend())}
            WHERE ${groupSql.joinToString(" OR ")}
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
    }

    private fun payloadTextExpr(): String = "(payload ->> ?)"

    private fun entitySelectSql(projection: EntityColumnProjection, postgresCast: Boolean): String {
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
            FROM objs_entity
        """.trimIndent()
    }

    private fun readEntity(rs: ResultSet, projection: EntityColumnProjection): EntityMatchCandidate =
        RawEntityCandidate(
            id = rs.getObject("id", UUID::class.java),
            type = rs.getString("type"),
            schemaVersion = rs.getString("schema_version"),
            payloadJson = if (projection.includePayload) rs.getString("payload") else null,
            annotationsJson = if (projection.includeAnnotations) rs.getString("annotations") else null,
            payloadDeferred = !projection.includePayload,
            annotationsDeferred = !projection.includeAnnotations,
        )

    private fun flattenStages(matcher: Matcher): List<Matcher> =
        when (matcher) {
            is ChainedMatcher -> matcher.matchers.flatMap(::flattenStages)
            else -> listOf(matcher)
        }

    private fun checkBudget(deadlineNanos: Long) {
        if (System.nanoTime() > deadlineNanos) {
            throw ValidationException(
                "pool-query",
                ValidationResult.of(
                    ValidationIssue(
                        code = "MATCHER_SELECTION_TIMEOUT",
                        message = "Pool entity selection exceeded the ${SELECTION_BUDGET_MINUTES}-minute budget",
                    ),
                ),
            )
        }
    }

    private inner class JdbcEntityCandidateBackend : EntityCandidateBackend {
        override val isPostgres: Boolean
            get() = this@PoolEntityReader.isPostgres

        override fun allEntitiesSource(): CandidateSource =
            CandidateSource { checkBudget ->
                val selected = mutableListOf<EntityMatchCandidate>()
                scanEntities(uow.connection(), activeProjection) { candidate ->
                    checkBudget()
                    selected += candidate
                }
                selected
            }

        override fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): CandidateSource? {
            if (!isPostgres || disjuncts.isEmpty()) {
                return null
            }
            // Pool reader only uses obj-expr pushdown; annotation-only OR sources are unused here.
            return null
        }

        override fun entityIdsSource(ids: List<UUID>): CandidateSource {
            if (ids.isEmpty()) {
                return CandidateSource { emptyList() }
            }
            return CandidateSource { checkBudget ->
                checkBudget()
                selectEntitiesByIds(ids, activeProjection)
            }
        }

        override fun objExprPushdownSource(plan: ObjExprPushdown): CandidateSource? {
            // JSONB @> requires Postgres; scalar payload compares / prefix pushdown is Postgres-first.
            if (!isPostgres && (plan.needsJsonbContainment || plan.needsPayloadScalarPredicates)) {
                return null
            }
            return CandidateSource { checkBudget ->
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

        internal fun sqlLikePrefix(prefix: String): String =
            prefix
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%"
    }
}

/**
 * Raw JDBC entity candidate. JSON columns may be deferred ([payloadDeferred] /
 * [annotationsDeferred]) until survivor hydration.
 */
class RawEntityCandidate(
    override val id: UUID?,
    override val type: String,
    override val schemaVersion: String,
    payloadJson: String?,
    annotationsJson: String?,
    private val payloadDeferred: Boolean = false,
    private val annotationsDeferred: Boolean = false,
) : EntityMatchCandidate {
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
