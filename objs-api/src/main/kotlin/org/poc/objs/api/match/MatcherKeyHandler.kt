package org.poc.objs.api.match

/**
 * Extensible DSL handler for one matcher object key (for example `anno` or `anno-expr`).
 */
interface MatcherKeyHandler {
    val key: String

    fun decode(value: Any?, path: String): Matcher
}
