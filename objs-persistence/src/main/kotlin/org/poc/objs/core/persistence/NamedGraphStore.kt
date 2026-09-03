package org.poc.objs.core.persistence

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.EntityLiveGraphs
import org.poc.objs.api.domain.GraphHeader
import org.poc.objs.api.domain.GraphMutation
import org.poc.objs.api.domain.MutationMode
import org.poc.objs.api.domain.ResolvedGraph
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.GraphException
import org.poc.objs.api.domain.FirstSeenGraphMergePolicy
import org.poc.objs.api.domain.GraphMergePolicy
import org.poc.objs.api.domain.GraphListItem
import org.poc.objs.api.domain.GraphSpec
import org.poc.objs.api.domain.PageRequest
import org.poc.objs.api.domain.PagedEntities
import org.poc.objs.api.match.GraphExprMatcher
import org.poc.objs.api.match.GraphExprPushdown
import org.poc.objs.api.validation.EntityTypeLookup
import org.poc.objs.api.validation.PersistGate
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.api.validation.ValidationResult
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.core.validation.Validator
import java.sql.ResultSet
import java.util.UUID

/**
 * Graph store: entity membership is M2M (`objs_graph_entity`); edges are graph-owned via
 * `objs_graph_edge.graph_id` (C-13 — no more `bom_subgraph_edges` M2M).
 */
class NamedGraphStore(
    private val graphDao: GraphDao,
    private val membershipDao: GraphMembershipDao,
    private val entityDao: EntityDao,
    private val edgeDao: EdgeDao,
    private val validator: Validator,
    private val deepVersions: DeepGraphVersionService,
    private val versionMemberDao: GraphVersionMemberDao,
    private val uow: UnitOfWork,
) {
    private lateinit var graphStore: GraphStore

    fun attachGraphStore(store: GraphStore) {
        graphStore = store
    }

    private val objectMapper = ObjectMapper()
    private var postgres: Boolean? = null

    private fun isPostgres(): Boolean {
        if (postgres == null) {
            postgres = uow.connection().metaData.databaseProductName.equals("PostgreSQL", ignoreCase = true)
        }
        return postgres!!
    }

    private fun readHeader(rs: ResultSet): GraphHeader {
        val annotationsJson = rs.getString("annotations")
        val annotations: Map<String, String> =
            if (annotationsJson.isNullOrBlank()) {
                emptyMap()
            } else {
                objectMapper.readValue(annotationsJson, ANNOTATIONS_TYPE)
            }
        return GraphHeader(
            id = rs.getObject("id", UUID::class.java),
            annotations = annotations,
            createdAt = rs.getTimestamp("created_at")?.toInstant(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant(),
        )
    }

    private fun gate(): PersistGate = PersistGate(
        validator = validator,
        storeLookup = EntityTypeLookup { id -> entityDao.findById(id)?.type },
        existsEntity = { id -> entityDao.existsById(id) },
        existsEdge = { id -> edgeDao.existsById(id) },
    )

    fun create(spec: GraphSpec): ResolvedGraph = uow.write {
        val id = spec.id ?: UUID.randomUUID()
        if (graphDao.existsById(id)) {
            throw GraphException(
                code = "GRAPH_ID_CONFLICT",
                message = "Subgraph already exists: $id",
            )
        }
        validateMembership(spec.entityIds, spec.edgeIds)
        graphDao.save(
            GraphRecord(
                id = id,
                annotations = spec.annotations.toMutableMap(),
            ),
        )
        replaceMembership(id, spec.entityIds, spec.edgeIds)
        requireNotNull(get(id))
    }

    fun updateAnnotations(id: UUID, annotations: Map<String, String>): ResolvedGraph = uow.write {
        val existing = graphDao.findById(id)
            ?: throw GraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        existing.annotations = annotations.toMutableMap()
        existing.updatedAt = java.time.Instant.now()
        graphDao.save(existing)
        requireNotNull(get(id))
    }

    fun replace(id: UUID, spec: GraphSpec): ResolvedGraph = uow.write {
        val existing = graphDao.findById(id)
            ?: throw GraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        validateMembership(spec.entityIds, spec.edgeIds)
        existing.annotations = spec.annotations.toMutableMap()
        existing.updatedAt = java.time.Instant.now()
        graphDao.save(existing)
        membershipDao.deleteByGraphId(id)
        replaceMembership(id, spec.entityIds, spec.edgeIds)
        requireNotNull(get(id))
    }

    fun delete(id: UUID) = uow.write {
        if (!graphDao.existsById(id)) {
            throw GraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        }
        graphDao.deleteById(id)
    }

    fun get(id: UUID): ResolvedGraph? = uow.read {
        val header = graphDao.findById(id) ?: return@read null
        resolve(header)
    }

    fun list(): List<GraphListItem> = uow.read {
        graphDao.findAll().map { header ->
            GraphListItem(
                id = header.id,
                annotations = header.annotations.toMap(),
                entityCount = membershipDao.countByGraphId(header.id),
                edgeCount = edgeDao.countByGraphId(header.id),
                createdAt = header.createdAt,
                updatedAt = header.updatedAt,
            )
        }
    }

    /**
     * G-U10 / WI-007 open-graph search over headers (no FTS). Empty [q] without [expr] returns
     * nothing — never the full catalog. When both are set, results must match **both** (AND).
     * Order is stable by id; [limit] is capped at [MAX_SEARCH_LIMIT].
     *
     * When [expr] lowers to equality/`&&` over `id` / `a.*` and the backend is PostgreSQL,
     * candidates are loaded via PK / `annotations @>` (GIN) instead of scanning all headers.
     */
    fun search(q: String? = null, expr: String? = null, limit: Int = DEFAULT_SEARCH_LIMIT): List<GraphHeader> =
        uow.read {
        val query = q?.trim().orEmpty()
        val expression = expr?.trim().orEmpty()
        if (query.isEmpty() && expression.isEmpty()) {
            return@read emptyList()
        }
        val capped = when {
            limit < 1 -> DEFAULT_SEARCH_LIMIT
            else -> minOf(limit, MAX_SEARCH_LIMIT)
        }
        val matcher = if (expression.isNotEmpty()) GraphExprMatcher(expression) else null
        val sqlLimit = if (query.isEmpty()) capped else null
        val candidates = headersForSearch(matcher, sqlLimit)
        candidates
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
     * lowers; otherwise scans headers and evaluates [GraphExprMatcher.matchesHeader].
     */
    fun matchingHeaders(matcher: GraphExprMatcher): List<GraphHeader> = uow.read {
        val pushdown = matcher.pushdown
        if (pushdown != null && isPostgres()) {
            findHeadersByPushdown(pushdown, limit = null)
        } else {
            graphDao.findAll()
                .asSequence()
                .map { it.toHeader() }
                .filter { matcher.matchesHeader(it.id, it.annotations) }
                .sortedBy { it.id }
                .toList()
        }
    }

    private fun headersForSearch(matcher: GraphExprMatcher?, sqlLimit: Int?): List<GraphHeader> {
        val pushdown = matcher?.pushdown
        if (pushdown != null && isPostgres()) {
            return findHeadersByPushdown(pushdown, sqlLimit)
        }
        return graphDao.findAll().map { it.toHeader() }
    }

    private fun findHeadersByPushdown(pushdown: GraphExprPushdown, limit: Int?): List<GraphHeader> {
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
            "SELECT id, annotations::text AS annotations, created_at, updated_at FROM objs_graph WHERE ",
        )
        sql.append(groupSql.joinToString(" OR "))
        sql.append(" ORDER BY id")
        if (limit != null) {
            sql.append(" LIMIT ?")
            args += limit
        }
        val connection = uow.connection()
        return connection.prepareStatement(sql.toString()).use { statement ->
            args.forEachIndexed { index, arg ->
                when (arg) {
                    is UUID -> statement.setObject(index + 1, arg)
                    is Int -> statement.setInt(index + 1, arg)
                    else -> statement.setString(index + 1, arg as String)
                }
            }
            statement.executeQuery().use { rs ->
                val results = mutableListOf<GraphHeader>()
                while (rs.next()) {
                    results += readHeader(rs)
                }
                results
            }
        }
    }

    /**
     * Hard materialization: clone members (new ids) into a brand-new graph, stamp [annotations]
     * on clones and the new header. Source graph is unchanged (G-S13–G-S15).
     */
    private fun snapshot(sourceId: UUID, annotations: Map<String, String>): ResolvedGraph {
        val source = get(sourceId)
            ?: throw GraphException(
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
            Entity(
                id = newId,
                type = entity.type,
                schemaVersion = entity.schemaVersion,
                payload = deepCopyMap(entity.payload),
                annotations = merged,
            )
        }
        val newEdges = source.contents.edges.map { edge ->
            val oldEdgeId = requireNotNull(edge.id) { "member edge missing id" }
            Edge(
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
        graphDao.save(GraphRecord(id = newGraphId, annotations = annotations.toMutableMap()))
        val writeResult = graphStore.write(
            Graph(
                entities = newEntities.toMutableList(),
                edges = newEdges.toMutableList(),
            ),
        )
        if (!writeResult.isValid) {
            throw GraphException(
                code = "GRAPH_CLONE_VALIDATE",
                message = writeResult.issues.joinToString("; ") { it.message ?: it.code },
            )
        }
        if (newEntities.isNotEmpty()) {
            membershipDao.saveAll(
                newEntities.map { GraphMembershipRecord(graphId = newGraphId, entityId = requireNotNull(it.id)) },
            )
        }
        return requireNotNull(get(newGraphId))
    }

    /**
     * Story vocabulary alias for [snapshot]: clone [sourceId] into a brand-new, independent graph
     * (new entity/edge ids, no parent FK — snapshot hierarchy is an application concern, not objs).
     */
    fun clone(sourceId: UUID, annotations: Map<String, String> = emptyMap()): ResolvedGraph =
        uow.write { snapshot(sourceId, annotations) }

    fun createDeepGraphVersion(
        graphId: UUID,
        versionAnnotations: Map<String, String> = emptyMap(),
    ) = uow.write { deepVersions.createDeepGraphVersion(graphId, versionAnnotations) }

    fun listGraphVersions(graphId: UUID) = deepVersions.listGraphVersions(graphId)

    fun getGraphVersion(graphId: UUID, version: Long) = deepVersions.getGraphVersion(graphId, version)

    fun listEntityVersions(entityId: UUID) = deepVersions.listEntityVersions(entityId)

    fun entityVersionStats(entityId: UUID, recent: Int = 5) = deepVersions.entityVersionStats(entityId, recent)

    fun getEntityVersion(entityId: UUID, version: Long) = deepVersions.getEntityVersion(entityId, version)

    fun listEdgeVersions(edgeId: UUID) = deepVersions.listEdgeVersions(edgeId)

    fun edgeVersionStats(edgeId: UUID, recent: Int = 5) = deepVersions.edgeVersionStats(edgeId, recent)

    fun getEdgeVersion(edgeId: UUID, version: Long) = deepVersions.getEdgeVersion(edgeId, version)

    /** Graphs with live membership or a deep-version pin for [entityId] (G-A5 + C-19). Empty for orphans. */
    fun listGraphIdsForEntity(entityId: UUID): List<UUID> = uow.read {
        val live = membershipDao.findByEntityId(entityId).map { it.graphId }
        val pinned = versionMemberDao.findDistinctGraphIdsByEntityId(entityId)
        (live + pinned).distinct().sortedBy { it.toString() }
    }

    /**
     * Live HEAD membership only (ignores deep-version pins). Headers sorted by [GraphHeader.updatedAt]
     * desc (then createdAt, then id). Optional [q] uses [matchesSearchText]; [limit] caps [EntityLiveGraphs.items].
     * [EntityLiveGraphs.total] is always the unfiltered live count.
     */
    fun listLiveGraphHeadersForEntity(
        entityId: UUID,
        q: String? = null,
        limit: Int? = null,
    ): EntityLiveGraphs = uow.read {
        val graphIds = membershipDao.findByEntityId(entityId).map { it.graphId }.distinct()
        val total = graphIds.size
        if (graphIds.isEmpty()) {
            return@read EntityLiveGraphs(items = emptyList(), total = 0)
        }
        val query = q?.trim().orEmpty()
        val sorted = graphDao.findAllById(graphIds)
            .asSequence()
            .map { it.toHeader() }
            .filter { query.isEmpty() || matchesSearchText(it, query) }
            .sortedWith(
                compareByDescending<GraphHeader> { it.updatedAt ?: java.time.Instant.EPOCH }
                    .thenByDescending { it.createdAt ?: java.time.Instant.EPOCH }
                    .thenBy { it.id.toString() },
            )
        val items =
            if (limit != null && limit > 0) {
                sorted.take(limit).toList()
            } else {
                sorted.toList()
            }
        EntityLiveGraphs(items = items, total = total)
    }

    /**
     * Graph-local edges incident to [entityId] (G-A14). Optional [graphId] restricts to one graph.
     */
    fun listIncidentEdges(entityId: UUID, graphId: UUID? = null): List<Edge> = uow.read {
        val rows =
            if (graphId == null) {
                edgeDao.findIncident(entityId)
            } else {
                edgeDao.findIncidentInGraph(entityId, graphId)
            }
        rows.map { it.toDomain() }
    }

    /** Member entity ids of [graphId] (membership table only). */
    fun listEntityIdsInGraph(graphId: UUID): List<UUID> = uow.read {
        membershipDao.findByGraphId(graphId).map { it.entityId }
    }

    /** Pool entities that are members of [graphId], ordered by type then id. */
    fun listMembers(graphId: UUID): List<Entity> = uow.read {
        val ids = membershipDao.findByGraphId(graphId).map { it.entityId }
        if (ids.isEmpty()) return@read emptyList()
        entityDao.findAllById(ids).map { it.toDomain() }
            .sortedWith(compareBy({ it.type }, { it.id.toString() }))
    }

    fun listMembers(graphId: UUID, page: PageRequest): PagedEntities = uow.read {
        val all = listMembers(graphId)
        val from = page.offset.coerceAtMost(all.size)
        val to = (from + page.size).coerceAtMost(all.size)
        PagedEntities(
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
    fun copyGraph(sourceId: UUID, annotations: Map<String, String> = emptyMap()): ResolvedGraph = uow.write {
        val source = get(sourceId)
            ?: throw GraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $sourceId",
            )
        val entityIds = source.contents.entities.mapNotNull { it.id }
        val edges = source.contents.edges.map { copyEdgeWithoutId(it) }
        persistLiveGraph(annotations, entityIds, edges)
    }

    /**
     * Persist-union of [sourceIds] in caller order. Default policy is [FirstSeenGraphMergePolicy].
     */
    fun mergeGraph(
        sourceIds: Collection<UUID>,
        annotations: Map<String, String> = emptyMap(),
        policy: GraphMergePolicy = FirstSeenGraphMergePolicy(),
    ): ResolvedGraph = uow.write {
        if (sourceIds.isEmpty()) {
            throw GraphException(
                code = "GRAPH_MERGE_EMPTY",
                message = "mergeGraph requires at least one source graph",
            )
        }
        val sources = sourceIds.map { id ->
            get(id) ?: throw GraphException(
                code = "GRAPH_NOT_FOUND",
                message = "Subgraph not found: $id",
            )
        }
        val nodes = linkedMapOf<Any, Entity>()
        for (graph in sources) {
            for (entity in graph.contents.entities) {
                val key = policy.nodeKey(entity)
                val existing = nodes[key]
                nodes[key] = if (existing == null) entity else policy.onDuplicateNode(existing, entity)
            }
        }
        val edges = linkedMapOf<Any, Edge>()
        for (graph in sources) {
            for (edge in graph.contents.edges) {
                val key = policy.edgeKey(edge)
                val existing = edges[key]
                edges[key] = if (existing == null) edge else policy.onDuplicateEdge(existing, edge)
            }
        }
        persistLiveGraph(
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
        edges: List<Edge>,
    ): ResolvedGraph {
        val created = create(
            GraphSpec(
                annotations = annotations,
                entityIds = entityIds.toSet(),
            ),
        )
        if (edges.isEmpty()) {
            return created
        }
        val newGraphId = created.id
        val newEdges = edges.map { edge ->
            Edge(
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
            Graph(
                entities = mutableListOf(),
                edges = newEdges.toMutableList(),
            ),
        )
        if (!writeResult.isValid) {
            throw GraphException(
                code = "GRAPH_COPY_VALIDATE",
                message = writeResult.issues.joinToString("; ") { it.message ?: it.code },
            )
        }
        return requireNotNull(get(newGraphId))
    }

    private fun copyEdgeWithoutId(edge: Edge): Edge =
        Edge(
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
    fun attach(graphId: UUID, entityId: UUID) = uow.write {
        requireGraphExists(graphId)
        if (!entityDao.existsById(entityId)) {
            throw GraphException(code = "GRAPH_ENTITY_MISSING", message = "Entity not found: $entityId")
        }
        membershipDao.save(GraphMembershipRecord(graphId = graphId, entityId = entityId))
        touch(graphId)
    }

    /**
     * Detach [entityId] from [graphId] (membership row only; pool entity kept) and drop this
     * graph's edges incident to it (edges cannot survive without a member endpoint).
     */
    fun detach(graphId: UUID, entityId: UUID) = uow.write {
        requireGraphExists(graphId)
        membershipDao.deleteByGraphIdAndEntityId(graphId, entityId)
        edgeDao.findByGraphId(graphId)
            .filter { it.sourceId == entityId || it.targetId == entityId }
            .forEach { edgeDao.delete(it) }
        touch(graphId)
    }

    /**
     * Transactional graph-scoped mutation: validate, then apply [MutationMode.MERGE] or
     * [MutationMode.REPLACE].
     *
     * MERGE: explicit edge unsets, entity detach, then sets (same id in unset and set: set wins).
     * REPLACE: [entities.set]/[edges.set] are the full desired membership + edges; prune extras;
     * non-empty unset → [REPLACE_UNSET_NOT_ALLOWED].
     */
    fun mutate(graphId: UUID, mutation: GraphMutation): ValidationResult = uow.write {
        val result = validateMutate(graphId, mutation)
        if (!result.isValid) {
            return@write result
        }
        when (mutation.mode) {
            MutationMode.MERGE -> {
                applyGraphDeletes(graphId, mutation)
                applyGraphSets(graphId, mutation)
            }
            MutationMode.REPLACE -> {
                applyGraphReplace(graphId, mutation)
            }
        }
        touch(graphId)
        ValidationResult.ok()
    }

    /**
     * Dry-run validation for [mutate]: same checks, no persistence. May assign ids to set
     * entities/edges (via [PersistGate.prepareIds]) and stamps [graphId] onto set edges.
     */
    fun validateMutate(graphId: UUID, mutation: GraphMutation): ValidationResult = uow.read {
        requireGraphExists(graphId)
        mutation.edges.set.forEach { it.graphId = graphId }

        if (mutation.mode == MutationMode.REPLACE && mutation.hasUnsets()) {
            return@read ValidationResult.of(
                ValidationIssue(
                    code = "REPLACE_UNSET_NOT_ALLOWED",
                    message = "REPLACE mutate rejects non-empty entities.unset / edges.unset; send the desired set only",
                ),
            )
        }

        val g = gate()
        val issues = mutableListOf<ValidationIssue>()
        if (mutation.mode == MutationMode.MERGE) {
            for (id in mutation.edges.unset.distinct()) {
                issues.addAll(g.validateDeleteEdge(id).issues)
            }
            for (id in mutation.entities.unset.distinct()) {
                issues.addAll(g.validateDeleteEntity(id).issues)
            }
            if (issues.isNotEmpty()) {
                return@read ValidationResult.of(issues)
            }
        }

        if (!mutation.hasSets()) {
            // MERGE no-op or REPLACE clear — both valid once unset/reject checks passed
            return@read ValidationResult.ok()
        }

        val graph = mutation.graph()
        val stage1 = validator.validateEntities(graph.entities)
        if (!stage1.isValid) {
            return@read stage1
        }
        g.prepareIds(graph)

        val unsetEntityIds =
            if (mutation.mode == MutationMode.MERGE) mutation.entities.unset.toSet() else emptySet()
        val projectedStore = EntityTypeLookup { id ->
            if (id in unsetEntityIds) null else entityDao.findById(id)?.type
        }
        val lookup = validator.combinedLookup(graph.entities, projectedStore)
        val edgeIssues = validator.validateEdges(graph.edges, lookup).issues.toMutableList()

        val currentMembers = membershipDao.findByGraphId(graphId).mapTo(hashSetOf()) { it.entityId }
        val projectedMembers =
            when (mutation.mode) {
                MutationMode.MERGE -> (currentMembers - unsetEntityIds) + graph.entities.mapNotNull { it.id }
                MutationMode.REPLACE -> graph.entities.mapNotNull { it.id }.toSet()
            }
        graph.edges.forEachIndexed { index, edge ->
            if (edge.source !in projectedMembers) {
                edgeIssues += ValidationIssue(
                    code = "EDGE_ENDPOINT_NOT_MEMBER",
                    message = "Edge source ${edge.source} is not a member of graph $graphId",
                    path = "edges.set[$index].source",
                )
            }
            if (edge.target !in projectedMembers) {
                edgeIssues += ValidationIssue(
                    code = "EDGE_ENDPOINT_NOT_MEMBER",
                    message = "Edge target ${edge.target} is not a member of graph $graphId",
                    path = "edges.set[$index].target",
                )
            }
        }
        if (edgeIssues.isNotEmpty()) {
            return@read ValidationResult(edgeIssues)
        }
        val identityIssues = mutableListOf<ValidationIssue>()
        graph.entities.forEachIndexed { index, entity ->
            val id = entity.id ?: return@forEachIndexed
            if (id in unsetEntityIds) return@forEachIndexed
            val stored = entityDao.findById(id)?.toDomain() ?: return@forEachIndexed
            identityIssues += validator.validateEntityIdentifierImmutability(
                stored,
                entity,
                path = "entities.set[$index]",
            )
        }
        graph.edges.forEachIndexed { index, edge ->
            val id = edge.id ?: return@forEachIndexed
            val stored = edgeDao.findById(id)?.toDomain() ?: return@forEachIndexed
            identityIssues += validator.validateEdgeIdentifierImmutability(
                stored,
                edge,
                path = "edges.set[$index]",
            )
        }
        ValidationResult(identityIssues)
    }

    private fun applyGraphSets(graphId: UUID, mutation: GraphMutation) {
        if (mutation.entities.set.isNotEmpty()) {
            graphStore.upsertEntities(mutation.entities.set)
            mutation.entities.set.forEach { entity ->
                membershipDao.save(
                    GraphMembershipRecord(graphId = graphId, entityId = requireNotNull(entity.id)),
                )
            }
        }
        applyGraphEdgeUpserts(graphId, mutation.edges.set)
    }

    private fun applyGraphReplace(graphId: UUID, mutation: GraphMutation) {
        val desiredEntityIds = mutation.entities.set.mapNotNull { it.id }.toSet()
        val desiredEdgeIds = mutation.edges.set.mapNotNull { it.id }.toSet()

        val currentEdgeIds = edgeDao.findByGraphId(graphId).mapNotNull { it.id }
        for (id in currentEdgeIds) {
            if (id !in desiredEdgeIds) {
                edgeDao.deleteById(id)
            }
        }

        val currentMembers = membershipDao.findByGraphId(graphId).map { it.entityId }
        for (entityId in currentMembers) {
            if (entityId !in desiredEntityIds) {
                membershipDao.deleteByGraphIdAndEntityId(graphId, entityId)
            }
        }

        applyGraphSets(graphId, mutation)
    }

    private fun applyGraphDeletes(graphId: UUID, mutation: GraphMutation) {
        val graphEdgeIds = edgeDao.findByGraphId(graphId).mapNotNullTo(hashSetOf()) { it.id }
        for (id in mutation.edges.unset.distinct()) {
            if (id in graphEdgeIds) {
                edgeDao.deleteById(id)
            }
        }
        val entityIds = mutation.entities.unset.distinct()
        if (entityIds.isEmpty()) {
            return
        }
        val idSet = entityIds.toSet()
        edgeDao.findByGraphId(graphId)
            .filter { it.sourceId in idSet || it.targetId in idSet }
            .forEach { edgeDao.delete(it) }
        entityIds.forEach { id -> membershipDao.deleteByGraphIdAndEntityId(graphId, id) }
    }

    private fun applyGraphEdgeUpserts(graphId: UUID, edges: List<Edge>) {
        for (edge in edges) {
            val id = requireNotNull(edge.id)
            val existing = edgeDao.findById(id)
            val now = java.time.Instant.now()
            val record = existing ?: EdgeRecord(id = id, createdAt = now, updatedAt = now)
            record.graphId = graphId
            record.sourceId = edge.source
            record.targetId = edge.target
            record.role = edge.role
            record.type = edge.type
            record.schemaVersion = edge.schemaVersion
            record.properties = edge.properties?.toMutableMap()
            record.updatedAt = now
            edgeDao.save(record)
        }
    }

    private fun requireGraphExists(graphId: UUID) {
        if (!graphDao.existsById(graphId)) {
            throw GraphException(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId")
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
            membershipDao.saveAll(
                entityIds.map { GraphMembershipRecord(graphId = graphId, entityId = it) },
            )
        }
        val currentEdgeIds = edgeDao.findByGraphId(graphId).mapNotNullTo(linkedSetOf()) { it.id }
        val toRemove = currentEdgeIds - edgeIds
        if (toRemove.isNotEmpty()) {
            edgeDao.deleteAllById(toRemove)
        }
        for (edgeId in edgeIds) {
            val edge = edgeDao.findById(edgeId) ?: throw GraphException(
                code = "GRAPH_EDGE_MISSING",
                message = "Edge not found: $edgeId",
            )
            edge.graphId = graphId
            edgeDao.save(edge)
        }
    }

    private fun validateMembership(entityIds: Set<UUID>, edgeIds: Set<UUID>) {
        for (entityId in entityIds) {
            if (!entityDao.existsById(entityId)) {
                throw GraphException(
                    code = "GRAPH_ENTITY_MISSING",
                    message = "Entity not found: $entityId",
                )
            }
        }
        for (edgeId in edgeIds) {
            val edge = edgeDao.findById(edgeId)
                ?: throw GraphException(
                    code = "GRAPH_EDGE_MISSING",
                    message = "Edge not found: $edgeId",
                )
            if (edge.sourceId !in entityIds || edge.targetId !in entityIds) {
                throw GraphException(
                    code = "GRAPH_EDGE_ENDPOINTS",
                    message = "Edge $edgeId endpoints must both be subgraph entity members",
                )
            }
        }
    }

    private fun resolve(header: GraphRecord): ResolvedGraph {
        val entityIds = membershipDao.findByGraphId(header.id).map { it.entityId }
        val entities = if (entityIds.isEmpty()) {
            emptyList()
        } else {
            entityDao.findAllById(entityIds).map { it.toDomain() }
        }
        val edges = edgeDao.findByGraphId(header.id).map { it.toDomain() }
        return ResolvedGraph(
            id = header.id,
            annotations = header.annotations.toMap(),
            contents = GraphContents(entities = entities, edges = edges),
            createdAt = header.createdAt,
            updatedAt = header.updatedAt,
        )
    }

    /** Bump graph HEAD `updated_at`. No-op if the header is gone. */
    fun touch(graphId: UUID) = uow.write {
        val header = graphDao.findById(graphId) ?: return@write
        header.updatedAt = java.time.Instant.now()
        graphDao.save(header)
    }

    private fun GraphRecord.toHeader() = GraphHeader(
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
        internal fun matchesSearchText(header: GraphHeader, q: String): Boolean {
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
