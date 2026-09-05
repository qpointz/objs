package org.poc.objs.policy.core

import org.poc.objs.policy.api.Category
import org.poc.objs.policy.api.CategoryInUseException
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.CategorySlugs
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.Policy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-local [CategoryRepository]. Delete refuses while [isReferenced] is true for the id.
 */
class InMemoryCategoryRepository(
    private val isReferenced: (UUID) -> Boolean = { false },
) : CategoryRepository {
    private val byId = ConcurrentHashMap<UUID, Category>()
    private val bySlug = ConcurrentHashMap<String, UUID>()

    override fun save(write: CategoryWrite): Category {
        val slug = CategorySlugs.requireValid(write.slug.trim())
        require(write.displayName.isNotBlank()) { "Category displayName must not be blank" }
        require(!bySlug.containsKey(slug)) { "Category slug already exists: $slug" }

        val stored = Category(
            id = UUID.randomUUID(),
            displayName = write.displayName.trim(),
            slug = slug,
        )
        byId[stored.id] = stored
        bySlug[slug] = stored.id
        return stored
    }

    override fun update(id: UUID, write: CategoryWrite): Category? {
        val existing = byId[id] ?: return null
        val slug = CategorySlugs.requireValid(write.slug.trim())
        require(write.displayName.isNotBlank()) { "Category displayName must not be blank" }
        val other = bySlug[slug]
        require(other == null || other == id) { "Category slug already exists: $slug" }

        if (existing.slug != slug) {
            bySlug.remove(existing.slug)
        }
        val updated = existing.copy(
            displayName = write.displayName.trim(),
            slug = slug,
        )
        byId[id] = updated
        bySlug[slug] = id
        return updated
    }

    override fun delete(id: UUID): Boolean {
        val existing = byId[id] ?: return false
        if (isReferenced(id)) {
            throw CategoryInUseException(id)
        }
        byId.remove(id)
        bySlug.remove(existing.slug)
        return true
    }

    override fun findById(id: UUID): Category? = byId[id]

    override fun findBySlug(slug: String): Category? =
        bySlug[slug]?.let { byId[it] }

    override fun list(): List<Category> =
        byId.values.sortedBy { it.slug }
}

/**
 * Wires [InMemoryCategoryRepository] + [InMemoryPolicyRepository] with cross-reference
 * for category delete / policy category validation.
 */
class InMemoryPolicyStores {
    val categories: InMemoryCategoryRepository =
        InMemoryCategoryRepository { id -> policies.list().any { it.categoryId == id } }
    val policies: InMemoryPolicyRepository = InMemoryPolicyRepository(categories)
}
