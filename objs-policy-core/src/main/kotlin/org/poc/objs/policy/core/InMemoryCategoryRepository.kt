package org.poc.objs.policy.core

import org.poc.objs.policy.api.Category
import org.poc.objs.policy.api.CategoryInUseException
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyKeys
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-local [CategoryRepository]. Delete refuses while [isReferenced] is true for the id.
 */
class InMemoryCategoryRepository(
    private val isReferenced: (UUID) -> Boolean = { false },
) : CategoryRepository {
    private val byId = ConcurrentHashMap<UUID, Category>()
    private val byKey = ConcurrentHashMap<String, UUID>()

    override fun save(write: CategoryWrite): Category {
        val key = PolicyKeys.requireValid(write.key.trim())
        require(write.name.isNotBlank()) { "Category name must not be blank" }
        require(!byKey.containsKey(key)) { "Category key already exists: $key" }

        val stored = Category(
            id = UUID.randomUUID(),
            name = write.name.trim(),
            key = key,
        )
        byId[stored.id] = stored
        byKey[key] = stored.id
        return stored
    }

    override fun update(id: UUID, write: CategoryWrite): Category? {
        val existing = byId[id] ?: return null
        val key = PolicyKeys.requireValid(write.key.trim())
        require(write.name.isNotBlank()) { "Category name must not be blank" }
        val other = byKey[key]
        require(other == null || other == id) { "Category key already exists: $key" }

        if (existing.key != key) {
            byKey.remove(existing.key)
        }
        val updated = existing.copy(
            name = write.name.trim(),
            key = key,
        )
        byId[id] = updated
        byKey[key] = id
        return updated
    }

    override fun delete(id: UUID): Boolean {
        val existing = byId[id] ?: return false
        if (isReferenced(id)) {
            throw CategoryInUseException(id)
        }
        byId.remove(id)
        byKey.remove(existing.key)
        return true
    }

    override fun findById(id: UUID): Category? = byId[id]

    override fun findByKey(key: String): Category? =
        byKey[key]?.let { byId[it] }

    override fun list(): List<Category> =
        byId.values.sortedBy { it.key }
}

/**
 * Wires [InMemoryCategoryRepository] + [InMemoryPolicyRepository] + [InMemorySuiteRepository]
 * with cross-reference for category delete / policy category validation.
 */
class InMemoryPolicyStores {
    val categories: InMemoryCategoryRepository =
        InMemoryCategoryRepository { id -> policies.list().any { it.categoryId == id } }
    val policies: InMemoryPolicyRepository = InMemoryPolicyRepository(categories)
    val suites: InMemorySuiteRepository = InMemorySuiteRepository()
}
