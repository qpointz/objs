package org.poc.objs.core.persistence

import jakarta.persistence.EntityManager
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMGraphDelete
import org.poc.objs.core.domain.BoMGraphMutation
import org.poc.objs.core.domain.BoMGraphUpsert
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.match.BoMAllGraphsMatcher
import org.poc.objs.core.match.BoMChainedMatcher
import org.poc.objs.core.match.BoMEntityDomainCandidate
import org.poc.objs.core.match.BoMGraphExprMatcher
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.match.BoMObjExprMatcher
import org.poc.objs.core.validation.BoMEntityTypeLookup
import org.poc.objs.core.validation.BoMPersistGate
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.poc.objs.core.validation.BoMValidator
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
class BoMGraphStore(
    private val entityRepository: BoMEntityRepository,
    private val edgeRepository: BoMEdgeRepository,
    private val validator: BoMValidator,
    private val entityManager: EntityManager,
    private val namedGraphs: BoMNamedGraphStore,
    private val poolReader: BoMPoolEntityReader,
) {
    private fun gate(): BoMPersistGate = BoMPersistGate(
        validator = validator,
        storeLookup = BoMEntityTypeLookup { id -> entityRepository.findById(id).map { it.type }.orElse(null) },
        existsEntity = { id -> entityRepository.existsById(id) },
        existsEdge = { id -> edgeRepository.existsById(id) },
    )

    /**
     * Batch upsert. On success, [graph] is mutated in place with all entity/edge ids assigned (G-R5).
     */
    @Transactional
    fun write(graph: BoMGraph): BoMValidationResult =
        mutate(BoMGraphMutation(upsert = BoMGraphUpsert(entities = graph.entities, edges = graph.edges)))

    /** Dry-run write validation (may assign temporary ids on [graph] via the persist gate). */
    @Transactional(readOnly = true)
    fun validate(graph: BoMGraph): BoMValidationResult =
        validateMutation(BoMGraphMutation(upsert = BoMGraphUpsert(entities = graph.entities, edges = graph.edges)))

    /**
     * Transactional mutate: validate projected state, then explicit edge deletes,
     * entity deletes (cascade incident edges), then upserts.
     *
     * Same id in delete and upsert: upsert wins in the final store state.
     */
    @Transactional
    fun mutate(mutation: BoMGraphMutation): BoMValidationResult {
        val result = validateMutation(mutation)
        if (!result.isValid) {
            return result
        }
        applyDeletes(mutation)
        applyUpserts(mutation.graph())
        return BoMValidationResult.ok()
    }

    /**
     * Dry-run mutation validation (may assign temporary ids on upsert entities/edges).
     * Edge endpoint lookup ignores entities scheduled for delete unless also upserted.
     */
    @Transactional(readOnly = true)
    fun validateMutation(mutation: BoMGraphMutation): BoMValidationResult {
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
        val deleted = mutation.delete.entities.toSet()
        val projectedStore = BoMEntityTypeLookup { id ->
            if (id in deleted) {
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
        val identityIssues = mutableListOf<BoMValidationIssue>()
        graph.entities.forEachIndexed { index, entity ->
            val id = entity.id ?: return@forEachIndexed
            if (id in deleted) return@forEachIndexed
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
        return if (identityIssues.isEmpty()) {
            BoMValidationResult.ok()
        } else {
            BoMValidationResult.of(identityIssues)
        }
    }

    @Transactional
    fun deleteEntity(id: UUID): BoMValidationResult =
        mutate(BoMGraphMutation(delete = BoMGraphDelete(entities = mutableListOf(id))))

    @Transactional
    fun deleteEdge(id: UUID): BoMValidationResult =
        mutate(BoMGraphMutation(delete = BoMGraphDelete(edges = mutableListOf(id))))

    /**
     * All-or-nothing batch delete (G-R3/G-R4). Thin shim over [mutate].
     */
    @Transactional
    fun delete(
        entityIds: Collection<UUID> = emptyList(),
        edgeIds: Collection<UUID> = emptyList(),
    ): BoMValidationResult {
        if (entityIds.isEmpty() && edgeIds.isEmpty()) {
            return BoMValidationResult.of(
                BoMValidationIssue(
                    code = "DELETE_EMPTY",
                    message = "At least one entityId or edgeId is required",
                ),
            )
        }
        return mutate(
            BoMGraphMutation(
                delete = BoMGraphDelete(
                    entities = entityIds.toMutableList(),
                    edges = edgeIds.toMutableList(),
                ),
            ),
        )
    }

    private fun applyDeletes(mutation: BoMGraphMutation) {
        for (id in mutation.delete.edges.distinct()) {
            if (edgeRepository.existsById(id)) {
                edgeRepository.deleteById(id)
            }
        }
        val entityIds = mutation.delete.entities.distinct().filter { entityRepository.existsById(it) }
        if (entityIds.isEmpty()) {
            return
        }
        edgeRepository.findBySourceIdInOrTargetIdIn(entityIds, entityIds)
            .forEach { edgeRepository.delete(it) }
        entityIds.forEach { entityRepository.deleteById(it) }
    }

    private fun applyUpserts(graph: BoMGraph) {
        upsertEntities(graph.entities)
        for (edge in graph.edges) {
            val id = requireNotNull(edge.id)
            val existing = edgeRepository.findById(id).orElse(null)
            val record = existing ?: BoMEdgeRecord(id = id)
            record.graphId = edge.graphId ?: existing?.graphId
                ?: error("edge $id requires graphId when creating a new edge")
            record.sourceId = edge.source
            record.targetId = edge.target
            record.role = edge.role
            record.type = edge.type
            record.schemaVersion = edge.schemaVersion
            record.properties = edge.properties?.toMutableMap()
            edgeRepository.save(record)
        }
    }

    @Transactional(readOnly = true)
    fun loadAll(): BoMGraph {
        val entities = entityRepository.findAll().map { it.toDomain() }.toMutableList()
        val edges = edgeRepository.findAll().map { it.toDomain() }.toMutableList()
        return BoMGraph(entities, edges)
    }

    /** Fetch a single pool entity, or null if it does not exist (WI-004: `GET /entities/{id}`). */
    @Transactional(readOnly = true)
    fun getEntity(id: UUID): BoMEntity? = entityRepository.findById(id).map { it.toDomain() }.orElse(null)

    /** List all pool entities, ungrouped by graph membership (WI-004: `GET /entities`). */
    @Transactional(readOnly = true)
    fun listEntities(): List<BoMEntity> = entityRepository.findAll().map { it.toDomain() }

    /**
     * Filter the entity **pool** with `obj-expr` (or a chain of `obj-expr` stages).
     * Includes orphans (no graph membership). Returns entities only — edges are graph-local
     * and are never induced across the whole pool (G-G16).
     *
     * Rejects stage-0 `all` / `graph-expr` (use [select] / [selectInGraph] for those).
     */
    @Transactional(readOnly = true)
    fun selectFromPool(matcher: BoMMatcher): BoMGraphContents {
        entityManager.flush()
        val stages = flattenStages(matcher)
        if (stages.isEmpty()) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_OBJ_EXPR_REQUIRED",
                        message = "pool query requires at least one obj-expr stage",
                        path = "$",
                    ),
                ),
            )
        }
        for (stage in stages) {
            if (stage !is BoMObjExprMatcher) {
                throw BoMValidationException(
                    "matcher-dsl",
                    BoMValidationResult.of(
                        BoMValidationIssue(
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
        return BoMGraphContents(entities = entities, edges = emptyList())
    }

    /**
     * Entity-only pool upsert (no edges, no membership). Shared by [applyUpserts] and
     * [BoMNamedGraphStore.mutate]'s graph-scoped `upsert.entities` step.
     */
    @Transactional
    fun upsertEntities(entities: Collection<BoMEntity>) {
        for (entity in entities) {
            val id = requireNotNull(entity.id)
            val record = entityRepository.findById(id).orElseGet { BoMEntityRecord(id = id) }
            record.type = entity.type
            record.schemaVersion = entity.schemaVersion
            record.payload = entity.payload.toMutableMap()
            record.annotations = entity.annotations.toMutableMap()
            entityRepository.save(record)
        }
    }

    /**
     * Select the union of stored members + graph-local edges of graph(s) matched by a stage-0
     * `all` or `graph-expr`, filtered by any later stages (typically `obj-expr`).
     * Entities and edges are **distinct by id** across the union.
     *
     * G-G16: there is no global graph, so a bare `obj-expr` (or any chain not starting with
     * `all` / `graph-expr`) would otherwise silently scan the whole pool as if it were one graph.
     * Reject it instead with `MATCHER_GRAPH_SCOPE_REQUIRED`; callers with a known graph id should
     * use [selectInGraph], or start their chain with `all` / `graph-expr`.
     */
    @Transactional(readOnly = true)
    fun select(matcher: BoMMatcher): BoMGraphContents {
        // JDBC/JPA reads share the Spring transaction connection; flush pending writes first.
        entityManager.flush()
        val stages = flattenStages(matcher)
        val first = stages.first()
        if (first !is BoMGraphExprMatcher && first !is BoMAllGraphsMatcher) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_GRAPH_SCOPE_REQUIRED",
                        message = "select requires stage-0 'all' or 'graph-expr' to fix a graph " +
                            "scope (there is no whole-pool graph); use selectInGraph(graphId, matcher) " +
                            "for a known graph, or start the chain with all / graph-expr",
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
    fun selectInGraph(graphId: UUID, matcher: BoMMatcher): BoMGraphContents {
        entityManager.flush()
        val resolved = namedGraphs.get(graphId)
            ?: throw BoMValidationException(
                "graph",
                BoMValidationResult.of(
                    BoMValidationIssue(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId", path = "graphId"),
                ),
            )
        val stages = flattenStages(matcher)
        var entities = resolved.contents.entities
        if (stages.isNotEmpty()) {
            entities = entities.filter { entity ->
                val candidate = BoMEntityDomainCandidate(entity)
                stages.all { it.matches(candidate) }
            }
        }
        val selectedIds = entities.mapNotNullTo(linkedSetOf()) { it.id }
        val edges = resolved.contents.edges.filter { it.source in selectedIds && it.target in selectedIds }
        return BoMGraphContents(entities = entities, edges = edges)
    }

    /**
     * Union of stored members/edges of every graph selected by stage-0 `all` or `graph-expr`,
     * then optional later-stage entity filters. Distinct by entity/edge id.
     */
    private fun selectAcrossGraphs(stages: List<BoMMatcher>): BoMGraphContents {
        val first = stages.first()
        val packs = when (first) {
            is BoMAllGraphsMatcher ->
                namedGraphs.list().mapNotNull { item -> namedGraphs.get(item.id) }
            is BoMGraphExprMatcher ->
                namedGraphs.matchingHeaders(first).mapNotNull { item -> namedGraphs.get(item.id) }
            else -> emptyList()
        }
        val entityById = linkedMapOf<UUID, BoMEntity>()
        val edgeById = linkedMapOf<UUID, BoMEdge>()
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
                val candidate = BoMEntityDomainCandidate(entity)
                filters.all { it.matches(candidate) }
            }
        }
        val selectedIds = entities.mapNotNullTo(linkedSetOf()) { it.id }
        val edges = edgeById.values.filter { it.source in selectedIds && it.target in selectedIds }
        return BoMGraphContents(entities = entities, edges = edges)
    }

    private fun flattenStages(matcher: BoMMatcher): List<BoMMatcher> =
        when (matcher) {
            is BoMChainedMatcher -> matcher.matchers.flatMap(::flattenStages)
            else -> listOf(matcher)
        }
}

fun BoMEntityRecord.toDomain() = BoMEntity(
    id = id,
    type = type,
    schemaVersion = schemaVersion,
    payload = payload.toMutableMap(),
    annotations = annotations.toMutableMap(),
)

fun BoMEdgeRecord.toDomain() = BoMEdge(
    id = id,
    graphId = graphId,
    source = sourceId,
    target = targetId,
    role = role,
    type = type,
    schemaVersion = schemaVersion,
    properties = properties?.toMutableMap(),
)
