package org.poc.objs.core.match

import java.util.UUID

/**
 * Produces the initial (or only) set of entity candidates for graph selection.
 * Sources own backend-specific access (table scan, SQL pushdown, in-memory list, …).
 */
fun interface BoMCandidateSource {
    /**
     * Materialize candidates. [checkBudget] must be invoked regularly for long scans.
     */
    fun collect(checkBudget: () -> Unit): List<BoMEntityMatchCandidate>
}

/**
 * Loads edge candidates after entity filters have produced [selectedEntityIds].
 *
 * Source-aware strategies (e.g. annotation containment) may fetch the max induced edge
 * set via SQL joins on the same predicate, then retain only edges among survivors.
 * When absent, executors fall back to id-bounded `IN` queries.
 */
fun interface BoMEdgeCandidateStrategy {
    fun collect(
        selectedEntityIds: Set<UUID>,
        checkBudget: () -> Unit,
    ): List<BoMEdgeMatchCandidate>
}

/**
 * Entity source that also provides an [edgeStrategy] for induced edges.
 */
interface BoMCandidateSourceWithEdges : BoMCandidateSource {
    val edgeStrategy: BoMEdgeCandidateStrategy
}

fun BoMCandidateSource.edgeStrategyOrNull(): BoMEdgeCandidateStrategy? =
    (this as? BoMCandidateSourceWithEdges)?.edgeStrategy

/**
 * Backend capabilities used when a matcher offers itself as a [BoMCandidateSource].
 */
interface BoMEntityCandidateBackend {
    val isPostgres: Boolean

    fun allEntitiesSource(): BoMCandidateSource

    /**
     * Postgres JSON containment for a single equality map (`annotations @> filter`).
     */
    fun annotationContainmentSource(filter: Map<String, String>): BoMCandidateSource? =
        annotationContainmentAnySource(listOf(filter))

    /**
     * Postgres source for `annotations @> f1 OR … OR @> fn` (DNF of equality maps).
     * Empty [disjuncts] is unsupported (returns null).
     */
    fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): BoMCandidateSource?
}

/**
 * Matcher that can provide an initial candidate source for a given backend.
 * When [toCandidateSource] returns null, executors fall back to [BoMEntityCandidateBackend.allEntitiesSource]
 * and apply this matcher as a filter.
 */
interface BoMSourceCapableMatcher : BoMMatcher {
    fun toCandidateSource(backend: BoMEntityCandidateBackend): BoMCandidateSource?
}

/** In-memory "all entities" source for [org.poc.objs.core.subgraph.BoMSubgraphSelector]. */
class BoMInMemoryAllEntitiesSource(
    private val candidates: List<BoMEntityMatchCandidate>,
) : BoMCandidateSource {
    override fun collect(checkBudget: () -> Unit): List<BoMEntityMatchCandidate> {
        checkBudget()
        return candidates
    }
}
