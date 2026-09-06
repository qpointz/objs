package org.poc.objs.core.persistence.policy

import org.poc.objs.core.persistence.tx.UnitOfWork
import java.util.UUID

class PolicySuiteDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun findById(id: UUID): PolicySuiteRecord? = em.find(PolicySuiteRecord::class.java, id)

    fun findByKey(key: String): PolicySuiteRecord? =
        em.createQuery(
            "select s from PolicySuiteRecord s where s.key = :key",
            PolicySuiteRecord::class.java,
        ).setParameter("key", key).resultList.firstOrNull()

    fun save(record: PolicySuiteRecord): PolicySuiteRecord =
        if (em.find(PolicySuiteRecord::class.java, record.id) == null) {
            em.persist(record)
            record
        } else {
            em.merge(record)
        }

    fun deleteById(id: UUID) {
        findById(id)?.let { em.remove(it) }
    }

    fun findAll(): List<PolicySuiteRecord> =
        em.createQuery("select s from PolicySuiteRecord s", PolicySuiteRecord::class.java).resultList

    fun deleteAll() {
        em.createQuery("delete from PolicySuiteRecord s").executeUpdate()
    }
}

class PolicySuiteFolderDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun findById(id: UUID): PolicySuiteFolderRecord? = em.find(PolicySuiteFolderRecord::class.java, id)

    fun findBySuiteId(suiteId: UUID): List<PolicySuiteFolderRecord> =
        em.createQuery(
            "select f from PolicySuiteFolderRecord f where f.suiteId = :suiteId order by f.sortOrder asc",
            PolicySuiteFolderRecord::class.java,
        ).setParameter("suiteId", suiteId).resultList

    fun save(record: PolicySuiteFolderRecord): PolicySuiteFolderRecord =
        if (em.find(PolicySuiteFolderRecord::class.java, record.id) == null) {
            em.persist(record)
            record
        } else {
            em.merge(record)
        }

    fun deleteBySuiteId(suiteId: UUID) {
        em.createQuery("delete from PolicySuiteFolderRecord f where f.suiteId = :suiteId")
            .setParameter("suiteId", suiteId)
            .executeUpdate()
    }

    fun deleteAll() {
        em.createQuery("delete from PolicySuiteFolderRecord f").executeUpdate()
    }
}
