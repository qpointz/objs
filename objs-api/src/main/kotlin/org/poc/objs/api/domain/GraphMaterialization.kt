package org.poc.objs.api.domain

class GraphMaterializationException(
    message: String,
    val diagnostics: List<GraphFragmentDiagnostic> = emptyList(),
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

object ResolvedGraphMaterialization {
    fun requireMaterializable(fragment: ResolvedGraphFragment) {
        if (fragment.hasErrors()) {
            throw GraphMaterializationException(
                fragment.diagnostics.joinToString("; ") { it.message },
                diagnostics = fragment.diagnostics,
            )
        }
    }
}
