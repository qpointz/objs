package org.poc.objs.core.persistence.policy

import org.poc.objs.core.persistence.tx.UnitOfWork
import java.util.UUID

class PolicyDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun findById(id: UUID): PolicyRecord? = em.find(PolicyRecord::class.java, id)

    fun save(record: PolicyRecord): PolicyRecord =
        if (em.find(PolicyRecord::class.java, record.id) == null) {
            em.persist(record)
            record
        } else {
            em.merge(record)
        }

    fun deleteById(id: UUID) {
        findById(id)?.let { em.remove(it) }
    }

    fun findByKey(key: String): List<PolicyRecord> =
        em.createQuery(
            "select p from PolicyRecord p where p.key = :key order by p.serial asc",
            PolicyRecord::class.java,
        ).setParameter("key", key).resultList

    fun findByKeyAndSerial(key: String, serial: Long): PolicyRecord? =
        em.createQuery(
            "select p from PolicyRecord p where p.key = :key and p.serial = :serial",
            PolicyRecord::class.java,
        ).setParameter("key", key).setParameter("serial", serial).resultList.firstOrNull()

    fun findLatestByKey(key: String): PolicyRecord? =
        em.createQuery(
            "select p from PolicyRecord p where p.key = :key order by p.serial desc",
            PolicyRecord::class.java,
        ).setParameter("key", key).setMaxResults(1).resultList.firstOrNull()

    fun maxSerialByKey(key: String): Long? =
        em.createQuery(
            "select max(p.serial) from PolicyRecord p where p.key = :key",
            Long::class.javaObjectType,
        ).setParameter("key", key).singleResult

    fun findAll(): List<PolicyRecord> =
        em.createQuery("select p from PolicyRecord p", PolicyRecord::class.java).resultList

    fun existsByCategoryId(categoryId: UUID): Boolean =
        em.createQuery(
            "select count(p) from PolicyRecord p where p.categoryId = :categoryId",
            Long::class.javaObjectType,
        ).setParameter("categoryId", categoryId).singleResult > 0

    fun deleteAll() {
        em.createQuery("delete from PolicyRecord p").executeUpdate()
    }
}
