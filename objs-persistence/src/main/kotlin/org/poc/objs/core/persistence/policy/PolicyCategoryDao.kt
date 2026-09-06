package org.poc.objs.core.persistence.policy

import org.poc.objs.core.persistence.tx.UnitOfWork
import java.util.UUID

class PolicyCategoryDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun findById(id: UUID): PolicyCategoryRecord? = em.find(PolicyCategoryRecord::class.java, id)

    fun findByKey(key: String): PolicyCategoryRecord? =
        em.createQuery(
            "select c from PolicyCategoryRecord c where c.key = :key",
            PolicyCategoryRecord::class.java,
        ).setParameter("key", key).resultList.firstOrNull()

    fun save(record: PolicyCategoryRecord): PolicyCategoryRecord =
        if (em.find(PolicyCategoryRecord::class.java, record.id) == null) {
            em.persist(record)
            record
        } else {
            em.merge(record)
        }

    fun deleteById(id: UUID) {
        findById(id)?.let { em.remove(it) }
    }

    fun findAll(): List<PolicyCategoryRecord> =
        em.createQuery("select c from PolicyCategoryRecord c", PolicyCategoryRecord::class.java).resultList

    fun deleteAll() {
        em.createQuery("delete from PolicyCategoryRecord c").executeUpdate()
    }
}
