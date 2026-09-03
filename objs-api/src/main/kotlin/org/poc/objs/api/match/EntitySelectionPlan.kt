package org.poc.objs.api.match

/**
 * Resolved entity selection plan: candidate [source] then in-memory [filters].
 *
 * When the first stage cannot supply a backend source (not source-capable, or
 * [SourceCapableMatcher.toCandidateSource] returns null — e.g. non-lowerable
 * `anno-expr`), [localEval] is true: all-entities source + every stage via [Matcher.matches].
 *
 * [edgeStrategy] is set when the entity source implements [CandidateSourceWithEdges]
 * (e.g. Postgres annotation containment). Otherwise null → id-bounded induced-edge load.
 */
data class EntitySelectionPlan(
    val source: CandidateSource,
    val filters: List<Matcher>,
    val localEval: Boolean,
    val edgeStrategy: EdgeCandidateStrategy? = null,
) {
    companion object {
        fun resolve(
            stages: List<Matcher>,
            backend: EntityCandidateBackend,
        ): EntitySelectionPlan {
            require(stages.isNotEmpty()) { "matcher stages must not be empty" }
            val first = stages.first()
            val sourced = (first as? SourceCapableMatcher)?.toCandidateSource(backend)
            return if (sourced != null) {
                EntitySelectionPlan(
                    source = sourced,
                    filters = stages.drop(1),
                    localEval = false,
                    edgeStrategy = sourced.edgeStrategyOrNull(),
                )
            } else {
                // Local eval / AllEntities: no source-level edge SQL → bound-by-ids fallback.
                EntitySelectionPlan(
                    source = backend.allEntitiesSource(),
                    filters = stages,
                    localEval = true,
                    edgeStrategy = null,
                )
            }
        }
    }
}
