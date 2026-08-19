package org.poc.objs.core.domain

/**
 * Catalog “latest” comparer (G-A3): numeric SemVer-ish tuples.
 * `1.10.0` is greater than `1.2.0`. Non-numeric leftovers compare as 0.
 */
object BoMSchemaVersion {
    fun compare(left: String, right: String): Int {
        val a = parse(left)
        val b = parse(right)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val ai = a.getOrElse(i) { 0 }
            val bi = b.getOrElse(i) { 0 }
            if (ai != bi) return ai.compareTo(bi)
        }
        return 0
    }

    fun <T> maxByVersion(items: Collection<T>, version: (T) -> String): T? =
        items.maxWithOrNull { x, y -> compare(version(x), version(y)) }

    private fun parse(raw: String): List<Int> =
        raw.split('.', '-', '+').map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}
