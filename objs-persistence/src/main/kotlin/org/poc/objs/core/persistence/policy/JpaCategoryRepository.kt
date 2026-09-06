package org.poc.objs.core.persistence.policy

import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.policy.api.Category
import org.poc.objs.policy.api.CategoryInUseException
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PolicyKeys
import java.time.Instant
import java.util.UUID

/**
 * JPA-backed [CategoryRepository] (C-28 WI-002). Delete refuses while any [PolicyRecord]
 * still references the category id.
 */
class JpaCategoryRepository(
    private val dao: PolicyCategoryDao,
    private val policyDao: PolicyDao,
    private val uow: UnitOfWork,
) : CategoryRepository {

    override fun save(write: CategoryWrite): Category = uow.write {
        val key = PolicyKeys.requireValid(write.key.trim())
        require(write.name.isNotBlank()) { "Category name must not be blank" }
        require(dao.findByKey(key) == null) { "Category key already exists: $key" }
        val now = Instant.now()
        val record = PolicyCategoryRecord(
            id = UUID.randomUUID(),
            key = key,
            name = write.name.trim(),
            createdAt = now,
            updatedAt = now,
        )
        dao.save(record)
        record.toDomain()
    }

    override fun update(id: UUID, write: CategoryWrite): Category? = uow.write {
        val existing = dao.findById(id) ?: return@write null
        val key = PolicyKeys.requireValid(write.key.trim())
        require(write.name.isNotBlank()) { "Category name must not be blank" }
        val other = dao.findByKey(key)
        require(other == null || other.id == id) { "Category key already exists: $key" }
        existing.key = key
        existing.name = write.name.trim()
        existing.updatedAt = Instant.now()
        dao.save(existing)
        existing.toDomain()
    }

    override fun delete(id: UUID): Boolean = uow.write {
        if (dao.findById(id) == null) return@write false
        if (policyDao.existsByCategoryId(id)) {
            throw CategoryInUseException(id)
        }
        dao.deleteById(id)
        true
    }

    override fun findById(id: UUID): Category? = uow.read { dao.findById(id)?.toDomain() }

    override fun findByKey(key: String): Category? = uow.read { dao.findByKey(key)?.toDomain() }

    override fun list(): List<Category> = uow.read {
        dao.findAll().sortedBy { it.key }.map { it.toDomain() }
    }
}

private fun PolicyCategoryRecord.toDomain() = Category(id = id, name = name, key = key)
