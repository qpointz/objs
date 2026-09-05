package org.poc.objs.policy.api

import java.util.UUID

/**
 * Persist / load user-managed policy categories (C-32). Split from [PolicyRepository].
 */
interface CategoryRepository {
    fun save(write: CategoryWrite): Category

    fun update(id: UUID, write: CategoryWrite): Category?

    /**
     * Delete when unreferenced. Throws [CategoryInUseException] if any policy still
     * references [id]. Returns false if unknown.
     */
    fun delete(id: UUID): Boolean

    fun findById(id: UUID): Category?

    fun findBySlug(slug: String): Category?

    fun list(): List<Category>
}

class CategoryInUseException(
    val categoryId: UUID,
) : IllegalStateException("Category $categoryId is still referenced by one or more policies")
