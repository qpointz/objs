package org.poc.objs.policy.api

import java.util.UUID

/**
 * User-managed category vocabulary entry (C-32).
 * [key] is the human-managed logical identity (G-P36seed); [id] is the stable primary key
 * referenced by [Policy.categoryId]. [name] is a separate display label.
 */
data class Category(
    val id: UUID,
    val name: String,
    val key: String,
)

/** Write payload for [CategoryRepository] create/update. */
data class CategoryWrite(
    val name: String,
    val key: String,
)

/**
 * Shared charset for Policy / Category / PolicySuite [key] identity fields (G-P36seed /
 * G-P40seed): non-blank, trimmed, `^[a-z][a-z0-9_-]*$` (lowercase, starts with a letter, then
 * letters/digits/`_`/`-`).
 */
object PolicyKeys {
    private val KEY = Regex("^[a-z][a-z0-9_-]*$")

    fun requireValid(key: String): String {
        require(KEY.matches(key)) {
            "Key must be lowercase, start with a letter, and contain only [a-z0-9_-] " +
                "(got: '$key')"
        }
        return key
    }
}
