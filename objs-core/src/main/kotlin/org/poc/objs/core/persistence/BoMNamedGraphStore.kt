package org.poc.objs.core.persistence

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphHeader
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMResolvedGraph
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.BoMGraphException
import org.poc.objs.core.domain.FirstSeenGraphMergePolicy
import org.poc.objs.core.domain.GraphMergePolicy
import org.poc.objs.core.domain.BoMGraphListItem
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.domain.BoMPageRequest
import org.poc.objs.core.domain.BoMPagedEntities
import org.poc.objs.core.match.BoMGraphExprMatcher
import org.poc.objs.core.match.BoMGraphExprPushdown
import org.poc.objs.core.validation.BoMEntityTypeLookup
import org.poc.objs.core.validation.BoMPersistGate
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.poc.objs.core.validation.BoMValidator
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import javax.sql.DataSource

/**
 * Graph store: entity membership is M2M (`bom_graph_entity`); edges are graph-owned via
 * `bom_graph_edge.graph_id` (C-13 — no more `bom_subgraph_edges` M2M).
 */
@Service
class BoMNamedGraphStore(
    private val graphRepository: BoMGraphRepository,
    private val membershipRepository: BoMGraphMembershipRepository,
    private val entityRepository: BoMEntityRepository,
    private val edgeRepository: BoMEdgeRepository,
    private val validator: BoMValidator,
    private val dataSource: DataSource,
    @Lazy private val graphStore: BoMGraphStore,
    private val deepVersions: BoMDeepGraphVersionService,
    private val versionMemberRepository: BoMGraphVersionMemberRepository,
) {
    private val jdbc = JdbcTemplate(dataSource)
    private val objectMapper = ObjectMapper()
    private val postgres: Boolean by lazy {
        dataSource.connection.use { connection ->
            connection.metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        }
    }
    private val headerRowMapper = RowMapper { rs, _ ->
        val annotationsJson = rs.getString("annotations")
        val annotations: Map<String, String> =
            if (annotationsJson.isNullOrBlank()) {
                emptyMap()
            } else {
                objectMapper.readValue(annotationsJson, ANNOTATIONS_TYPE)
            }
        BoMGraphHeader(
            id = rs.getObject("id", UUID::class.java),
            annotations = annotations,
            createdAt = rs.getTimestamp("created_at")?.toInstant(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant(),
        )
    }

    private fun gate(): BoMPersistGate = BoMPersistGate(
        validator = validator,
        storeLookup = BoMEntityTypeLookup { id -> entityRepository.findById(id).map { it.type }.orElse(null) },
        existsEntity = { id -> entityRepository.existsById(id) },
        existsEdge = { id -> edgeRepository.existsById(id) },
    )

    @Transactional
    fun create(spec: BoMGraphSpec): BoMResolvedGraph {
        val id = spec.id ?: UUID.randomUUID()
        if (graphRepository.existsById(id)) {
            throw BoMGraphException(
                code = "GRAPH_ID_CONFLICT",
                message = "Subgraph already exists: $id",
            )
        }
        validateMembership(spec.entityIds, spec.edgeIds)
        graphRepository.save(
            BoMGraphRecord(
                id = id,
                annotations = spec.annotations.toMutableMap(),
            ),
        )
        replaceMembership(id, spec.entityIds, spec.edgeIds)
        return requireNotNull(get(id))
    }

    @Transactional
    fun updateAnnotations(id: UUID, annotations: Map<String, String>): BoMResolvedGraph {
        val existing = graphRepository.findById(id).orElse(null)
            ?: throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        existing.annotations = annotations.toMutableMap()
        existing.updatedAt = java.time.Instant.now()
        graphRepository.save(existing)
        return requireNotNull(get(id))
    }

    @Transactional
    fun replace(id: UUID, spec: BoMGraphSpec): BoMResolvedGraph {
        val existing = graphRepository.findById(id).orElse(null)
            ?: throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        validateMembership(spec.entityIds, spec.edgeIds)
        existing.annotations = spec.annotations.toMutableMap()
        existing.updatedAt = java.time.Instant.now()
        graphRepository.save(existing)
        membershipRepository.deleteByGraphId(id)
        replaceMembership(id, spec.entityIds, spec.edgeIds)
        return requireNotNull(get(id))
    }

    @Transactional
    fun delete(id: UUID) {
        if (!graphRepository.existsById(id)) {
            throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        }
        graphRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): BoMResolvedGraph? {
        val header = graphRepository.findById(id).orElse(null) ?: return null
        return resolve(header)
    }

    @Transactional(readOnly = true)
    fun list(): List<BoMGraphListItem> =
        graphRepository.findAll().map { header ->
            BoMGraphListItem(
                id = header.id,
                annotations = header.annotations.toMap(),
                entityCount = membershipRepository.countByGraphId(header.id),
                edgeCount = edgeRepository.countByGraphId(header.id),
                createdAt = header.createdAt,
                updatedAt = header.updatedAt,
            )
        }

    /**
     * G-U10 / WI-007 open-graph search over headers (no FTS). Empty [q] without [expr] returns
     * nothing — never the full catalog. When both are set, results must match **both** (AND).
     * Order is stable by id; [limit] is capped at [MAX_SEARCH_LIMIT].
     *
     * When [expr] lowers to equality/`&&` over `id` / `a.*` and the backend is PostgreSQL,
     * candidates are loaded via PK / `annotations @>` (GIN) instead of scanning all headers.
     */
    @Transactional(readOnly = true)
    fun search(q: String? = null, expr: String? = null, limit: Int = DEFAULT_SEARCH_LIMIT): List<BoMGraphHeader> {
        val query = q?.trim().orEmpty()
        val expression = expr?.trim().orEmpty()
        if (query.isEmpty() && expression.isEmpty()) {
            return emptyList()
        }
        val capped = when {
            limit < 1 -> DEFAULT_SEARCH_LIMIT
            else -> minOf(limit, MAX_SEARCH_LIMIT)
        }
        val matcher = if (expression.isNotEmpty()) BoMGraphExprMatcher(expression) else null
        val sqlLimit = if (query.isEmpty()) capped else null
        val candidates = headersForSearch(matcher, sqlLimit)
        return candidates
            .asSequence()
            .filter { header ->
                val qOk = query.isEmpty() || matchesSearchText(header, query)
                val exprOk = matcher == null || matcher.matchesHeader(header.id, header.annotations)
                qOk && exprOk
            }
            .sortedBy { it.id }
            .take(capped)
            .toList()
    }

    /**
     * Headers matching [matcher]. Uses Postgres annotation/id pushdown when the expression
     * lowers; otherwise scans headers and evaluates [BoMGraphExprMatcher.matchesHeader].
     */
    @Transactional(readOnly = true)
    fun matchingHeaders(matcher: BoMGraphExprMatcher): List<BoMGraphHeader> {
        val pushdown = matcher.pushdown
        if (pushdown != null && postgres) {
            return findHeadersByPushdown(pushdown, limit = null)
        }
        return graphRepository.findAll()
            .asSequence()
            .map { it.toHeader() }
            .filter { matcher.matchesHeader(it.id, it.annotations) }
            .sortedBy { it.id }
            .toList()
    }

    private fun headersForSearch(matcher: BoMGraphExprMatcher?, sqlLimit: Int?): List<BoMGraphHeader> {
        val pushdown = matcher?.pushdown
        if (pushdown != null && postgres) {
            return findHeadersByPushdown(pushdown, sqlLimit)
        }
        return graphRepository.findAll().map { it.toHeader() }
    }

    private fun findHeadersByPushdown(pushdown: BoMGraphExprPushdown, limit: Int?): List<BoMGraphHeader> {
        if (pushdown.isUnsatisfiable) {
            return emptyList()
        }
        val groupSql = ArrayList<String>()
        val args = ArrayList<Any>()
        for (group in pushdown.dnf) {
            val clauses = ArrayList<String>()
            group.idEquals?.let { id ->
                clauses += "id = ?"
                args += id
            }
            for (id in group.idNotEquals) {
                clauses += "id <> ?"
                args += id
            }
            if (group.annotationEquals.isNotEmpty()) {
                clauses += "annotations @> ?::jsonb"
                args += objectMapper.writeValueAsString(group.annotationEquals)
            }
            for ((key, value) in group.annotationNotEquals) {
                clauses += "(annotations ->> ?) IS DISTINCT FROM ?"
                args += key
                args += value
            }
            require(clauses.isNotEmpty()) { "graph-expr AND-group WHERE must not be empty" }
            groupSql += "(${clauses.joinToString(" AND ")})"
        }
        require(groupSql.isNotEmpty()) { "graph-expr pushdown WHERE must not be empty" }
        val sql = StringBuilder(
            "SELECT id, annotations::text AS annotations, created_at, updated_at FROM bom_graph WHERE ",
        )
        sql.append(groupSql.joinToString(" OR "))
        sql.append(" ORDER BY id")
        if (limit != null) {
            sql.append(" LIMIT ?")
            args += limit
        }
        return jdbc.query(sql.toString(), headerRowMapper, *args.toTypedArray())
    }

    /**
     * Hard materialization: clone members (new ids) into a brand-new graph, stamp [annotations]
     * on clones and the new header. Source graph is unchanged (G-S13–G-S15).
     */
    @Transactional
    private fun snapshot(sourceId: UUID, annotations: Map<String, String>): BoMResolvedGraph {
        val source = get(sourceId)
            ?: throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $sourceId",
            )
        val newGraphId = UUID.randomUUID()
        val idMap = linkedMapOf<UUID, UUID>()
        val newEntities = source.contents.entities.map { entity ->
            val oldId = requireNotNull(entity.id) { "member entity missing id" }
            val newId = UUID.randomUUID()
            idMap[oldId] = newId
            val merged = LinkedHashMap(entity.annotations)
            annotations.forEach { (k, v) -> merged[k] = v }
            BoMEntity(
                id = newId,
                type = entity.type,
                schemaVersion = entity.schemaVersion,
                payload = deepCopyMap(entity.payload),
                annotations = merged,
            )
        }
        val newEdges = source.contents.edges.map { edge ->
            val oldEdgeId = requireNotNull(edge.id) { "member edge missing id" }
            BoMEdge(
                id = UUID.randomUUID(),
                graphId = newGraphId,
                source = requireNotNull(idMap[edge.source]) { "edge $oldEdgeId source not in pack" },
                target = requireNotNull(idMap[edge.target]) { "edge $oldEdgeId target not in pack" },
                role = edge.role,
                type = edge.type,
                schemaVersion = edge.schemaVersion,
                properties = edge.properties?.let { deepCopyMap(it) },
            )
        }
        // Header must exist before edges referencing it via graph_id (NOT NULL FK) are written.
        graphRepository.save(BoMGraphRecord(id = newGraphId, annotations = annotations.toMutableMap()))
        val writeResult = graphStore.write(
            BoMGraph(
                entities = newEntities.toMutableList(),
                edges = newEdges.toMutableList(),
            ),
        )
        if (!writeResult.isValid) {
            throw BoMGraphException(
                code = "GRAPH_CLONE_VALIDATE",
                message = writeResult.issues.joinToString("; ") { it.message ?: it.code },
            )
        }
        if (newEntities.isNotEmpty()) {
            membershipRepository.saveAll(
                newEntities.map { BoMGraphMembershipRecord(graphId = newGraphId, entityId = requireNotNull(it.id)) },
            )
        }
        return requireNotNull(get(newGraphId))
    }

    /**
     * Story vocabulary alias for [snapshot]: clone [sourceId] into a brand-new, independent graph
     * (new entity/edge ids, no parent FK — snapshot hierarchy is an application concern, not objs).
     */
    @Transactional
    fun clone(sourceId: UUID, annotations: Map<String, String> = emptyMap()): BoMResolvedGraph =
        snapshot(sourceId, annotations)

    @Transactional
    @JvmOverloads
    fun createDeepGraphVersion(
        graphId: UUID,
        versionAnnotations: Map<String, String> = emptyMap(),
    ) = deepVersions.createDeepGraphVersion(graphId, versionAnnotations)

    @Transactional(readOnly = true)
    fun listGraphVersions(graphId: UUID) = deepVersions.listGraphVersions(graphId)

    @Transactional(readOnly = true)
    fun getGraphVersion(graphId: UUID, version: Long) = deepVersions.getGraphVersion(graphId, version)

    @Transactional(readOnly = true)
    fun listEntityVersions(entityId: UUID) = deepVersions.listEntityVersions(entityId)

    @Transactional(readOnly = true)
    fun entityVersionStats(entityId: UUID, recent: Int = 5) = deepVersions.entityVersionStats(entityId, recent)

    @Transactional(readOnly = true)
    fun getEntityVersion(entityId: UUID, version: Long) = deepVersions.getEntityVersion(entityId, version)

    @Transactional(readOnly = true)
    fun listEdgeVersions(edgeId: UUID) = deepVersions.listEdgeVersions(edgeId)

    @Transactional(readOnly = true)
    fun edgeVersionStats(edgeId: UUID, recent: Int = 5) = deepVersions.edgeVersionStats(edgeId, recent)

    @Transactional(readOnly = true)
    fun getEdgeVersion(edgeId: UUID, version: Long) = deepVersions.getEdgeVersion(edgeId, version)

    /** Graphs with live membership or a deep-version pin for [entityId] (G-A5 + C-19). Empty for orphans. */
    @Transactional(readOnly = true)
    fun listGraphIdsForEntity(entityId: UUID): List<UUID> {
        val live = membershipRepository.findByEntityId(entityId).map { it.graphId }
        val pinned = versionMemberRepository.findDistinctGraphIdsByEntityId(entityId)
        return (live + pinned).distinct().sortedBy { it.toString() }
    }

    /**
     * Graph-local edges incident to [entityId] (G-A14). Optional [graphId] restricts to one graph.
     */
    @Transactional(readOnly = true)
    @JvmOverloads
    fun listIncidentEdges(entityId: UUID, graphId: UUID? = null): List<BoMEdge> {
        val rows =
            if (graphId == null) {
                edgeRepository.findIncident(entityId)
            } else {
                edgeRepository.findIncidentInGraph(entityId, graphId)
            }
        return rows.map { it.toDomain() }
    }

    /** Member entity ids of [graphId] (membership table only). */
    @Transactional(readOnly = true)
    fun listEntityIdsInGraph(graphId: UUID): List<UUID> =
        membershipRepository.findByGraphId(graphId).map { it.entityId }

    /** Pool entities that are members of [graphId], ordered by type then id. */
    @Transactional(readOnly = true)
    fun listMembers(graphId: UUID): List<BoMEntity> {
        val ids = listEntityIdsInGraph(graphId)
        if (ids.isEmpty()) return emptyList()
        return entityRepository.findAllById(ids).map { it.toDomain() }
            .sortedWith(compareBy({ it.type }, { it.id.toString() }))
    }

    @Transactional(readOnly = true)
    fun listMembers(graphId: UUID, page: BoMPageRequest): BoMPagedEntities {
        val all = listMembers(graphId)
        val from = page.offset.coerceAtMost(all.size)
        val to = (from + page.size).coerceAtMost(all.size)
        return BoMPagedEntities(
            items = all.subList(from, to),
            total = all.size.toLong(),
            page = page.page,
            size = page.size,
        )
    }

    /**
     * Live membership copy: new graph id, **same** pool entity ids, graph-local edges copied
     * with new ids. Does not insert pool entities (unlike [clone]).
     */
    @Transactional
    @JvmOverloads
    fun copyGraph(sourceId: UUID, annotations: Map<String, String> = emptyMap()): BoMResolvedGraph {
        val source = get(sourceId)
            ?: throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $sourceId",
            )
        val entityIds = source.contents.entities.mapNotNull { it.id }
        val edges = source.contents.edges.map { copyEdgeWithoutId(it) }
        return persistLiveGraph(annotations, entityIds, edges)
    }

    /**
     * Persist-union of [sourceIds] in caller order. Default policy is [FirstSeenGraphMergePolicy].
     */
    @Transactional
    @JvmOverloads
    fun mergeGraph(
        sourceIds: Collection<UUID>,
        annotations: Map<String, String> = emptyMap(),
        policy: GraphMergePolicy = FirstSeenGraphMergePolicy(),
    ): BoMResolvedGraph {
        if (sourceIds.isEmpty()) {
            throw BoMGraphException(
                code = "GRAPH_MERGE_EMPTY",
                message = "mergeGraph requires at least one source graph",
            )
        }
        val sources = sourceIds.map { id ->
            get(id) ?: throw BoMGraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        }
        val nodes = linkedMapOf<Any, BoMEntity>()
        for (graph in sources) {
            for (entity in graph.contents.entities) {
                val key = policy.nodeKey(entity)
                val existing = nodes[key]
                nodes[key] = if (existing == null) entity else policy.onDuplicateNode(existing, entity)
            }
        }
        val edges = linkedMapOf<Any, BoMEdge>()
        for (graph in sources) {
            for (edge in graph.contents.edges) {
                val key = policy.edgeKey(edge)
                val existing = edges[key]
                edges[key] = if (existing == null) edge else policy.onDuplicateEdge(existing, edge)
            }
        }
        return persistLiveGraph(
            annotations,
            nodes.values.mapNotNull { it.id },
            edges.values.map { copyEdgeWithoutId(it) },
        )
    }

    /**
     * New live graph: membership of existing pool entities + new edge rows. Does not write
     * pool entities.
     */
    private fun persistLiveGraph(
        annotations: Map<String, String>,
        entityIds: Collection<UUID>,
        edges: List<BoMEdge>,
    ): BoMResolvedGraph {
        val created = create(
            BoMGraphSpec(
                annotations = annotations,
                entityIds = entityIds.toSet(),
            ),
        )
        if (edges.isEmpty()) {
            return created
        }
        val newGraphId = created.id
        val newEdges = edges.map { edge ->
            BoMEdge(
                id = UUID.randomUUID(),
                graphId = newGraphId,
                source = edge.source,
                target = edge.target,
                role = edge.role,
                type = edge.type,
                schemaVersion = edge.schemaVersion,
                properties = edge.properties?.let { deepCopyMap(it) },
            )
        }
        val writeResult = graphStore.write(
            BoMGraph(
                entities = mutableListOf(),
                edges = newEdges.toMutableList(),
            ),
        )
        if (!writeResult.isValid) {
            throw BoMGraphException(
                code = "GRAPH_COPY_VALIDATE",
                message = writeResult.issues.joinToString("; ") { it.message ?: it.code },
            )
        }
        return requireNotNull(get(newGraphId))
    }

    private fun copyEdgeWithoutId(edge: BoMEdge): BoMEdge =
        BoMEdge(
            source = edge.source,
            target = edge.target,
            role = edge.role,
            type = edge.type,
            schemaVersion = edge.schemaVersion,
            properties = edge.properties?.let { deepCopyMap(it) },
        )

    /**
     * Attach an existing pool entity to [graphId] (membership row only; idempotent).
     */
    @Transactional
    fun attach(graphId: UUID, entityId: UUID) {
        requireGraphExists(graphId)
        if (!entityRepository.existsById(entityId)) {
            throw BoMGraphException(code = "GRAPH_ENTITY_MISSING", message = "Entity not found: $entityId")
        }
        membershipRepository.save(BoMGraphMembershipRecord(graphId = graphId, entityId = entityId))
        touch(graphId)
    }

    /**
     * Detach [entityId] from [graphId] (membership row only; pool entity kept) and drop this
     * graph's edges incident to it (edges cannot survive without a member endpoint).
     */
    @Transactional
    fun detach(graphId: UUID, entityId: UUID) {
        requireGraphExists(graphId)
        membershipRepository.deleteByGraphIdAndEntityId(graphId, entityId)
        edgeRepository.findByGraphId(graphId)
            .filter { it.sourceId == entityId || it.targetId == entityId }
            .forEach { edgeRepository.delete(it) }
        touch(graphId)
    }

    /**
     * Transactional graph-scoped mutation (WI-004): validate, then explicit edge deletes
     * (only this graph's edges), entity **detach** (membership + incident edges; pool entities
     * kept), then upserts — entity upsert lands in the pool + this graph's membership; edge
     * upsert is stamped with [graphId] and requires both endpoints to be projected members.
     *
     * Same id in delete and upsert: upsert wins (mirrors [BoMGraphStore.mutate]).
     */
    @Transactional
    fun mutate(graphId: UUID, mutation: BoMGraphMutation): BoMValidationResult {
        val result = validateMutate(graphId, mutation)
        if (!result.isValid) {
            return result
        }
        applyGraphDeletes(graphId, mutation)
        if (mutation.upsert.entities.isNotEmpty()) {
            graphStore.upsertEntities(mutation.upsert.entities)
            mutation.upsert.entities.forEach { entity ->
                membershipRepository.save(
                    BoMGraphMembershipRecord(graphId = graphId, entityId = requireNotNull(entity.id)),
                )
            }
        }
        applyGraphEdgeUpserts(graphId, mutation.upsert.edges)
        touch(graphId)
        return BoMValidationResult.ok()
    }

    /**
     * Dry-run validation for [mutate]: same checks, no persistence. May assign ids to upsert
     * entities/edges (via [BoMPersistGate.prepareIds]) and stamps [graphId] onto upsert edges.
     */
    @Transactional(readOnly = true)
    fun validateMutate(graphId: UUID, mutation: BoMGraphMutation): BoMValidationResult {
        requireGraphExists(graphId)
        mutation.upsert.edges.forEach { it.graphId = graphId }

        val g = gate()
        val issues = mutableListOf<BoMValidationIssue>()
        for (id in mutation.delete.edges.distinct()) {
            issues.addAll(g.validateDeleteEdge(id).issues)
        }
        for (id in mutation.delete.entities.distinct()) {
            issues.addAll(g.validateDeleteEntity(id).issues)
        }
        if (issues.isNotEmpty()) {
            return BoMValidationResult.of(issues)
        }
        if (!mutation.hasUpserts()) {
            return BoMValidationResult.ok()
        }

        val graph = mutation.graph()
        val stage1 = validator.validateEntities(graph.entities)
        if (!stage1.isValid) {
            return stage1
        }
        g.prepareIds(graph)

        val deletedEntityIds = mutation.delete.entities.toSet()
        val projectedStore = BoMEntityTypeLookup { id ->
            if (id in deletedEntityIds) null else entityRepository.findById(id).map { it.type }.orElse(null)
        }
        val lookup = validator.combinedLookup(graph.entities, projectedStore)
        val edgeIssues = validator.validateEdges(graph.edges, lookup).issues.toMutableList()

        val currentMembers = membershipRepository.findByGraphId(graphId).mapTo(hashSetOf()) { it.entityId }
        val projectedMembers = (currentMembers - deletedEntityIds) + graph.entities.mapNotNull { it.id }
        graph.edges.forEachIndexed { index, edge ->
            if (edge.source !in projectedMembers) {
                edgeIssues += BoMValidationIssue(
                    code = "EDGE_ENDPOINT_NOT_MEMBER",
                    message = "Edge source ${edge.source} is not a member of graph $graphId",
                    path = "edges[$index].source",
                )
            }
            if (edge.target !in projectedMembers) {
                edgeIssues += BoMValidationIssue(
                    code = "EDGE_ENDPOINT_NOT_MEMBER",
                    message = "Edge target ${edge.target} is not a member of graph $graphId",
                    path = "edges[$index].target",
                )
            }
        }
        if (edgeIssues.isNotEmpty()) {
            return BoMValidationResult(edgeIssues)
        }
        val identityIssues = mutableListOf<BoMValidationIssue>()
        graph.entities.forEachIndexed { index, entity ->
            val id = entity.id ?: return@forEachIndexed
            if (id in deletedEntityIds) return@forEachIndexed
            val stored = entityRepository.findById(id).orElse(null)?.toDomain() ?: return@forEachIndexed
            identityIssues += validator.validateEntityIdentifierImmutability(
                stored,
                entity,
                path = "entities[$index]",
            )
        }
        graph.edges.forEachIndexed { index, edge ->
            val id = edge.id ?: return@forEachIndexed
            val stored = edgeRepository.findById(id).orElse(null)?.toDomain() ?: return@forEachIndexed
            identityIssues += validator.validateEdgeIdentifierImmutability(
                stored,
                edge,
                path = "edges[$index]",
            )
        }
        return BoMValidationResult(identityIssues)
    }

    private fun applyGraphDeletes(graphId: UUID, mutation: BoMGraphMutation) {
        val graphEdgeIds = edgeRepository.findByGraphId(graphId).mapNotNullTo(hashSetOf()) { it.id }
        for (id in mutation.delete.edges.distinct()) {
            if (id in graphEdgeIds) {
                edgeRepository.deleteById(id)
            }
        }
        val entityIds = mutation.delete.entities.distinct()
        if (entityIds.isEmpty()) {
            return
        }
        val idSet = entityIds.toSet()
        edgeRepository.findByGraphId(graphId)
            .filter { it.sourceId in idSet || it.targetId in idSet }
            .forEach { edgeRepository.delete(it) }
        entityIds.forEach { id -> membershipRepository.deleteByGraphIdAndEntityId(graphId, id) }
    }

    private fun applyGraphEdgeUpserts(graphId: UUID, edges: List<BoMEdge>) {
        for (edge in edges) {
            val id = requireNotNull(edge.id)
            val existing = edgeRepository.findById(id).orElse(null)
            val now = java.time.Instant.now()
            val record = existing ?: BoMEdgeRecord(id = id, createdAt = now, updatedAt = now)
            record.graphId = graphId
            record.sourceId = edge.source
            record.targetId = edge.target
            record.role = edge.role
            record.type = edge.type
            record.schemaVersion = edge.schemaVersion
            record.properties = edge.properties?.toMutableMap()
            record.updatedAt = now
            edgeRepository.save(record)
        }
    }

    private fun requireGraphExists(graphId: UUID) {
        if (!graphRepository.existsById(graphId)) {
            throw BoMGraphException(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId")
        }
    }

    private fun deepCopyMap(source: Map<String, Any?>): MutableMap<String, Any?> {
        val copy = LinkedHashMap<String, Any?>()
        for ((k, v) in source) {
            copy[k] = when (v) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    deepCopyMap(v as Map<String, Any?>)
                }
                is List<*> -> v.map { item ->
                    if (item is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        deepCopyMap(item as Map<String, Any?>)
                    } else {
                        item
                    }
                }.toMutableList()
                else -> v
            }
        }
        return copy
    }

    /**
     * Entity membership is stored as M2M rows. Edges are owned via `graph_id`: any edge listed
     * in [edgeIds] is (re)assigned to [graphId]; edges previously owned by [graphId] but no
     * longer listed are removed (they cannot exist without a graph).
     */
    private fun replaceMembership(graphId: UUID, entityIds: Set<UUID>, edgeIds: Set<UUID>) {
        if (entityIds.isNotEmpty()) {
            membershipRepository.saveAll(
                entityIds.map { BoMGraphMembershipRecord(graphId = graphId, entityId = it) },
            )
        }
        val currentEdgeIds = edgeRepository.findByGraphId(graphId).mapNotNullTo(linkedSetOf()) { it.id }
        val toRemove = currentEdgeIds - edgeIds
        if (toRemove.isNotEmpty()) {
            edgeRepository.deleteAllById(toRemove)
        }
        for (edgeId in edgeIds) {
            val edge = edgeRepository.findById(edgeId).orElseThrow {
                BoMGraphException(code = "GRAPH_EDGE_MISSING", message = "Edge not found: $edgeId")
            }
            edge.graphId = graphId
            edgeRepository.save(edge)
        }
    }

    private fun validateMembership(entityIds: Set<UUID>, edgeIds: Set<UUID>) {
        for (entityId in entityIds) {
            if (!entityRepository.existsById(entityId)) {
                throw BoMGraphException(
                    code = "GRAPH_ENTITY_MISSING",
                    message = "Entity not found: $entityId",
                )
            }
        }
        for (edgeId in edgeIds) {
            val edge = edgeRepository.findById(edgeId).orElse(null)
                ?: throw BoMGraphException(
                    code = "GRAPH_EDGE_MISSING",
                    message = "Edge not found: $edgeId",
                )
            if (edge.sourceId !in entityIds || edge.targetId !in entityIds) {
                throw BoMGraphException(
                    code = "GRAPH_EDGE_ENDPOINTS",
                    message = "Edge $edgeId endpoints must both be subgraph entity members",
                )
            }
        }
    }

    private fun resolve(header: BoMGraphRecord): BoMResolvedGraph {
        val entityIds = membershipRepository.findByGraphId(header.id).map { it.entityId }
        val entities = if (entityIds.isEmpty()) {
            emptyList()
        } else {
            entityRepository.findAllById(entityIds).map { it.toDomain() }
        }
        val edges = edgeRepository.findByGraphId(header.id).map { it.toDomain() }
        return BoMResolvedGraph(
            id = header.id,
            annotations = header.annotations.toMap(),
            contents = BoMGraphContents(entities = entities, edges = edges),
            createdAt = header.createdAt,
            updatedAt = header.updatedAt,
        )
    }

    /** Bump graph HEAD `updated_at`. No-op if the header is gone. */
    @Transactional
    fun touch(graphId: UUID) {
        val header = graphRepository.findById(graphId).orElse(null) ?: return
        header.updatedAt = java.time.Instant.now()
        graphRepository.save(header)
    }

    private fun BoMGraphRecord.toHeader() = BoMGraphHeader(
        id = id,
        annotations = annotations.toMap(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 15
        const val MAX_SEARCH_LIMIT = 100

        private val ANNOTATIONS_TYPE = object : TypeReference<Map<String, String>>() {}

        /** v1 q match: UUID / UUID-prefix + case-insensitive substring on id and annotation key/value. */
        internal fun matchesSearchText(header: BoMGraphHeader, q: String): Boolean {
            val idStr = header.id.toString()
            if (idStr.startsWith(q, ignoreCase = true) || idStr.contains(q, ignoreCase = true)) {
                return true
            }
            return header.annotations.any { (key, value) ->
                key.contains(q, ignoreCase = true) || value.contains(q, ignoreCase = true)
            }
        }
    }
}
