package org.poc.objs.core.match

/**
 * Resolved entity selection plan: candidate [source] then in-memory [filters].
 *
 * When the first stage cannot supply a backend source (not source-capable, or
 * [BoMSourceCapableMatcher.toCandidateSource] returns null — e.g. non-lowerable
 * `anno-expr`), [localEval] is true: all-entities source + every stage via [BoMMatcher.matches].
 *
 * [edgeStrategy] is set when the entity source implements [BoMCandidateSourceWithEdges]
 * (e.g. Postgres annotation containment). Otherwise null → id-bounded induced-edge load.
 */
data class BoMEntitySelectionPlan(
    val source: BoMCandidateSource,
    val filters: List<BoMMatcher>,
    val localEval: Boolean,
    val edgeStrategy: BoMEdgeCandidateStrategy? = null,
) {
    companion object {
        fun resolve(
            stages: List<BoMMatcher>,
            backend: BoMEntityCandidateBackend,
        ): BoMEntitySelectionPlan {
            require(stages.isNotEmpty()) { "matcher stages must not be empty" }
            val first = stages.first()
            val sourced = (first as? BoMSourceCapableMatcher)?.toCandidateSource(backend)
            return if (sourced != null) {
                BoMEntitySelectionPlan(
                    source = sourced,
                    filters = stages.drop(1),
                    localEval = false,
                    edgeStrategy = sourced.edgeStrategyOrNull(),
                )
            } else {
                // Local eval / AllEntities: no source-level edge SQL → bound-by-ids fallback.
                BoMEntitySelectionPlan(
                    source = backend.allEntitiesSource(),
                    filters = stages,
                    localEval = true,
                    edgeStrategy = null,
                )
            }
        }
    }
}
