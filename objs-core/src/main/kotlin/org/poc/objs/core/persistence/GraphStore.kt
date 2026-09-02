package org.poc.objs.core.persistence

import jakarta.persistence.EntityManager
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.GraphMutation
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.graphMutation
import org.poc.objs.core.domain.CatalogSupport
import org.poc.objs.core.domain.DuplicateGroup
import org.poc.objs.core.domain.IdentityProjection
import org.poc.objs.core.domain.PageRequest
import org.poc.objs.core.domain.SchemaCatalog
import org.poc.objs.core.domain.SchemaUsage
import org.poc.objs.core.domain.PagedEntities
import org.poc.objs.core.match.AllGraphsMatcher
import org.poc.objs.core.match.ChainedMatcher
import org.poc.objs.core.match.EntityDomainCandidate
import org.poc.objs.core.match.GraphExprMatcher
import org.poc.objs.core.match.GraphIdsMatcher
import org.poc.objs.core.match.Matcher
import org.poc.objs.core.match.ObjExprMatcher
import org.poc.objs.core.validation.EntityTypeLookup
import org.poc.objs.core.validation.PersistGate
import org.poc.objs.core.validation.ValidationException
import org.poc.objs.core.validation.ValidationIssue
import org.poc.objs.core.validation.ValidationResult
import org.poc.objs.core.validation.Validator
import org.poc.objs.core.versioning.VersioningStrategy
import org.poc.objs.core.versioning.VersionedKind
import org.poc.objs.core.versioning.VersioningContext
import org.poc.objs.core.versioning.VersioningOp
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Persistence facade: batch pool write with two-stage gate; load, and graph-scoped select.
 *
 * [select] / `selectInGraph` enforce G-G16 (no whole-pool-as-a-graph): a bare `obj-expr`
 * (or any matcher whose first stage is not `all` / `graph-expr`) is rejected — see [select].
 * Pool-wide object search (orphans included) uses [selectFromPool] instead.
 */
@Service
class GraphStore(
    private val entityRepository: EntityRepository,
    private val edgeRepository: EdgeRepository,
    private val validator: Validator,
    private val entityManager: EntityManager,
    private val namedGraphs: NamedGraphStore,
    private val poolReader: PoolEntityReader,
    private val schemas: SchemaCatalog,
    private val catalog: CatalogSupport,
    private val versioning: VersioningStrategy,
) {
    private fun gate(): PersistGate = PersistGate(
        validator = validator,
        storeLookup = EntityTypeLookup { id -> entityRepository.findById(id).map { it.type }.orElse(null) },
        existsEntity = { id -> entityRepository.existsById(id) },
        existsEdge = { id -> edgeRepository.existsById(id) },
    )

    /**
     * Batch upsert. On success, [graph] is mutated in place with all entity/edge ids assigned (G-R5).
     */
    @Transactional
    fun write(graph: Graph): ValidationResult =
        mutate(GraphMutation.of(graph))

    /** Dry-run write validation (may assign temporary ids on [graph] via the persist gate). */
    @Transactional(readOnly = true)
    fun validate(graph: Graph): ValidationResult =
        validateMutation(GraphMutation.of(graph))

    /**
     * Transactional mutate: validate projected state, then explicit edge unsets,
     * entity unsets (cascade incident edges), then sets.
     *
     * Same id in unset and set: set wins in the final store state.
     */
    @Transactional
    fun mutate(mutation: GraphMutation): ValidationResult {
        val result = validateMutation(mutation)
        if (!result.isValid) {
            return result
        }
        applyDeletes(mutation)
        applyUpserts(mutation.graph())
        return ValidationResult.ok()
    }

    /**
     * Dry-run mutation validation (may assign temporary ids on set entities/edges).
     * Edge endpoint lookup ignores entities scheduled for unset unless also set.
     */
    @Transactional(readOnly = true)
    fun validateMutation(mutation: GraphMutation): ValidationResult {
        val g = gate()
        val issues = mutableListOf<ValidationIssue>()
        for (id in mutation.edges.unset.distinct()) {
            issues.addAll(g.validateDeleteEdge(id).issues)
        }
        for (id in mutation.entities.unset.distinct()) {
            issues.addAll(g.validateDeleteEntity(id).issues)
        }
        if (issues.isNotEmpty()) {
            return ValidationResult.of(issues)
        }
        if (!mutation.hasSets()) {
            return ValidationResult.ok()
        }
        val graph = mutation.graph()
        val stage1 = validator.validateEntities(graph.entities)
        if (!stage1.isValid) {
            return stage1
        }
        g.prepareIds(graph)
        val unsetEntities = mutation.entities.unset.toSet()
        val projectedStore = EntityTypeLookup { id ->
            if (id in unsetEntities) {
                null
            } else {
                entityRepository.findById(id).map { it.type }.orElse(null)
            }
        }
        val lookup = validator.combinedLookup(graph.entities, projectedStore)
        val edgeResult = validator.validateEdges(graph.edges, lookup)
        if (!edgeResult.isValid) {
            return edgeResult
        }
        val identityIssues = mutableListOf<ValidationIssue>()
        graph.entities.forEachIndexed { index, entity ->
            val id = entity.id ?: return@forEachIndexed
            if (id in unsetEntities) return@forEachIndexed
            val stored = entityRepository.findById(id).orElse(null)?.toDomain() ?: return@forEachIndexed
            identityIssues += validator.validateEntityIdentifierImmutability(
                stored,
                entity,
                path = "entities.set[$index]",
            )
        }
        graph.edges.forEachIndexed { index, edge ->
            val id = edge.id ?: return@forEachIndexed
            val stored = edgeRepository.findById(id).orElse(null)?.toDomain() ?: return@forEachIndexed
            identityIssues += validator.validateEdgeIdentifierImmutability(
                stored,
                edge,
                path = "edges.set[$index]",
            )
        }
        return if (identityIssues.isEmpty()) {
            ValidationResult.ok()
        } else {
            ValidationResult.of(identityIssues)
        }
    }

    @Transactional
    fun deleteEntity(id: UUID): ValidationResult =
        mutate(graphMutation { entities { unset(id) } })

    @Transactional
    fun deleteEdge(id: UUID): ValidationResult =
        mutate(graphMutation { edges { unset(id) } })

    /**
     * All-or-nothing batch delete (G-R3/G-R4). Thin shim over [mutate].
     */
    @Transactional
    fun delete(
        entityIds: Collection<UUID> = emptyList(),
        edgeIds: Collection<UUID> = emptyList(),
    ): ValidationResult {
        if (entityIds.isEmpty() && edgeIds.isEmpty()) {
            return ValidationResult.of(
                ValidationIssue(
                    code = "DELETE_EMPTY",
                    message = "At least one entityId or edgeId is required",
                ),
            )
        }
        return mutate(
            graphMutation {
                if (entityIds.isNotEmpty()) entities { unset(entityIds) }
                if (edgeIds.isNotEmpty()) edges { unset(edgeIds) }
            },
        )
    }

    private fun applyDeletes(mutation: GraphMutation) {
        val touchedGraphs = linkedSetOf<UUID>()
        for (id in mutation.edges.unset.distinct()) {
            val edge = edgeRepository.findById(id).orElse(null) ?: continue
            touchedGraphs += edge.graphId
            edgeRepository.delete(edge)
        }
        val entityIds = mutation.entities.unset.distinct().filter { entityRepository.existsById(it) }
        if (entityIds.isNotEmpty()) {
            edgeRepository.findBySourceIdInOrTargetIdIn(entityIds, entityIds)
                .forEach { edge ->
                    touchedGraphs += edge.graphId
                    edgeRepository.delete(edge)
                }
            entityIds.forEach { entityRepository.deleteById(it) }
        }
        touchedGraphs.forEach { namedGraphs.touch(it) }
    }

    private fun applyUpserts(graph: Graph) {
        upsertEntities(graph.entities)
        for (edge in graph.edges) {
            val id = requireNotNull(edge.id)
            val existing = edgeRepository.findById(id).orElse(null)
            val now = java.time.Instant.now()
            val record = existing ?: EdgeRecord(id = id, createdAt = now, updatedAt = now)
            record.graphId = edge.graphId ?: existing?.graphId
                ?: error("edge $id requires graphId when creating a new edge")
            record.sourceId = edge.source
            record.targetId = edge.target
            record.role = edge.role
            record.type = edge.type
            record.schemaVersion = edge.schemaVersion
            record.properties = edge.properties?.toMutableMap()
            record.updatedAt = now
            edgeRepository.save(record)
            versioning.shouldCapture(
                VersioningContext(
                    graphId = record.graphId,
                    kind = VersionedKind.EDGE,
                    op = if (existing == null) VersioningOp.CREATE else VersioningOp.UPDATE,
                    parentId = id,
                    headVersion = record.headVersion,
                ),
            )
            namedGraphs.touch(record.graphId)
        }
    }

    @Transactional(readOnly = true)
    fun loadAll(): Graph {
        val entities = entityRepository.findAll().map { it.toDomain() }.toMutableList()
        val edges = edgeRepository.findAll().map { it.toDomain() }.toMutableList()
        return Graph(entities, edges)
    }

    /** Fetch a single pool entity, or null if it does not exist (WI-004: `GET /entities/{id}`). */
    @Transactional(readOnly = true)
    fun getEntity(id: UUID): Entity? = entityRepository.findById(id).map { it.toDomain() }.orElse(null)

    /** List all pool entities, ungrouped by graph membership (WI-004: `GET /entities`). */
    @Transactional(readOnly = true)
    fun listEntities(): List<Entity> = entityRepository.findAll().map { it.toDomain() }

    /**
     * Filter the entity **pool** with `obj-expr` (or a chain of `obj-expr` stages).
     * Includes orphans (no graph membership). Returns entities only — edges are graph-local
     * and are never induced across the whole pool (G-G16).
     *
     * Rejects stage-0 `all` / `graph-expr` (use [select] / [selectInGraph] for those).
     */
    @Transactional(readOnly = true)
    fun selectFromPool(matcher: Matcher): GraphContents {
        entityManager.flush()
        val stages = flattenStages(matcher)
        if (stages.isEmpty()) {
            throw ValidationException(
                "matcher-dsl",
                ValidationResult.of(
                    ValidationIssue(
                        code = "MATCHER_OBJ_EXPR_REQUIRED",
                        message = "pool query requires at least one obj-expr stage",
                        path = "$",
                    ),
                ),
            )
        }
        for (stage in stages) {
            if (stage !is ObjExprMatcher) {
                throw ValidationException(
                    "matcher-dsl",
                    ValidationResult.of(
                        ValidationIssue(
                            code = "MATCHER_POOL_OBJ_EXPR_ONLY",
                            message = "pool query accepts only obj-expr (or a chain of obj-expr); " +
                                "use /graphs/query for all / graph-expr",
                            path = "$",
                        ),
                    ),
                )
            }
        }
        val entities = poolReader.selectEntities(matcher).map { it.toDomain() }
        return GraphContents(entities = entities, edges = emptyList())
    }

    /**
     * Paged pool select (G-A6). Order is `type`, then `id`. Total is the full match count
     * (after slow-path filter when the matcher does not fully push down).
     */
    @Transactional(readOnly = true)
    fun selectFromPool(matcher: Matcher, page: PageRequest): PagedEntities {
        val all = selectFromPool(matcher).entities.sortedWith(compareBy({ it.type }, { it.id.toString() }))
        val from = page.offset.coerceAtMost(all.size)
        val to = (from + page.size).coerceAtMost(all.size)
        return PagedEntities(
            items = all.subList(from, to),
            total = all.size.toLong(),
            page = page.page,
            size = page.size,
        )
    }

    /** Pool-wide `type → count` (G-A7). */
    @Transactional(readOnly = true)
    fun countByType(): Map<String, Long> =
        entityRepository.countGroupedByType().associate { it.getType() to it.getCnt() }

    /** Member counts by type for [graphId] (G-A7). */
    @Transactional(readOnly = true)
    fun countByType(graphId: UUID): Map<String, Long> =
        entityRepository.countGroupedByTypeInGraph(graphId).associate { it.getType() to it.getCnt() }

    /**
     * Pool entities of [type] whose [IdentityProjection] equals [identityMap].
     * Empty or all-unset identity → empty (G-A13). Scan-by-type then group (G-A4).
     */
    @Transactional(readOnly = true)
    fun findEntitiesByIdentity(
        type: String,
        identityMap: Map<String, @JvmSuppressWildcards Any?>,
    ): List<Entity> {
        val wanted = identityMap.filterValues { !IdentityProjection.isUnset(it) }
        if (wanted.isEmpty()) return emptyList()
        return entitiesOfType(type).filter { entity ->
            identityOf(entity) == wanted
        }
    }

    /**
     * Identity clusters of [type] with more than one pool entity. Empty identity omitted (G-A13).
     */
    @Transactional(readOnly = true)
    fun findDuplicateGroups(type: String): List<DuplicateGroup> {
        val groups = linkedMapOf<String, MutableList<Entity>>()
        val identities = linkedMapOf<String, Map<String, Any?>>()
        for (entity in entitiesOfType(type)) {
            val identity = identityOf(entity) ?: continue
            if (identity.isEmpty()) continue
            val key = identity.entries.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" }
            groups.getOrPut(key) { mutableListOf() }.add(entity)
            identities.putIfAbsent(key, identity)
        }
        return groups.entries
            .filter { it.value.size > 1 }
            .map { (key, members) ->
                DuplicateGroup(type = type, identity = identities.getValue(key), entities = members)
            }
    }

    private fun entitiesOfType(type: String): List<Entity> {
        val trimmed = type.trim()
        if (trimmed.isEmpty()) return emptyList()
        val escaped = trimmed.replace("\\", "\\\\").replace("'", "\\'")
        return selectFromPool(ObjExprMatcher("type == '$escaped'")).entities
    }

    private fun identityOf(entity: Entity): Map<String, Any?>? {
        val schema =
            schemas.get(entity.type, entity.schemaVersion)?.takeIf { it.usage == SchemaUsage.ENTITY }
                ?: catalog.latestEntitySchema(entity.type)
                ?: return null
        return IdentityProjection.project(schema.contentSchema, entity.payload)
    }

    /**
     * Entity-only pool upsert (no edges, no membership). Shared by [applyUpserts] and
     * [NamedGraphStore.mutate]'s graph-scoped `entities.set` step.
     */
    @Transactional
    fun upsertEntities(entities: Collection<Entity>) {
        for (entity in entities) {
            val id = requireNotNull(entity.id)
            val existing = entityRepository.findById(id).orElse(null)
            val now = java.time.Instant.now()
            val record = existing ?: EntityRecord(id = id, createdAt = now, updatedAt = now)
            record.type = entity.type
            record.schemaVersion = entity.schemaVersion
            record.payload = entity.payload.toMutableMap()
            record.annotations = entity.annotations.toMutableMap()
            record.updatedAt = now
            entityRepository.save(record)
            versioning.shouldCapture(
                VersioningContext(
                    graphId = null,
                    kind = VersionedKind.ENTITY,
                    op = if (existing == null) VersioningOp.CREATE else VersioningOp.UPDATE,
                    parentId = id,
                    headVersion = record.headVersion,
                ),
            )
        }
    }

    /**
     * Select the union of stored members + graph-local edges of graph(s) matched by a stage-0
     * `all`, `graph-expr`, or `graphs-in`, filtered by any later stages (typically `obj-expr`).
     * Entities and edges are **distinct by id** across the union.
     *
     * G-G16: there is no global graph, so a bare `obj-expr` (or any chain not starting with
     * `all` / `graph-expr` / `graphs-in`) would otherwise silently scan the whole pool as if it
     * were one graph. Reject it instead with `MATCHER_GRAPH_SCOPE_REQUIRED`; callers with a known
     * graph id should use [selectInGraph], or start their chain with `all` / `graph-expr` /
     * `graphs-in`.
     */
    @Transactional(readOnly = true)
    fun select(matcher: Matcher): GraphContents {
        // JDBC/JPA reads share the Spring transaction connection; flush pending writes first.
        entityManager.flush()
        val stages = flattenStages(matcher)
        val first = stages.first()
        if (first !is GraphExprMatcher &&
            first !is AllGraphsMatcher &&
            first !is GraphIdsMatcher
        ) {
            throw ValidationException(
                "matcher-dsl",
                ValidationResult.of(
                    ValidationIssue(
                        code = "MATCHER_GRAPH_SCOPE_REQUIRED",
                        message = "select requires stage-0 'all', 'graph-expr', or 'graphs-in' to fix a graph " +
                            "scope (there is no whole-pool graph); use selectInGraph(graphId, matcher) " +
                            "for a known graph, or start the chain with all / graph-expr / graphs-in",
                        path = "$",
                    ),
                ),
            )
        }
        return selectAcrossGraphs(stages)
    }

    /**
     * Filter a **known** graph's stored members/edges by [matcher] (typically `obj-expr` or a
     * chain of `obj-expr` stages). The graph is fixed by [graphId], so unscoped matchers are safe
     * here — this is the graph-scoped counterpart to [select]'s `graph-expr` requirement.
     */
    @Transactional(readOnly = true)
    fun selectInGraph(graphId: UUID, matcher: Matcher): GraphContents {
        entityManager.flush()
        val resolved = namedGraphs.get(graphId)
            ?: throw ValidationException(
                "graph",
                ValidationResult.of(
                    ValidationIssue(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId", path = "graphId"),
                ),
            )
        val stages = flattenStages(matcher)
        var entities = resolved.contents.entities
        if (stages.isNotEmpty()) {
            entities = entities.filter { entity ->
                val candidate = EntityDomainCandidate(entity)
                stages.all { it.matches(candidate) }
            }
        }
        val selectedIds = entities.mapNotNullTo(linkedSetOf()) { it.id }
        val edges = resolved.contents.edges.filter { it.source in selectedIds && it.target in selectedIds }
        return GraphContents(entities = entities, edges = edges)
    }

    /**
     * Same as [selectInGraph] but against a reconstructed deep graph version (Note 2 pin).
     */
    @Transactional(readOnly = true)
    fun selectInGraphVersion(graphId: UUID, version: Long, matcher: Matcher): GraphContents {
        entityManager.flush()
        val resolved = namedGraphs.getGraphVersion(graphId, version)
        val stages = flattenStages(matcher)
        var entities = resolved.contents.entities
        if (stages.isNotEmpty()) {
            entities = entities.filter { entity ->
                val candidate = EntityDomainCandidate(entity)
                stages.all { it.matches(candidate) }
            }
        }
        val selectedIds = entities.mapNotNullTo(linkedSetOf()) { it.id }
        val edges = resolved.contents.edges.filter { it.source in selectedIds && it.target in selectedIds }
        return GraphContents(entities = entities, edges = edges)
    }

    /** Reconstruct a deep graph version (contents only). */
    @Transactional(readOnly = true)
    fun loadGraphVersion(graphId: UUID, version: Long) = namedGraphs.getGraphVersion(graphId, version)

    @Transactional(readOnly = true)
    fun listEntityVersions(entityId: UUID) = namedGraphs.listEntityVersions(entityId)

    @Transactional(readOnly = true)
    fun entityVersionStats(entityId: UUID, recent: Int = 5) = namedGraphs.entityVersionStats(entityId, recent)

    @Transactional(readOnly = true)
    fun getEntityVersion(entityId: UUID, version: Long) = namedGraphs.getEntityVersion(entityId, version)

    /** Live HEAD graphs containing [entityId] (no pins). See [NamedGraphStore.listLiveGraphHeadersForEntity]. */
    @Transactional(readOnly = true)
    @JvmOverloads
    fun listLiveGraphHeadersForEntity(
        entityId: UUID,
        q: String? = null,
        limit: Int? = null,
    ) = namedGraphs.listLiveGraphHeadersForEntity(entityId, q, limit)

    @Transactional(readOnly = true)
    fun listEdgeVersions(edgeId: UUID) = namedGraphs.listEdgeVersions(edgeId)

    @Transactional(readOnly = true)
    fun edgeVersionStats(edgeId: UUID, recent: Int = 5) = namedGraphs.edgeVersionStats(edgeId, recent)

    @Transactional(readOnly = true)
    fun getEdgeVersion(edgeId: UUID, version: Long) = namedGraphs.getEdgeVersion(edgeId, version)

    /**
     * Union of stored members/edges of every graph selected by stage-0 `all`, `graph-expr`,
     * or `graphs-in`, then optional later-stage entity filters. Distinct by entity/edge id.
     */
    private fun selectAcrossGraphs(stages: List<Matcher>): GraphContents {
        val first = stages.first()
        val packs = when (first) {
            is AllGraphsMatcher ->
                namedGraphs.list().mapNotNull { item -> namedGraphs.get(item.id) }
            is GraphExprMatcher ->
                namedGraphs.matchingHeaders(first).mapNotNull { item -> namedGraphs.get(item.id) }
            is GraphIdsMatcher ->
                first.graphIds.mapNotNull { id -> namedGraphs.get(id) }
            else -> emptyList()
        }
        val entityById = linkedMapOf<UUID, Entity>()
        val edgeById = linkedMapOf<UUID, Edge>()
        for (pack in packs) {
            for (entity in pack.contents.entities) {
                val id = entity.id ?: continue
                entityById[id] = entity
            }
            for (edge in pack.contents.edges) {
                val id = edge.id ?: continue
                edgeById[id] = edge
            }
        }
        var entities = entityById.values.toList()
        val filters = stages.drop(1)
        if (filters.isNotEmpty()) {
            entities = entities.filter { entity ->
                val candidate = EntityDomainCandidate(entity)
                filters.all { it.matches(candidate) }
            }
        }
        val selectedIds = entities.mapNotNullTo(linkedSetOf()) { it.id }
        val edges = edgeById.values.filter { it.source in selectedIds && it.target in selectedIds }
        return GraphContents(entities = entities, edges = edges)
    }

    private fun flattenStages(matcher: Matcher): List<Matcher> =
        when (matcher) {
            is ChainedMatcher -> matcher.matchers.flatMap(::flattenStages)
            else -> listOf(matcher)
        }
}

fun EntityRecord.toDomain() = Entity(
    id = id,
    type = type,
    schemaVersion = schemaVersion,
    payload = payload.toMutableMap(),
    annotations = annotations.toMutableMap(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    headVersion = headVersion,
)

fun EdgeRecord.toDomain() = Edge(
    id = id,
    graphId = graphId,
    source = sourceId,
    target = targetId,
    role = role,
    type = type,
    schemaVersion = schemaVersion,
    properties = properties?.toMutableMap(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    headVersion = headVersion,
)
