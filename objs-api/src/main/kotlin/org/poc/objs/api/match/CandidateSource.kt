package org.poc.objs.api.match

import java.util.UUID

/**
 * Produces the initial (or only) set of entity candidates for graph selection.
 * Sources own backend-specific access (table scan, SQL pushdown, in-memory list, …).
 */
fun interface CandidateSource {
    /**
     * Materialize candidates. [checkBudget] must be invoked regularly for long scans.
     */
    fun collect(checkBudget: () -> Unit): List<EntityMatchCandidate>
}

/**
 * Loads edge candidates after entity filters have produced [selectedEntityIds].
 *
 * Source-aware strategies (e.g. annotation containment) may fetch the max induced edge
 * set via SQL joins on the same predicate, then retain only edges among survivors.
 * When absent, executors fall back to id-bounded `IN` queries.
 */
fun interface EdgeCandidateStrategy {
    fun collect(
        selectedEntityIds: Set<UUID>,
        checkBudget: () -> Unit,
    ): List<EdgeMatchCandidate>
}

/**
 * Entity source that also provides an [edgeStrategy] for induced edges.
 */
interface CandidateSourceWithEdges : CandidateSource {
    val edgeStrategy: EdgeCandidateStrategy
}

fun CandidateSource.edgeStrategyOrNull(): EdgeCandidateStrategy? =
    (this as? CandidateSourceWithEdges)?.edgeStrategy

/**
 * Backend capabilities used when a matcher offers itself as a [CandidateSource].
 */
interface EntityCandidateBackend {
    val isPostgres: Boolean

    fun allEntitiesSource(): CandidateSource

    /**
     * Postgres JSON containment for a single equality map (`annotations @> filter`).
     */
    fun annotationContainmentSource(filter: Map<String, String>): CandidateSource? =
        annotationContainmentAnySource(listOf(filter))

    /**
     * Postgres source for `annotations @> f1 OR … OR @> fn` (DNF of equality maps).
     * Empty [disjuncts] is unsupported (returns null).
     */
    fun annotationContainmentAnySource(disjuncts: List<Map<String, String>>): CandidateSource?

    /**
     * Entities whose primary key is in [ids] (empty → empty source). Default: unsupported (null).
     */
    fun entityIdsSource(ids: List<java.util.UUID>): CandidateSource? = null

    /**
     * DNF pushdown for lowerable [ObjExprPushdown] (`==`/`!=` + `&&`/`||` over
     * type / id / schemaVersion / a.* / p.*). Default: unsupported (null → local eval).
     */
    fun objExprPushdownSource(plan: ObjExprPushdown): CandidateSource? = null
}

/**
 * Matcher that can provide an initial candidate source for a given backend.
 * When [toCandidateSource] returns null, executors fall back to [EntityCandidateBackend.allEntitiesSource]
 * and apply this matcher as a filter.
 */
interface SourceCapableMatcher : Matcher {
    fun toCandidateSource(backend: EntityCandidateBackend): CandidateSource?
}
