package org.poc.objs.policy.api

/**
 * Validates and normalizes suite version match strings (G-P27s).
 * - blank → latest
 * - `major.minor.serial` → exact
 * - `major.*` → latest in major line
 */
object SuiteVersionSelectors {
    private val EXACT = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
    private val MAJOR_WILDCARD = Regex("""^(\d+)\.\*$""")

    sealed class Parsed {
        data object Latest : Parsed()

        data class Exact(val majorMinor: String, val serial: Long) : Parsed()

        data class MajorWildcard(val major: Int) : Parsed()
    }

    fun parse(raw: String?): Parsed {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return Parsed.Latest
        EXACT.matchEntire(s)?.let { m ->
            return Parsed.Exact(
                majorMinor = "${m.groupValues[1]}.${m.groupValues[2]}",
                serial = m.groupValues[3].toLong(),
            )
        }
        MAJOR_WILDCARD.matchEntire(s)?.let { m ->
            return Parsed.MajorWildcard(m.groupValues[1].toInt())
        }
        throw IllegalArgumentException(
            "Invalid suite version selector '$s' (use blank, major.minor.serial, or major.*)",
        )
    }

    fun matches(policy: Policy, parsed: Parsed): Boolean =
        when (parsed) {
            is Parsed.Latest -> true
            is Parsed.Exact ->
                policy.version == parsed.majorMinor && policy.serial == parsed.serial
            is Parsed.MajorWildcard -> {
                val major = policy.version.substringBefore('.', missingDelimiterValue = "").toIntOrNull()
                major == parsed.major
            }
        }
}
