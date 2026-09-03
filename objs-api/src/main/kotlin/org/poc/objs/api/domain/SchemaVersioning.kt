package org.poc.objs.api.domain

/**
 * Version helpers for schema catalogs.
 *
 * v1 only increments the highest major component. Full semantic versioning rules remain future work.
 */
object SchemaVersioning {
    private val versionPattern = Regex("""^(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:[-+].*)?$""")

    data class ParsedVersion(
        val raw: String,
        val major: Int,
        val dotted: Boolean,
    )

    fun parse(version: String): ParsedVersion? {
        val trimmed = version.trim()
        val match = versionPattern.matchEntire(trimmed) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val dotted = match.groupValues[2].isNotEmpty() || match.groupValues[3].isNotEmpty()
        return ParsedVersion(raw = trimmed, major = major, dotted = dotted)
    }

    /**
     * Compute the next major version from existing versions of one type.
     *
     * Examples: `4` → `5`, `4.2.1` → `5.0.0`, empty → `1.0.0`.
     * When any existing version is dotted, the next major is emitted as `N.0.0`.
     */
    fun nextMajor(existingVersions: Collection<String>): String {
        if (existingVersions.isEmpty()) return "1.0.0"
        val parsed = existingVersions.mapNotNull { parse(it) }
        if (parsed.isEmpty()) {
            throw SchemaDefinitionException(
                "Cannot compute next major version from non-numeric versions: ${existingVersions.joinToString()}",
            )
        }
        val maxMajor = parsed.maxOf { it.major }
        val dotted = parsed.any { it.dotted }
        return if (dotted) "${maxMajor + 1}.0.0" else "${maxMajor + 1}"
    }
}
