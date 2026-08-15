package org.poc.objs.sbom.service

/**
 * Human-readable relation labels (G-P6). Default: split role on `_` and title-case.
 * Optional fixed overrides only where the beautifier is wrong.
 */
object RelationLabels {
    private val overrides: Map<String, String> = mapOf(
        // keep empty unless a role needs a non-mechanical label
    )

    fun display(role: String): String {
        val key = role.trim()
        overrides[key]?.let { return it }
        if (key.isEmpty()) return key
        return key.split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { ch -> ch.titlecase() }
            }
    }
}
