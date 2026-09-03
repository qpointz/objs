package org.poc.objs.api.domain

import org.poc.objs.api.domain.*

/**
 * 1-based page of pool/graph entities (G-A6). [size] is clamped to `1..100`, default 20.
 */
data class PageRequest(
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
) {
    val offset: Int get() = (page - 1) * size

    init {
        require(page >= 1) { "page must be >= 1" }
        require(size in MIN_SIZE..MAX_SIZE) { "size must be in $MIN_SIZE..$MAX_SIZE" }
    }

    companion object {
        const val DEFAULT_PAGE = 1
        const val DEFAULT_SIZE = 20
        const val MIN_SIZE = 1
        const val MAX_SIZE = 100

        @JvmStatic
        @JvmOverloads
        fun of(page: Int? = null, size: Int? = null): PageRequest {
            val p = page ?: DEFAULT_PAGE
            val s = (size ?: DEFAULT_SIZE).coerceIn(MIN_SIZE, MAX_SIZE)
            require(p >= 1) { "page must be >= 1" }
            return PageRequest(page = p, size = s)
        }
    }
}

data class PagedEntities(
    val items: List<Entity>,
    val total: Long,
    val page: Int,
    val size: Int,
)
