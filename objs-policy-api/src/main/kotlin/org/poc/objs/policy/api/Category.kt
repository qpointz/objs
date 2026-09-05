package org.poc.objs.policy.api

import java.util.UUID

/**
 * User-managed category vocabulary entry (C-32).
 * [slug] is `[a-z]+` only; [id] is the stable primary key referenced by [Policy.categoryId].
 */
data class Category(
    val id: UUID,
    val displayName: String,
    val slug: String,
)

/** Write payload for [CategoryRepository] create/update. */
data class CategoryWrite(
    val displayName: String,
    val slug: String,
)

object CategorySlugs {
    private val SLUG = Regex("^[a-z]+$")

    fun requireValid(slug: String): String {
        require(SLUG.matches(slug)) {
            "Category slug must be lowercase letters only ([a-z]+), got: '$slug'"
        }
        return slug
    }
}
