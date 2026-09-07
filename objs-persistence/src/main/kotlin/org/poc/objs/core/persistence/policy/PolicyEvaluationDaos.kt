package org.poc.objs.core.persistence.policy

import org.poc.objs.core.persistence.tx.UnitOfWork
import java.util.UUID

class PolicyEvaluationDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun findById(id: UUID): PolicyEvaluationRecord? =
        em.find(PolicyEvaluationRecord::class.java, id)

    fun save(record: PolicyEvaluationRecord): PolicyEvaluationRecord =
        if (em.find(PolicyEvaluationRecord::class.java, record.evaluationId) == null) {
            em.persist(record)
            record
        } else {
            em.merge(record)
        }

    fun deleteById(id: UUID) {
        findById(id)?.let { em.remove(it) }
    }
}

class PolicyOutcomeDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun deleteByEvaluationId(evaluationId: UUID) {
        em.createQuery("delete from PolicyOutcomeRecord o where o.evaluationId = :id")
            .setParameter("id", evaluationId)
            .executeUpdate()
    }

    fun save(record: PolicyOutcomeRecord): PolicyOutcomeRecord {
        em.persist(record)
        return record
    }

    fun findByEvaluationId(evaluationId: UUID): List<PolicyOutcomeRecord> =
        em.createQuery(
            "select o from PolicyOutcomeRecord o where o.evaluationId = :id order by o.ordinal asc",
            PolicyOutcomeRecord::class.java,
        ).setParameter("id", evaluationId).resultList
}

class PolicyFindingDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun deleteByOutcomeIds(outcomeIds: Collection<UUID>) {
        if (outcomeIds.isEmpty()) return
        em.createQuery("delete from PolicyFindingRecord f where f.outcomeId in :ids")
            .setParameter("ids", outcomeIds)
            .executeUpdate()
    }

    fun save(record: PolicyFindingRecord): PolicyFindingRecord {
        em.persist(record)
        return record
    }

    fun findByOutcomeIds(outcomeIds: Collection<UUID>): List<PolicyFindingRecord> {
        if (outcomeIds.isEmpty()) return emptyList()
        return em.createQuery(
            "select f from PolicyFindingRecord f where f.outcomeId in :ids order by f.idx asc",
            PolicyFindingRecord::class.java,
        ).setParameter("ids", outcomeIds).resultList
    }
}
