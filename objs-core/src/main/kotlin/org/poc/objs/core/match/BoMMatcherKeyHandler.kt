package org.poc.objs.core.match

/**
 * Extensible DSL handler for one matcher object key (for example `anno` or `anno-expr`).
 */
interface BoMMatcherKeyHandler {
    val key: String

    fun decode(value: Any?, path: String): BoMMatcher
}
